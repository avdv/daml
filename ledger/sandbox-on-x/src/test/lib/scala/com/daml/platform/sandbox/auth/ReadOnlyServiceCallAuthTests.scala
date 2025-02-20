// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.platform.sandbox.auth

import com.daml.ledger.api.v1.admin.{user_management_service => proto}
import org.scalatest.Assertion

import scala.concurrent.Future

trait ReadOnlyServiceCallAuthTests extends ServiceCallWithMainActorAuthTests {

  /** Allows to override what is regarded as a successful response, e.g. lookup queries for
    * commands can return a NOT_FOUND, which is fine because the result is not PERMISSION_DENIED
    */
  def successfulBehavior: Future[Any] => Future[Assertion] = expectSuccess(_: Future[Any])

  /** Flag to switch of a particular kind of test for technical reasons. See the use sites for details. */
  protected val testCanReadAsMainActor: Boolean = true

  protected def serviceCallWithMainActorUser(
      userPrefix: String,
      rights: Vector[proto.Right.Kind],
  ): Future[Any] =
    createUserByAdmin(userPrefix + mainActor, rights.map(proto.Right(_)))
      .flatMap { case (_, token) => serviceCallWithoutApplicationId(token) }

  it should "deny calls with an expired read-only token" in {
    expectUnauthenticated(serviceCallWithToken(canReadAsMainActorExpired))
  }
  it should "allow calls with explicitly non-expired read-only token" in {
    successfulBehavior(serviceCallWithToken(canReadAsMainActorExpiresTomorrow))
  }
  it should "allow calls with read-only token without expiration" in {
    successfulBehavior(serviceCallWithToken(canReadAsMainActor))
  }
  it should "allow calls with user token that can-read-as main actor" in {
    // The completion stream tests are structured as submit-command-then-consume-completions, which requires read-write
    // rights. The tests for custom claim tokens provide the read-write tokens implicitly. That is not possible for user tokens.
    // We thus disable this test via an override in the completion stream tests.
    assume(testCanReadAsMainActor)
    successfulBehavior(
      serviceCallWithMainActorUser(
        "u1",
        Vector(proto.Right.Kind.CanReadAs(proto.Right.CanReadAs(mainActor))),
      )
    )
  }
  it should "deny calls with 'participant_admin' user token" in {
    expectPermissionDenied(serviceCallWithoutApplicationId(canReadAsAdminStandardJWT))
  }
  it should "deny calls with user token that cannot read as main actor" in {
    expectPermissionDenied(serviceCallWithMainActorUser("u2", Vector.empty))
  }
  it should "deny calls with non-expired 'unknown_user' user token" in {
    expectPermissionDenied(serviceCallWithoutApplicationId(canReadAsUnknownUserStandardJWT))
  }

  it should "deny calls with an expired read/write token" in {
    expectUnauthenticated(serviceCallWithToken(canActAsMainActorExpired))
  }
  it should "allow calls with explicitly non-expired read/write token" in {
    successfulBehavior(serviceCallWithToken(canActAsMainActorExpiresTomorrow))
  }
  it should "allow calls with read/write token without expiration" in {
    successfulBehavior(serviceCallWithToken(canActAsMainActor))
  }
  it should "allow calls with user token that can-act-as main actor" in {
    successfulBehavior(
      serviceCallWithMainActorUser(
        "u3",
        Vector(proto.Right.Kind.CanActAs(proto.Right.CanActAs(mainActor))),
      )
    )
  }

  it should "allow calls with the correct ledger ID" in {
    successfulBehavior(serviceCallWithToken(canReadAsMainActorActualLedgerId))
  }
  it should "deny calls with a random ledger ID" in {
    expectPermissionDenied(serviceCallWithToken(canReadAsMainActorRandomLedgerId))
  }
  it should "allow calls with the correct participant ID" in {
    successfulBehavior(serviceCallWithToken(canReadAsMainActorActualParticipantId))
  }
  it should "deny calls with a random participant ID" in {
    expectPermissionDenied(serviceCallWithToken(canReadAsMainActorRandomParticipantId))
  }
}
