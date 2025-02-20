// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.rxjava.grpc;

import static java.util.Arrays.asList;

import com.daml.grpc.adapter.ExecutionSequencerFactory;
import com.daml.ledger.api.v1.CommandServiceGrpc;
import com.daml.ledger.api.v1.CommandServiceOuterClass;
import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.SubmitAndWaitRequest;
import com.daml.ledger.javaapi.data.Transaction;
import com.daml.ledger.javaapi.data.TransactionTree;
import com.daml.ledger.rxjava.CommandClient;
import com.daml.ledger.rxjava.grpc.helpers.StubHelper;
import com.daml.ledger.rxjava.util.CreateSingle;
import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.reactivex.Single;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CommandClientImpl implements CommandClient {

  private final String ledgerId;
  private final CommandServiceGrpc.CommandServiceFutureStub serviceStub;
  private final ExecutionSequencerFactory sequencerFactory;

  public CommandClientImpl(
      @NonNull String ledgerId,
      @NonNull Channel channel,
      @NonNull ExecutionSequencerFactory sequencerFactory,
      @NonNull Optional<String> accessToken) {
    this.ledgerId = ledgerId;
    this.sequencerFactory = sequencerFactory;
    this.serviceStub =
        StubHelper.authenticating(CommandServiceGrpc.newFutureStub(channel), accessToken);
  }

  private Single<Empty> submitAndWait(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull Optional<String> accessToken) {
    CommandServiceOuterClass.SubmitAndWaitRequest request =
        SubmitAndWaitRequest.toProto(
            this.ledgerId,
            workflowId,
            applicationId,
            commandId,
            actAs,
            readAs,
            minLedgerTimeAbs,
            minLedgerTimeRel,
            deduplicationTime,
            commands);
    return CreateSingle.fromFuture(
        StubHelper.authenticating(this.serviceStub, accessToken).submitAndWait(request),
        sequencerFactory);
  }

  @Override
  public Single<Empty> submitAndWait(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWait(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.empty());
  }

  @Override
  public Single<Empty> submitAndWait(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWait(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.empty());
  }

  @Override
  public Single<Empty> submitAndWait(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWait(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<Empty> submitAndWait(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWait(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<Empty> submitAndWait(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWait(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.empty());
  }

  @Override
  public Single<Empty> submitAndWait(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWait(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.empty());
  }

  @Override
  public Single<Empty> submitAndWait(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWait(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<Empty> submitAndWait(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWait(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.of(accessToken));
  }

  private Single<String> submitAndWaitForTransactionId(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull Optional<String> accessToken) {
    CommandServiceOuterClass.SubmitAndWaitRequest request =
        SubmitAndWaitRequest.toProto(
            this.ledgerId,
            workflowId,
            applicationId,
            commandId,
            actAs,
            readAs,
            minLedgerTimeAbs,
            minLedgerTimeRel,
            deduplicationTime,
            commands);
    return CreateSingle.fromFuture(
            StubHelper.authenticating(this.serviceStub, accessToken)
                .submitAndWaitForTransactionId(request),
            sequencerFactory)
        .map(CommandServiceOuterClass.SubmitAndWaitForTransactionIdResponse::getTransactionId);
  }

  @Override
  public Single<String> submitAndWaitForTransactionId(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransactionId(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.empty());
  }

  @Override
  public Single<String> submitAndWaitForTransactionId(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransactionId(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.empty());
  }

  @Override
  public Single<String> submitAndWaitForTransactionId(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransactionId(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<String> submitAndWaitForTransactionId(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransactionId(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<String> submitAndWaitForTransactionId(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransactionId(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.empty());
  }

  @Override
  public Single<String> submitAndWaitForTransactionId(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransactionId(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.empty());
  }

  @Override
  public Single<String> submitAndWaitForTransactionId(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransactionId(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<String> submitAndWaitForTransactionId(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransactionId(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.of(accessToken));
  }

  private Single<Transaction> submitAndWaitForTransaction(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull Optional<String> accessToken) {
    CommandServiceOuterClass.SubmitAndWaitRequest request =
        SubmitAndWaitRequest.toProto(
            this.ledgerId,
            workflowId,
            applicationId,
            commandId,
            actAs,
            readAs,
            minLedgerTimeAbs,
            minLedgerTimeRel,
            deduplicationTime,
            commands);
    return CreateSingle.fromFuture(
            StubHelper.authenticating(this.serviceStub, accessToken)
                .submitAndWaitForTransaction(request),
            sequencerFactory)
        .map(CommandServiceOuterClass.SubmitAndWaitForTransactionResponse::getTransaction)
        .map(Transaction::fromProto);
  }

  @Override
  public Single<Transaction> submitAndWaitForTransaction(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransaction(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.empty());
  }

  @Override
  public Single<Transaction> submitAndWaitForTransaction(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransaction(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.empty());
  }

  @Override
  public Single<Transaction> submitAndWaitForTransaction(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransaction(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<Transaction> submitAndWaitForTransaction(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransaction(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<Transaction> submitAndWaitForTransaction(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransaction(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.empty());
  }

  @Override
  public Single<Transaction> submitAndWaitForTransaction(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransaction(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.empty());
  }

  @Override
  public Single<Transaction> submitAndWaitForTransaction(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransaction(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<Transaction> submitAndWaitForTransaction(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransaction(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.of(accessToken));
  }

  private Single<TransactionTree> submitAndWaitForTransactionTree(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull Optional<String> accessToken) {
    CommandServiceOuterClass.SubmitAndWaitRequest request =
        SubmitAndWaitRequest.toProto(
            this.ledgerId,
            workflowId,
            applicationId,
            commandId,
            actAs,
            readAs,
            minLedgerTimeAbs,
            minLedgerTimeRel,
            deduplicationTime,
            commands);
    return CreateSingle.fromFuture(
            StubHelper.authenticating(this.serviceStub, accessToken)
                .submitAndWaitForTransactionTree(request),
            sequencerFactory)
        .map(CommandServiceOuterClass.SubmitAndWaitForTransactionTreeResponse::getTransaction)
        .map(TransactionTree::fromProto);
  }

  @Override
  public Single<TransactionTree> submitAndWaitForTransactionTree(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransactionTree(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.empty());
  }

  @Override
  public Single<TransactionTree> submitAndWaitForTransactionTree(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransactionTree(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.empty());
  }

  @Override
  public Single<TransactionTree> submitAndWaitForTransactionTree(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransactionTree(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<TransactionTree> submitAndWaitForTransactionTree(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull Optional<Instant> minLedgerTimeAbs,
      @NonNull Optional<Duration> minLedgerTimeRel,
      @NonNull Optional<Duration> deduplicationTime,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransactionTree(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        minLedgerTimeAbs,
        minLedgerTimeRel,
        deduplicationTime,
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<TransactionTree> submitAndWaitForTransactionTree(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransactionTree(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.empty());
  }

  @Override
  public Single<TransactionTree> submitAndWaitForTransactionTree(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull List<@NonNull Command> commands) {
    return submitAndWaitForTransactionTree(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.empty());
  }

  @Override
  public Single<TransactionTree> submitAndWaitForTransactionTree(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull String party,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransactionTree(
        workflowId,
        applicationId,
        commandId,
        asList(party),
        asList(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.of(accessToken));
  }

  @Override
  public Single<TransactionTree> submitAndWaitForTransactionTree(
      @NonNull String workflowId,
      @NonNull String applicationId,
      @NonNull String commandId,
      @NonNull List<@NonNull String> actAs,
      @NonNull List<@NonNull String> readAs,
      @NonNull List<@NonNull Command> commands,
      @NonNull String accessToken) {
    return submitAndWaitForTransactionTree(
        workflowId,
        applicationId,
        commandId,
        actAs,
        readAs,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        commands,
        Optional.of(accessToken));
  }
}
