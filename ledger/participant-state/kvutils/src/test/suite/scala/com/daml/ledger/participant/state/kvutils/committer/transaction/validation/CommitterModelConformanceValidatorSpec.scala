// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.participant.state.kvutils.committer.transaction.validation

import java.time.{Instant, ZoneOffset, ZonedDateTime}
import com.codahale.metrics.MetricRegistry
import com.daml.daml_lf_dev.DamlLf
import com.daml.ledger.participant.state.kvutils.TestHelpers.{createCommitContext, lfTuple}
import com.daml.ledger.participant.state.kvutils.committer.transaction.{
  DamlTransactionEntrySummary,
  Rejections,
}
import com.daml.ledger.participant.state.kvutils.committer.{StepContinue, StepStop}
import com.daml.ledger.participant.state.kvutils.store.events.{
  CausalMonotonicityViolated,
  DamlSubmitterInfo,
  DamlTransactionEntry,
  DamlTransactionRejectionEntry,
}
import com.daml.ledger.participant.state.kvutils.store.{
  DamlContractState,
  DamlLogEntry,
  DamlStateKey,
  DamlStateValue,
}
import com.daml.ledger.participant.state.kvutils.{Conversions, Err}
import com.daml.ledger.validator.TestHelper.{makeContractIdStateKey, makeContractIdStateValue}
import com.daml.lf.archive.testing.Encode
import com.daml.lf.crypto.Hash
import com.daml.lf.data.Time.Timestamp
import com.daml.lf.data.{ImmArray, Ref}
import com.daml.lf.engine.{Engine, Result, ResultError, Error => LfError}
import com.daml.lf.kv.archives.RawArchive
import com.daml.lf.language.Ast.Expr
import com.daml.lf.language.{Ast, LanguageVersion}
import com.daml.lf.testing.parser.Implicits.defaultParserParameters
import com.daml.lf.transaction.TransactionOuterClass.ContractInstance
import com.daml.lf.transaction._
import com.daml.lf.transaction.test.TransactionBuilder
import com.daml.lf.transaction.test.TransactionBuilder.Implicits._
import com.daml.lf.value.Value.{ContractId, ValueRecord, ValueText}
import com.daml.lf.value.{Value, ValueOuterClass}
import com.daml.logging.LoggingContext
import com.daml.metrics.Metrics
import com.google.protobuf.ByteString
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec

class CommitterModelConformanceValidatorSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ArgumentMatchersSugar
    with TableDrivenPropertyChecks {
  import CommitterModelConformanceValidatorSpec._

  private implicit val loggingContext: LoggingContext = LoggingContext.ForTesting

  private val metrics = new Metrics(new MetricRegistry)

  private val defaultValidator = new CommitterModelConformanceValidator(mock[Engine], metrics)
  private val rejections = new Rejections(metrics)

  private val inputCreate = create(
    inputContractId,
    keyAndMaintainer = Some(inputContractKey -> inputContractKeyMaintainer),
  )
  private val aCreate = create(aContractId)
  private val anotherCreate = create(ContractId.V1(Hash.hashPrivateKey("#anotherContractId")))

  private val exercise = txBuilder.exercise(
    contract = inputCreate,
    choice = "DummyChoice",
    consuming = false,
    actingParties = Set(aKeyMaintainer),
    argument = aDummyValue,
    byKey = false,
  )

  private val lookupNodes =
    Seq(aCreate -> true, anotherCreate -> true) map { case (create, found) =>
      txBuilder.lookupByKey(create, found)
    }

  val Seq(aTransaction, anotherTransaction) = lookupNodes map { node =>
    val builder = txBuilder
    val rootId = builder.add(exercise)
    val lookupId = builder.add(node, rootId)
    builder.build() -> lookupId
  }

  private val aTransactionEntry = DamlTransactionEntrySummary(
    DamlTransactionEntry.newBuilder
      .setSubmissionSeed(aSubmissionSeed)
      .setLedgerEffectiveTime(Conversions.buildTimestamp(ledgerEffectiveTime))
      .setRawTransaction(Conversions.assertEncodeTransaction(aTransaction._1).byteString)
      .build
  )

  "createValidationStep" should {
    "create StepContinue in case of correct input" in {
      val mockEngine = mock[Engine]
      val mockValidationResult = mock[Result[Unit]]
      when(
        mockValidationResult.consume(
          any[Value.ContractId => Option[
            Value.VersionedContractInstance
          ]],
          any[Ref.PackageId => Option[Ast.Package]],
          any[GlobalKeyWithMaintainers => Option[Value.ContractId]],
        )
      ).thenReturn(Right(()))
      when(
        mockEngine.validate(
          any[Set[Ref.Party]],
          any[SubmittedTransaction],
          any[Timestamp],
          any[Ref.ParticipantId],
          any[Timestamp],
          any[Hash],
        )(any[LoggingContext])
      ).thenReturn(mockValidationResult)

      val validator = new CommitterModelConformanceValidator(mockEngine, metrics)

      validator.createValidationStep(rejections)(
        createCommitContext(
          None,
          Map(
            inputContractIdStateKey -> Some(makeContractIdStateValue()),
            contractIdStateKey1 -> Some(makeContractIdStateValue()),
          ),
        ),
        aTransactionEntry,
      ) shouldBe StepContinue(aTransactionEntry)
    }

    "create StepStop in case of validation error" in {
      val mockEngine = mock[Engine]
      when(
        mockEngine.validate(
          any[Set[Ref.Party]],
          any[SubmittedTransaction],
          any[Timestamp],
          any[Ref.ParticipantId],
          any[Timestamp],
          any[Hash],
        )(any[LoggingContext])
      ).thenReturn(
        ResultError(
          LfError.Validation.ReplayMismatch(
            ReplayMismatch(aTransaction._1, anotherTransaction._1)
          )
        )
      )

      val validator = new CommitterModelConformanceValidator(mockEngine, metrics)

      val step = validator
        .createValidationStep(rejections)(
          createCommitContext(
            None,
            Map(
              inputContractIdStateKey -> Some(makeContractIdStateValue()),
              contractIdStateKey1 -> Some(makeContractIdStateValue()),
            ),
          ),
          aTransactionEntry,
        )
      inside(step) { case StepStop(logEntry) =>
        logEntry.getTransactionRejectionEntry.getReasonCase shouldBe DamlTransactionRejectionEntry.ReasonCase.VALIDATION_FAILURE
      }
    }

    "create StepStop in case of missing input" in {
      val validator = createThrowingValidator(Err.MissingInputState(inputContractIdStateKey))

      val step = validator
        .createValidationStep(rejections)(
          createCommitContext(None),
          aTransactionEntry,
        )
      inside(step) { case StepStop(logEntry) =>
        logEntry.getTransactionRejectionEntry.getReasonCase shouldBe DamlTransactionRejectionEntry.ReasonCase.MISSING_INPUT_STATE
      }
    }

    "create StepStop in case of decode error" in {
      val validator =
        createThrowingValidator(Err.ArchiveDecodingFailed(aPackageId, "'test message'"))

      val step = validator
        .createValidationStep(rejections)(
          createCommitContext(None),
          aTransactionEntry,
        )
      inside(step) { case StepStop(logEntry) =>
        logEntry.getTransactionRejectionEntry.getReasonCase shouldBe DamlTransactionRejectionEntry.ReasonCase.INVALID_PARTICIPANT_STATE
        logEntry.getTransactionRejectionEntry.getInvalidParticipantState.getDetails should be(
          "Decoding of Daml-LF archive aPackage failed: 'test message'"
        )
      }
    }

    def createThrowingValidator(consumeError: Err): CommitterModelConformanceValidator = {
      val mockEngine = mock[Engine]
      val mockValidationResult = mock[Result[Unit]]

      when(
        mockValidationResult.consume(
          any[Value.ContractId => Option[
            Value.VersionedContractInstance
          ]],
          any[Ref.PackageId => Option[Ast.Package]],
          any[GlobalKeyWithMaintainers => Option[Value.ContractId]],
        )
      ).thenThrow(consumeError)

      when(
        mockEngine.validate(
          any[Set[Ref.Party]],
          any[SubmittedTransaction],
          any[Timestamp],
          any[Ref.ParticipantId],
          any[Timestamp],
          any[Hash],
        )(any[LoggingContext])
      ).thenReturn(mockValidationResult)

      new CommitterModelConformanceValidator(mockEngine, metrics)
    }
  }

  "lookupContract" should {
    "return Some when a contract is present in the current state" in {
      val commitContext = createCommitContext(
        None,
        Map(
          inputContractIdStateKey -> Some(
            aContractIdStateValue
          )
        ),
      )

      val contractInstance = defaultValidator.lookupContract(commitContext)(
        Conversions.decodeContractId(inputContractId.coid)
      )

      contractInstance shouldBe Some(aContractInst)
    }

    "throw if a contract does not exist in the current state" in {
      an[Err.MissingInputState] should be thrownBy defaultValidator.lookupContract(
        createCommitContext(
          None,
          Map.empty,
        )
      )(Conversions.decodeContractId(inputContractId.coid))
    }
  }

  "lookupKey" should {
    val contractKeyInputs = aTransactionEntry.transaction.contractKeyInputs match {
      case Left(_) => fail()
      case Right(contractKeyInputs) => contractKeyInputs
    }

    "return Some when mapping exists" in {
      defaultValidator.lookupKey(contractKeyInputs)(
        aGlobalKeyWithMaintainers(inputContractKey, inputContractKeyMaintainer)
      ) shouldBe Some(Conversions.decodeContractId(inputContractId.coid))
    }

    "return None when mapping does not exist" in {
      defaultValidator.lookupKey(contractKeyInputs)(
        aGlobalKeyWithMaintainers("nonexistentKey", "nonexistentMaintainer")
      ) shouldBe None
    }
  }

  "lookupPackage" should {
    "return the package" in {
      val stateKey = DamlStateKey.newBuilder().setPackageId("aPackage").build()
      val stateValue = DamlStateValue
        .newBuilder()
        .setArchive(anArchive.byteString)
        .build()
      val commitContext = createCommitContext(
        None,
        Map(stateKey -> Some(stateValue)),
      )

      val maybePackage = defaultValidator.lookupPackage(commitContext)(aPackageId)

      maybePackage shouldBe a[Some[_]]
    }

    "fail when the package is missing" in {
      an[Err.MissingInputState] should be thrownBy defaultValidator.lookupPackage(
        createCommitContext(
          None,
          Map.empty,
        )
      )(Ref.PackageId.assertFromString("nonexistentPackageId"))
    }

    "fail when the archive is invalid" in {
      val stateKey = DamlStateKey.newBuilder().setPackageId("invalidPackage").build()

      val stateValues = Table(
        "state values",
        DamlStateValue.newBuilder.build(),
        DamlStateValue.newBuilder.setArchive(DamlLf.Archive.getDefaultInstance.toByteString).build(),
      )

      forAll(stateValues) { stateValue =>
        an[Err.ArchiveDecodingFailed] should be thrownBy defaultValidator.lookupPackage(
          createCommitContext(
            None,
            Map(stateKey -> Some(stateValue)),
          )
        )(Ref.PackageId.assertFromString("invalidPackage"))
      }
    }
  }

  "validateCausalMonotonicity" should {
    "create StepContinue when causal monotonicity holds" in {
      defaultValidator
        .validateCausalMonotonicity(
          aTransactionEntry,
          createCommitContext(
            None,
            Map(
              inputContractIdStateKey -> Some(makeContractIdStateValue()),
              contractIdStateKey1 -> Some(aStateValueActiveAt(ledgerEffectiveTime.minusSeconds(1))),
            ),
          ),
          rejections,
        ) shouldBe StepContinue(aTransactionEntry)
    }

    "reject transaction when causal monotonicity does not hold" in {
      val step = defaultValidator
        .validateCausalMonotonicity(
          aTransactionEntry,
          createCommitContext(
            None,
            Map(
              inputContractIdStateKey -> Some(makeContractIdStateValue()),
              contractIdStateKey1 -> Some(aStateValueActiveAt(ledgerEffectiveTime.plusSeconds(1))),
            ),
          ),
          rejections,
        )

      val expectedEntry = DamlLogEntry.newBuilder
        .setTransactionRejectionEntry(
          DamlTransactionRejectionEntry.newBuilder
            .setSubmitterInfo(DamlSubmitterInfo.getDefaultInstance)
            .setCausalMonotonicityViolated(
              CausalMonotonicityViolated.newBuilder
            )
        )
        .build()
      step shouldBe StepStop(expectedEntry)
    }

    "accept a transaction in case an input contract is non-existent (possibly because it has been pruned)" in {
      val step = defaultValidator.validateCausalMonotonicity(
        aTransactionEntry,
        createCommitContext(None, Map.empty), // No contract is present in the commit context.
        rejections,
      )

      step shouldBe StepContinue(aTransactionEntry)
    }
  }

  private def create(
      contractId: ContractId,
      signatories: Set[Ref.Party] = Set(aKeyMaintainer),
      argument: Value = aDummyValue,
      keyAndMaintainer: Option[(String, String)] = Some(aKey -> aKeyMaintainer),
  ): Node.Create = {
    txBuilder.create(
      id = contractId,
      templateId = aTemplateId,
      argument = argument,
      signatories = signatories,
      observers = Set.empty,
      key = keyAndMaintainer.map { case (key, maintainer) => lfTuple(maintainer, key) },
    )
  }
}

