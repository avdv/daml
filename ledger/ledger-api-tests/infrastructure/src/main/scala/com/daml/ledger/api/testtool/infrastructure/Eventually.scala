// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.api.testtool.infrastructure

import com.daml.timer.RetryStrategy

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{ExecutionContext, Future}

object Eventually {
  /*
  Runs provided closure with the exponential back-off retry strategy for a number of `attempts`.
   */
  def eventually[A](
      runAssertion: => Future[A],
      attempts: Int = 10,
      firstWaitTime: Duration = 10.millis,
  )(implicit ec: ExecutionContext): Future[A] =
    RetryStrategy.exponentialBackoff(attempts, firstWaitTime) { (_, _) =>
      runAssertion
    }
}
