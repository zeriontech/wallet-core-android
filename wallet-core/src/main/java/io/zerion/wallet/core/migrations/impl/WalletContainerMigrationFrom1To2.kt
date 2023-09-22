package io.zerion.wallet.core.migrations.impl

import io.zerion.wallet.core.migrations.WalletContainerMigration
import io.zerion.wallet.core.models.WalletContainer

internal class WalletContainerMigrationFrom1To2 : WalletContainerMigration {

    override fun execute(walletContainer: WalletContainer, password: ByteArray): WalletContainer {
        walletContainer.addPrimaryAccount(password)

        return walletContainer
    }
}