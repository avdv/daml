// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.platform.sandbox

import com.daml.ledger.api.domain.LedgerId
import com.daml.lf.data.Ref
import com.daml.platform.apiserver.SeedService.Seeding
import com.daml.platform.common.LedgerIdMode
import com.daml.platform.sandbox.cli.CommonCliSpecBase
import com.daml.platform.sandbox.cli.CommonCliSpecBase.exampleJdbcUrl
import com.daml.sandbox.Cli

class CliSpec extends CommonCliSpecBase(Cli) {

  "Cli" should {
    "parse the ledgerid when given" in {
      val ledgerId = "myledger"
      checkOption(
        Array("--ledgerid", ledgerId),
        _.copy(ledgerIdMode =
          LedgerIdMode.Static(LedgerId(Ref.LedgerString.assertFromString(ledgerId)))
        ),
      )
    }

    "parse the sql-backend-jdbcurl flag when given" in {
      val jdbcUrl = "jdbc:postgresql://localhost:5432/test?user=test"
      checkOption(Array("--sql-backend-jdbcurl", jdbcUrl), _.copy(jdbcUrl = Some(jdbcUrl)))
    }

    "parse the jdbcurl flag (deprecated) when given" in {
      checkOption(Array("--jdbcurl", exampleJdbcUrl), _.copy(jdbcUrl = Some(exampleJdbcUrl)))
    }

    "parse the contract-id-seeding mode when given" in {
      checkOption(Array("--contract-id-seeding", "strong"), _.copy(seeding = Seeding.Strong))
    }
  }

}
