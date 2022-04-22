package io.zerion.wallet.core.implementations

import io.zerion.wallet.core.WalletDataSource
import io.zerion.wallet.core.models.WalletContainer
import io.zerion.wallet.core.utils.WalletContainerParser

/**
 * Created by rolea on 24.11.2021.
 */
class WalletDataSourceImpl(
    private val cipherManager: CipherManager,
    walletName: String,
) : WalletDataSource {

    private val prefix = "$walletName."

    override fun load(id: String): WalletContainer? {
        return loadByKey(createKey(id))
    }

    override fun loadAll(): List<WalletContainer> {
        val result = mutableListOf<WalletContainer>()
        cipherManager
            .getAllAliases(prefix)
            .forEach {
                loadByKey(it)?.let { walletContainer ->
                    result += walletContainer
                }
            }

        return result
    }

    override fun save(container: WalletContainer) {
        cipherManager.put(createKey(container.id), container.export())
    }

    override fun delete(id: String) {
        cipherManager.remove(id)
    }

    override fun deleteAll() {
        cipherManager.getAllAliases(prefix)
            .forEach {
                cipherManager.remove(it)
            }
    }

    override fun count(): Int {
        return cipherManager.countEntry(prefix)
    }

    private fun createKey(id: String): String {
        return "$prefix$id"
    }

    private fun loadByKey(id: String): WalletContainer? {
        try {
            cipherManager.get(id)
                .let {
                    return WalletContainerParser.fromJson(it)
                }
        } catch (e: RuntimeException) {
            return null
        }
    }
}