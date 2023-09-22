package io.zerion.wallet.core.migrations

import io.zerion.wallet.core.models.WalletContainer

internal interface WalletContainerMigration {

    fun execute(walletContainer: WalletContainer, password: ByteArray): WalletContainer

}