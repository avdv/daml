// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.platform.apiserver

import com.daml.ledger.api.v1.experimental_features.{
  CommandDeduplicationFeatures,
  ExperimentalContractIds,
}

case class LedgerFeatures(
    staticTime: Boolean = false,
    commandDeduplicationFeatures: CommandDeduplicationFeatures =
      CommandDeduplicationFeatures.defaultInstance,
    contractIdFeatures: ExperimentalContractIds = ExperimentalContractIds.defaultInstance,
)
