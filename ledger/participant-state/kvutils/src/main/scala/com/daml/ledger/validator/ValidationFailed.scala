// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.validator

import com.daml.ledger.participant.state.kvutils.Raw

import scala.util.control.NoStackTrace

sealed trait ValidationFailed extends RuntimeException with NoStackTrace

object ValidationFailed {

  final case class MissingInputState(keys: Seq[Raw.StateKey]) extends ValidationFailed

  final case class ValidationError(reason: String) extends ValidationFailed

}
