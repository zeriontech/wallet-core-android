package io.zerion.wallet.core.implementations

import io.zerion.wallet.core.WalletDataSource
import io.zerion.wallet.core.WalletRepository
import io.zerion.wallet.core.exceptions.WalletException
import io.zerion.wallet.core.extensions.isValidMnemonic
import io.zerion.wallet.core.extensions.isValidPrivateKey
import io.zerion.wallet.core.extensions.toHexByteArray
import io.zerion.wallet.core.migrations.WalletContainerMigrator
import io.zerion.wallet.core.models.WalletContainer
import io.zerion.wallet.core.utils.WalletContainerParser
import wallet.core.jni.CoinType
import wallet.core.jni.StoredKey
import wallet.core.jni.StoredKeyEncryptionLevel
import java.util.UUID

/**
 * Created by rolea on 01.12.2021
 */
class WalletRepositoryImpl(private val dataSource: WalletDataSource) : WalletRepository {
    private val walletContainerMigrator = WalletContainerMigrator()

    override fun createWallet(password: String, name: String?): WalletContainer {
        if (password.trim().isEmpty())
            throw WalletException.InvalidPasswordException()

        val id = generateId()
        val storedKey = StoredKey(name ?: makeWalletName(), password.toByteArray(), StoredKeyEncryptionLevel.STANDARD)

        return WalletContainer(
            id = id,
            storedKey = storedKey,
            password = password.toByteArray()
        ).apply { addAccount(password.toByteArray()) }
    }

    override fun createWalletPersist(password: String, name: String?): WalletContainer {
        val wallet = createWallet(password, name)
        save(wallet)
        return wallet
    }

    override fun importWallet(input: String, password: String, name: String?): WalletContainer {
        if (password.isEmpty())
            throw WalletException.InvalidPasswordException()

        return when {
            input.isValidMnemonic() -> {
                importFromMnemonic(input, password, name)
            }
            input.isValidPrivateKey() -> {
                importFromPrivateKey(input, password, name)
            }
            else -> {
                throw WalletException.FailedToImportException()
            }
        }
    }

    override fun importWalletPersist(input: String, password: String, name: String?): WalletContainer {
        val wallet = importWallet(input, password, name)
        save(wallet)
        return wallet
    }

    override fun changePassword(old: ByteArray, new: ByteArray) {
        loadAll().forEach {
            it.changePassword(
                old = old,
                new = new
            )
            save(it)
        }
    }

    override fun exportWalletContainer(
        identifier: String,
        password: ByteArray,
        exportPassword: ByteArray
    ): WalletContainer {
        val walletContainer = load(identifier)
                ?.apply {
                    walletContainerMigrator.migrate(
                        newVersion = WalletContainer.currentVersion,
                        walletDataSource = dataSource,
                        walletContainer = this,
                        password = password,
                    )
                } ?: throw WalletException.MailformedContainerException()

        walletContainer.changePassword(password, exportPassword)
        return walletContainer
    }

    override fun importWalletContainer(
        walletContainerByteArray: ByteArray,
        password: ByteArray,
        walletContainerPassword: ByteArray,
    ): WalletContainer {
        val walletContainer = WalletContainerParser.fromJson(walletContainerByteArray)
        walletContainer.changePassword(walletContainerPassword, password)
        return walletContainer
    }

    override fun load(identifier: String): WalletContainer? {
        return dataSource.load(identifier)
    }

    override fun count(): Int {
        return dataSource.count()
    }

    override fun loadAll(): List<WalletContainer> {
        return dataSource.loadAll()
    }

    override fun save(wallet: WalletContainer) {
        dataSource.save(wallet)
    }

    override fun delete(identifier: String) {
        dataSource.delete(identifier)
    }

    override fun deleteAll() {
        dataSource.deleteAll()
    }

    override fun migrateVersionsIfNeeded(password: ByteArray) {
        loadAll()
            .filter { it.version < WalletContainer.currentVersion }
            .forEach {
                walletContainerMigrator.migrate(
                    newVersion = WalletContainer.currentVersion,
                    walletDataSource = dataSource,
                    walletContainer = it,
                    password = password
                )
            }
    }


    private fun generateId(): String {
        return UUID.randomUUID().toString().lowercase()
    }

    private fun makeWalletName(): String {
        return "Wallet Group #${dataSource.count() + 1}"
    }

    private fun importFromMnemonic(
        input: String,
        password: String,
        name: String?
    ): WalletContainer {
        val coin = CoinType.ETHEREUM
        val id = generateId()
        val storedKey = StoredKey.importHDWallet(
            input,
            name ?: makeWalletName(),
            password.toByteArray(),
            coin
        )

        return WalletContainer(
            id = id,
            storedKey = storedKey,
            password = password.toByteArray()
        )
    }

    private fun importFromPrivateKey(input: String, password: String, name: String?): WalletContainer {
        val coin = CoinType.ETHEREUM
        val id = generateId()
        val storedKey: StoredKey = StoredKey.importPrivateKey(
            input.toHexByteArray(),
            name ?: makeWalletName(),
            password.toByteArray(),
            coin
        ) ?: throw WalletException.FailedToImportPrivateKeyException()

        return WalletContainer(
            id = id,
            storedKey = storedKey,
            password = password.toByteArray()
        ).apply { addAccount(password.toByteArray()) }
    }


    companion object {
        // load TrustWalletCore
        // essential for all parts of app
        init {
            System.loadLibrary("TrustWalletCore")
        }
    }
}