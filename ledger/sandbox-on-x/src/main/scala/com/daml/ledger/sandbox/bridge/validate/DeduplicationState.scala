// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.sandbox.bridge.validate

import com.daml.ledger.participant.state.v2.ChangeId
import com.daml.ledger.sandbox.bridge.BridgeMetrics
import com.daml.ledger.sandbox.bridge.validate.DeduplicationState.DeduplicationQueue
import com.daml.lf.data.Time

import java.time.Duration
import scala.collection.mutable
import scala.util.chaining._

case class DeduplicationState private (
    private[validate] val deduplicationQueue: DeduplicationQueue,
    private val maxDeduplicationDuration: Duration,
    private val bridgeMetrics: BridgeMetrics,
) {

  def deduplicate(
      changeId: ChangeId,
      commandDeduplicationDuration: Duration,
      recordTime: Time.Timestamp,
  ): Boolean = {
    assert(
      deduplicationQueue.lastRecordTimeOption.forall(_ <= recordTime),
      s"Inserted record time ($recordTime) for changeId ($changeId) cannot be before the last inserted record time (${deduplicationQueue.lastRecordTimeOption}).",
    )
    assert(
      commandDeduplicationDuration.compareTo(maxDeduplicationDuration) <= 0,
      s"Cannot deduplicate for a period ($commandDeduplicationDuration) longer than the max deduplication duration ($maxDeduplicationDuration).",
    )

    bridgeMetrics.SequencerState.deduplicationQueueLength.update(deduplicationQueue.size)

    val expiredTimestamp = expiredThreshold(maxDeduplicationDuration, recordTime)
    deduplicationQueue.removeOlderThan(expiredTimestamp)

    deduplicationQueue
      .get(changeId)
      .exists(_ >= expiredThreshold(commandDeduplicationDuration, recordTime))
      .tap { isDuplicate =>
        // Insert the new deduplication entry if not a duplicate
        if (!isDuplicate) deduplicationQueue.update(changeId, recordTime)
      }
  }

  private def expiredThreshold(
      deduplicationDuration: Duration,
      now: Time.Timestamp,
  ): Time.Timestamp =
    now.subtract(deduplicationDuration)
}

object DeduplicationState {
  private[sandbox] type DeduplicationQueue = DeduplicationStateQueueMap

  private[validate] def empty(
      deduplicationDuration: Duration,
      bridgeMetrics: BridgeMetrics,
  ): DeduplicationState =
    DeduplicationState(
      deduplicationQueue = DeduplicationStateQueueMap.empty,
      maxDeduplicationDuration = deduplicationDuration,
      bridgeMetrics = bridgeMetrics,
    )

  /** This data structure is tailored for keeping an ordered (by insertion) sequence of deduplication entries
    * of the form (changeId, recordTime) with optimal complexity of each exposed operation.
    *
    * @param vector An ordered (by insertion) vector of deduplication entries
    * @param mappings Mapping of changeId to recordTime
    */
  private[validate] case class DeduplicationStateQueueMap(
      vector: mutable.Queue[(ChangeId, Time.Timestamp)],
      mappings: mutable.Map[ChangeId, Time.Timestamp],
  ) {

    /** Returns a state without the entries that are before the expirationTimestamp.
      *
      * Complexity: eL - effectively linear in the number of expired entries
      */
    def removeOlderThan(expirationTimestamp: Time.Timestamp): Unit = {
      // Assuming that the entries have monotonically increasing recordTimes,
      // drop all entries with the recordTime before the expirationTimestamp.
      val prunedVector = vector.dequeueWhile { case (_, recordTime) =>
        recordTime < expirationTimestamp
      }

      // A recordTime for a changeId could have been updated by a newer command inserted in the mapping.
      // Hence, remove only entries whose recordTimes match the expired entry's recordTime.
      mappings --= prunedVector.view.flatMap { case (changeId, recordTime) =>
        mappings.get(changeId).filter(_ == recordTime).map(_ => changeId)
      }
    }

    /** Updates the state with a new deduplication entry.
      *
      * Complexity: eC - effectively constant
      */
    def update(
        changeId: ChangeId,
        recordTime: Time.Timestamp,
    ): Unit = {
      vector.enqueue(changeId -> recordTime)
      mappings += (changeId -> recordTime)
    }

    /** Fetches, if exists, the record time for a changeId
      * Complexity: eC - effectively constant
      */
    def get(changeId: ChangeId): Option[Time.Timestamp] =
      mappings.get(changeId)

    /** The number of deduplication entries */
    def size: Int = vector.size

    /** The last (and assumed highest) record time in the series */
    def lastRecordTimeOption: Option[Time.Timestamp] = vector.lastOption.map(_._2)
  }

  object DeduplicationStateQueueMap {
    def empty: DeduplicationStateQueueMap =
      DeduplicationStateQueueMap(mutable.Queue.empty, mutable.Map.empty)
  }
}
