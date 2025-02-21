// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.api

import com.daml.ledger.api.v1.transaction.{Transaction, TransactionTree}
import com.daml.telemetry.SpanAttribute
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers._

class TraceIdentifiersTest extends AnyWordSpec {
  val expected = Map(
    (SpanAttribute.TransactionId, "transaction-id"),
    (SpanAttribute.CommandId, "command-id"),
    (SpanAttribute.WorkflowId, "workflow-id"),
    (SpanAttribute.Offset, "offset"),
  )

  "extract identifiers from Transaction" should {
    "set non-empty values" in {
      val observed = TraceIdentifiers.fromTransaction(
        Transaction("transaction-id", "command-id", "workflow-id", None, Seq(), "offset")
      )
      observed shouldEqual expected
    }

    "not set empty values" in {
      val observed =
        TraceIdentifiers.fromTransaction(Transaction("", "", "", None, Seq(), ""))
      observed shouldBe empty
    }
  }

  "extract identifiers from TransactionTree" should {
    "set non-empty values" in {
      val observed = TraceIdentifiers.fromTransactionTree(
        TransactionTree("transaction-id", "command-id", "workflow-id", None, "offset", Map())
      )
      observed shouldEqual expected
    }

    "not set empty values" in {
      val observed =
        TraceIdentifiers.fromTransaction(Transaction("", "", "", None, Seq(), ""))
      observed shouldBe empty
    }
  }
}
