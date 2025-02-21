// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.participant.state.kvutils.committer

private[committer] object Compat {
  def wrapMap[K, V](m: Map[K, V]): Map[K, Option[V]] = new Map[K, Option[V]] {
    override def updated[V1 >: Option[V]](k: K, v: V1): Map[K, V1] = ???
    override def get(key: K): Option[Option[V]] = Some(m.get(key))
    override def iterator: Iterator[(K, Option[V])] = ???
    override def removed(key: K): Map[K, Option[V]] = ???
  }
}
