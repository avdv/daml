// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.platform.sandbox.cli

import com.daml.platform.sandbox.config.{LedgerName, SandboxConfig}
import scopt.OptionParser

class CommonCli(name: LedgerName) extends CommonCliBase(name) {
  override val parser: OptionParser[SandboxConfig] = super.parser
}
