package io.zerion.wallet.core.migrations

import io.zerion.wallet.core.WalletDataSource
import io.zerion.wallet.core.migrations.impl.WalletContainerMigrationFrom1To2
import io.zerion.wallet.core.models.WalletContainer

internal class WalletContainerMigrator {

    private val migrations: Map<Int, WalletContainerMigration> = mapOf(
        1 to WalletContainerMigrationFrom1To2()
    )

    fun migrate(
        newVersion: Int,
        walletDataSource: WalletDataSource,
        walletContainer: WalletContainer,
        password: ByteArray
    ): WalletContainer {
        if (walletContainer.version >= newVersion) {
            return walletContainer
        }

        var oldVersionCopy = walletContainer.version
        var walletContainerMigrated = walletContainer

        do {
            val migration = migrations[oldVersionCopy]
            migration
                ?.execute(walletContainerMigrated, password)
                ?.let {
                    walletContainerMigrated.incrementVersion()
                    walletContainerMigrated = it
                }
            oldVersionCopy++
        } while (migration != null)

        walletDataSource.save(walletContainerMigrated)

        return walletContainerMigrated
    }
}