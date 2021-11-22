// Copyright (c) 2021 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.platform.server.api.services.grpc

import java.time.{Duration, Instant}

import com.daml.error.{DamlContextualizedErrorLogger, ErrorCodesVersionSwitcher}
import com.daml.ledger.api.SubmissionIdGenerator
import com.daml.ledger.api.domain.LedgerId
import com.daml.ledger.api.v1.command_submission_service.CommandSubmissionServiceGrpc.{
  CommandSubmissionService => ApiCommandSubmissionService
}
import com.daml.ledger.api.v1.command_submission_service.{
  CommandSubmissionServiceGrpc,
  SubmitRequest => ApiSubmitRequest,
}
import com.daml.ledger.api.validation.{CommandsValidator, SubmitRequestValidator}
import com.daml.logging.{ContextualizedLogger, LoggingContext}
import com.daml.metrics.{Metrics, Timed}
import com.daml.platform.api.grpc.GrpcApiService
import com.daml.platform.server.api.services.domain.CommandSubmissionService
import com.daml.platform.server.api.validation.{ErrorFactories, FieldValidations}
import com.daml.platform.server.api.{ProxyCloseable, ValidationLogger}
import com.daml.telemetry.{DefaultTelemetry, TelemetryContext}
import com.google.protobuf.empty.Empty
import io.grpc.ServerServiceDefinition

import scala.concurrent.{ExecutionContext, Future}

class GrpcCommandSubmissionService(
    override protected val service: CommandSubmissionService with AutoCloseable,
    ledgerId: LedgerId,
    currentLedgerTime: () => Instant,
    currentUtcTime: () => Instant,
    maxDeduplicationTime: () => Option[Duration],
    submissionIdGenerator: SubmissionIdGenerator,
    metrics: Metrics,
    errorCodesVersionSwitcher: ErrorCodesVersionSwitcher,
)(implicit executionContext: ExecutionContext, loggingContext: LoggingContext)
    extends ApiCommandSubmissionService
    with ProxyCloseable
    with GrpcApiService {

  protected implicit val logger: ContextualizedLogger = ContextualizedLogger.get(getClass)
  private val validator = new SubmitRequestValidator(
    new CommandsValidator(ledgerId, errorCodesVersionSwitcher),
    FieldValidations(ErrorFactories(errorCodesVersionSwitcher)),
  )

  override def submit(request: ApiSubmitRequest): Future[Empty] = {
    implicit val telemetryContext: TelemetryContext =
      DefaultTelemetry.contextFromGrpcThreadLocalContext()
    val requestWithSubmissionId = generateSubmissionIdIfEmpty(request)
    val errorLogger = new DamlContextualizedErrorLogger(
      logger = logger,
      loggingContext = loggingContext,
      correlationId = requestWithSubmissionId.commands.map(_.submissionId),
    )
    Timed
      .value(
        metrics.daml.commands.validation,
        validator.validate(
          requestWithSubmissionId,
          currentLedgerTime(),
          currentUtcTime(),
          maxDeduplicationTime(),
        )(errorLogger),
      )
      .fold(
        t => Future.failed(ValidationLogger.logFailure(requestWithSubmissionId, t)),
        service.submit(_).map(_ => Empty.defaultInstance),
      )
  }

  override def bindService(): ServerServiceDefinition =
    CommandSubmissionServiceGrpc.bindService(this, executionContext)

  private def generateSubmissionIdIfEmpty(request: ApiSubmitRequest): ApiSubmitRequest =
    if (request.commands.exists(_.submissionId.isEmpty))
      request.update(_.commands.submissionId := submissionIdGenerator.generate())
    else
      request
}
