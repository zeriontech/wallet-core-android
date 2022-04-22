package io.zerion.wallet.core

import io.zerion.wallet.core.models.WalletContainer

/**
 * Created by rolea on 28.11.2021.
 */
interface WalletRepository {
    fun createWallet(password: String, name: String?): WalletContainer

    fun createWalletPersist(password: String, name: String?): WalletContainer

    fun importWallet(input: String, password: String, name: String?): WalletContainer

    fun importWalletPersist(input: String, password: String, name: String?): WalletContainer

    fun load(identifier: String): WalletContainer?

    fun count() : Int

    fun loadAll(): List<WalletContainer>

    fun save(wallet: WalletContainer)

    fun delete(identifier: String)

    fun deleteAll()
}