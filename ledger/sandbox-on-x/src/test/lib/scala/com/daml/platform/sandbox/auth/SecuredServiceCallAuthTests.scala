// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.platform.sandbox.auth

trait SecuredServiceCallAuthTests extends ServiceCallAuthTests {
  behavior of serviceCallName

  it should "deny unauthenticated calls" in {
    expectUnauthenticated(serviceCallWithToken(None))
  }
}
