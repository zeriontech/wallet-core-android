package io.zerion.wallet.core

import io.zerion.wallet.core.models.WalletContainer

/**
 * Created by rolea on 24.11.2021.
 */
interface WalletDataSource {
    fun load(id: String): WalletContainer?
    fun loadAll(): List<WalletContainer>
    fun save(container: WalletContainer)
    fun delete(id: String)
    fun deleteAll()
    fun count(): Int
}