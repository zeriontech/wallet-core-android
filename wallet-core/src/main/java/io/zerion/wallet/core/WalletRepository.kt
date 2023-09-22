package io.zerion.wallet.core

import io.zerion.wallet.core.models.WalletContainer

/**
 * Created by rolea on 28.11.2021.
 */
interface WalletRepository {

    fun migrateVersionsIfNeeded(password: ByteArray)

    fun createWallet(
        password: String,
        name: String?
    ): WalletContainer

    fun createWalletPersist(
        password: String,
        name: String?
    ): WalletContainer

    fun importWallet(
        input: String,
        password: String,
        name: String?
    ): WalletContainer

    fun importWalletPersist(
        input: String,
        password: String,
        name: String?
    ): WalletContainer

    fun changePassword(old: ByteArray, new: ByteArray)

    fun load(identifier: String): WalletContainer?

    fun count() : Int

    fun loadAll(): List<WalletContainer>

    fun save(wallet: WalletContainer)

    fun delete(identifier: String)

    fun deleteAll()

    fun exportWalletContainer(
        identifier: String,
        password: ByteArray,
        exportPassword: ByteArray
    ): WalletContainer
    fun importWalletContainer(
        walletContainerByteArray: ByteArray,
        password: ByteArray,
        walletContainerPassword: ByteArray
    ): WalletContainer
}