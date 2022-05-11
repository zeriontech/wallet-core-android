package io.zerion.wallet.core.utils

import io.zerion.wallet.core.utils.WalletContainerParser.JsonKeys.address
import io.zerion.wallet.core.utils.WalletContainerParser.JsonKeys.derivationPath
import io.zerion.wallet.core.exceptions.WalletException.MailformedContainerException
import io.zerion.wallet.core.models.WalletContainer
import io.zerion.wallet.core.models.WalletAccount
import io.zerion.wallet.core.utils.WalletContainerParser.JsonKeys.accounts
import io.zerion.wallet.core.utils.WalletContainerParser.JsonKeys.identifier
import io.zerion.wallet.core.utils.WalletContainerParser.JsonKeys.version
import io.zerion.wallet.core.utils.WalletContainerParser.JsonKeys.wallet
import org.json.JSONObject
import wallet.core.jni.StoredKey

/**
 * Created by rolea on 24.11.2021.
 */
object WalletContainerParser {
    init {
        System.loadLibrary("TrustWalletCore")
    }

    enum class JsonKeys {
        identifier,
        version,
        wallet,
        accounts,
        address,
        index,
        derivationPath
    }

    fun fromJson(json: ByteArray): WalletContainer {
        return fromJson(json.decodeToString())
    }

    private fun fromJson(json: String): WalletContainer {
        try {
            JSONObject(json.replace("\n","")).let {
                val id = it.getString(identifier.name)
                val version = it.getInt(version.name)
                val walletData = it.getJSONObject(wallet.name).toString()
                val name = it.optString("name")
                val wallet = StoredKey.importJSON(walletData.encodeToByteArray())

                val accountsJson = it.getJSONArray(accounts.name)
                val accounts = mutableListOf<WalletAccount>()
                for (index in 0 until accountsJson.length()) {
                    accountsJson.getJSONObject(index).let {
                        WalletAccount(
                            it.getString(address.name),
                            it.optInt(JsonKeys.index.name),
                            it.optString(derivationPath.name)
                        )
                    }
                        .let { accounts.add(it) }
                }

                return WalletContainer(
                    id = id,
                    storedKey = wallet,
                    version = version,
                    name = name,
                    accounts = accounts
                )
            }
        } catch (e: RuntimeException) {
            throw MailformedContainerException()
        }
    }

}