object CommitterModelConformanceValidatorSpec {

  private val inputContractId = ContractId.V1(Hash.hashPrivateKey("#inputContractId"))
  private val inputContractIdStateKey = makeContractIdStateKey(inputContractId.coid)
  private val aContractId = ContractId.V1(Hash.hashPrivateKey("#someContractId"))
  private val contractIdStateKey1 = makeContractIdStateKey(aContractId.coid)
  private val inputContractKey = "inputContractKey"
  private val inputContractKeyMaintainer = "inputContractKeyMaintainer"
  private val aKey = "key"
  private val aKeyMaintainer = "maintainer"
  private val aDummyValue = TransactionBuilder.record("field" -> "value")
  private val aTemplateId = "DummyModule:DummyTemplate"
  private val aPackageId = "aPackage"

  private val aSubmissionSeed = ByteString.copyFromUtf8("a" * 32)
  private val ledgerEffectiveTime =
    ZonedDateTime.of(2021, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant
  private val txVersion = TransactionVersion.StableVersions.max

  private val aContractIdStateValue = {
    makeContractIdStateValue().toBuilder
      .setContractState(
        DamlContractState
          .newBuilder()
          .setRawContractInstance(
            ContractInstance
              .newBuilder()
              .setTemplateId(
                ValueOuterClass.Identifier
                  .newBuilder()
                  .setPackageId(defaultPackageId)
                  .addModuleName("DummyModule")
                  .addName("DummyTemplate")
              )
              .setArgVersioned(
                ValueOuterClass.VersionedValue
                  .newBuilder()
                  .setVersion(txVersion.protoValue)
                  .setValue(
                    ValueOuterClass.Value.newBuilder().setText("dummyValue").build().toByteString
                  )
              )
              .build()
              .toByteString
          )
          .build()
      )
      .build()
  }

  private val aContractInst =
    Value.VersionedContractInstance(
      txVersion,
      aTemplateId,
      ValueText("dummyValue"),
      "",
    )

  private val anArchive: RawArchive = {
    val pkg = Ast.GenPackage[Expr](
      Map.empty,
      Set.empty,
      LanguageVersion.default,
      Some(
        Ast.PackageMetadata(
          Ref.PackageName.assertFromString("aPackage"),
          Ref.PackageVersion.assertFromString("0.0.0"),
        )
      ),
    )
    val archive = Encode.encodeArchive(
      defaultParserParameters.defaultPackageId -> pkg,
      defaultParserParameters.languageVersion,
    )
    RawArchive(archive.toByteString)
  }

  private def txBuilder = TransactionBuilder()

  private def aGlobalKeyWithMaintainers(key: String, maintainer: String) = GlobalKeyWithMaintainers(
    GlobalKey.assertBuild(
      aTemplateId,
      ValueRecord(
        None,
        ImmArray(
          (None, ValueText(maintainer)),
          (None, ValueText(key)),
        ),
      ),
    ),
    Set.empty,
  )

  private def aStateValueActiveAt(activeAt: Instant) =
    DamlStateValue.newBuilder
      .setContractState(
        DamlContractState.newBuilder.setActiveAt(Conversions.buildTimestamp(activeAt))
      )
      .build
}
