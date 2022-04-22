package io.zerion.wallet.core.models

import io.zerion.wallet.core.exceptions.WalletException
import io.zerion.wallet.core.utils.WalletContainerParser
import io.zerion.wallet.core.models.WalletContainer.Type.Mnemonic
import io.zerion.wallet.core.utils.WalletContainerParser.JsonKeys
import io.zerion.wallet.core.exceptions.WalletException.FailedToAddAccountException
import io.zerion.wallet.core.exceptions.WalletException.FailedToDecryptMnemonicException
import io.zerion.wallet.core.exceptions.WalletException.FailedToDecryptPrivateKeyException
import io.zerion.wallet.core.exceptions.WalletException.FailedToExportException
import io.zerion.wallet.core.exceptions.WalletException.FailedToRemoveAccountException
import io.zerion.wallet.core.exceptions.WalletException.InvalidDerivationPathException
import io.zerion.wallet.core.exceptions.WalletException.UnableToDeriveAccountException
import org.json.JSONArray
import org.json.JSONObject
import wallet.core.jni.CoinType
import wallet.core.jni.CoinType.ETHEREUM
import wallet.core.jni.HDVersion.XPUB
import wallet.core.jni.HDWallet
import wallet.core.jni.PrivateKey
import wallet.core.jni.Purpose.BIP44
import wallet.core.jni.StoredKey

/**
 * Created by rolea on 24.11.2021.
 */
class WalletContainer(
    val id: String,
    private var storedKey: StoredKey,
) {

    enum class Type {
        Mnemonic, PrivateKey
    }

    val type: Type
        get() {
            return if (storedKey.isMnemonic) Mnemonic else Type.PrivateKey
        }
    var name: String
    var version: Int
        private set

    var accounts: MutableList<WalletAccount>
        private set

    private var coinType: CoinType

    init {
        name = storedKey.name()
        version = currentVersion
        accounts = mutableListOf()
        this.coinType = ETHEREUM
        migrateVersionIfNeeded()
    }

    constructor(
        id: String,
        storedKey: StoredKey,
        version: Int,
        name: String,
        accounts: MutableList<WalletAccount>,
    ) : this(id, storedKey) {
        this.name = name
        this.version = version
        this.accounts = accounts
        this.coinType = ETHEREUM
        migrateVersionIfNeeded()
    }

    private fun migrateVersionIfNeeded() {
    }

    private fun makeDerivationPath(accountIndex: Int): DerivationPath {
        return DerivationPath(
            BIP44.value(),
            ETHEREUM.slip44Id(),
            0,
            0,
            accountIndex
        )
    }

    private fun makeDerivationPath(path: String): DerivationPath {
        try {
            return DerivationPath(path)
        } catch (e: RuntimeException) {
            throw InvalidDerivationPathException()
        }
    }

    private fun deriveAccount(derivationPath: DerivationPath, password: String): WalletAccount {
        val wallet = try {
            storedKey.wallet(password.toByteArray())
        } catch (e: RuntimeException) {
            throw UnableToDeriveAccountException()
        }

        val xpub = wallet.getExtendedPublicKey(coinType.purpose(), coinType, XPUB)!!
        return createWalletAccount(xpub, derivationPath)
    }

    private fun createWalletAccount(
        xpub: String,
        derivationPath: DerivationPath,
    ): WalletAccount {
        val pubkey = HDWallet.getPublicKeyFromExtended(xpub, coinType, derivationPath.toString())!!
        val address = coinType.deriveAddressFromPublicKey(pubkey)

        return WalletAccount(
            address,
            derivationPath.address,
            derivationPath.toString()
        )
    }

    private fun decryptPrivateKey(derivationPath: DerivationPath, password: String): PrivateKey {
        val wallet = try {
            storedKey.wallet(password.toByteArray())
        } catch (e: RuntimeException) {
            throw FailedToDecryptPrivateKeyException()
        }
        return wallet.getKey(coinType, derivationPath.toString())
    }

    fun derivationPath(accountIndex: Int): String {
        val path = makeDerivationPath(accountIndex)
        return path.toString()
    }

    fun deriveAccount(derivationPath: String, password: String): WalletAccount {
        val path = makeDerivationPath(derivationPath)
        return deriveAccount(path, password)
    }

    fun deriveAccount(accountIndex: Int, password: String): WalletAccount {
        val path = makeDerivationPath(accountIndex)
        return deriveAccount(path, password)
    }

    fun deriveAccounts(fromIndex: Int, toIndex: Int, password: String): List<WalletAccount> {
        val wallet = try {
            storedKey.wallet(password.toByteArray())
        } catch (e: RuntimeException) {
            throw UnableToDeriveAccountException()
        }

        val xpub = wallet.getExtendedPublicKey(coinType.purpose(), coinType, XPUB)
        val accounts = mutableListOf<WalletAccount>()

        for (index in fromIndex..toIndex) {
            val path = makeDerivationPath(index)
            accounts.add(
                createWalletAccount(xpub, path)
            )
        }

        return accounts
    }

    fun derivePrimaryAccount(password: String): WalletAccount {
        val keyData = decryptPrimaryPrivateKey(password)
        val privateKey = try {
            PrivateKey(keyData)
        } catch (e: RuntimeException) {
            throw UnableToDeriveAccountException()
        }

        return WalletAccount(
            coinType.deriveAddress(privateKey),
            null,
            null
        )
    }

    fun addAccount(password: String): WalletAccount {
        val index = (accounts.mapNotNull { it.index }.maxByOrNull { it } ?: -1) + 1
        val path = derivationPath(index)
        return addAccount(path, password)
    }

    fun addAccount(derivationPath: String, password: String): WalletAccount {
        return when (type) {
            Mnemonic -> {
                if (hasAccount(derivationPath)) {
                    throw FailedToAddAccountException()
                }
                val account = deriveAccount(derivationPath, password)
                accounts.add(account)
                account
            }
            Type.PrivateKey -> {
                val account = derivePrimaryAccount(password)
                accounts = mutableListOf(account)
                account
            }
        }
    }

    fun hasAccount(derivationPath: String): Boolean {
        return accounts.any { it.derivationPath == derivationPath }
    }

    fun removeAccount(derivationPath: String) {
        accounts
            .indexOfFirst { it.derivationPath == derivationPath }
            .also {
                if (it == -1) throw FailedToRemoveAccountException()
                accounts.removeAt(it)
            }
    }

    fun decryptPrivateKey(derivationPath: String, password: String): ByteArray {
        val path = makeDerivationPath(derivationPath)
        return decryptPrivateKey(path, password).data()
    }

    fun decryptPrivateKey(accountIndex: Int, password: String): ByteArray {
        val path = makeDerivationPath(accountIndex)
        return decryptPrivateKey(path, password).data()
    }

    fun decryptPrimaryPrivateKey(password: String): ByteArray {
        val privateKey = try {
            storedKey.decryptPrivateKey(password.toByteArray())
        } catch (e: RuntimeException) {
            throw  FailedToDecryptPrivateKeyException()
        }
        return privateKey
    }

    fun decryptMnemonic(password: String): String {
        val mnemonic = try {
            storedKey.decryptMnemonic(password.toByteArray())
        } catch (e: RuntimeException) {
            throw  FailedToDecryptMnemonicException()
        }
        return mnemonic
    }

    fun changePassword(old: String, new: String) {
        val newStoredKey: StoredKey?
        when (type) {
            Type.PrivateKey -> {

                val privateKeyData = decryptPrimaryPrivateKey(old)
                newStoredKey = StoredKey.importPrivateKey(
                    privateKeyData,
                    storedKey.name(),
                    new.encodeToByteArray(),
                    coinType
                )
            }
            Mnemonic -> {
                val mnemonic = decryptMnemonic(old)
                newStoredKey = StoredKey.importHDWallet(
                    mnemonic,
                    storedKey.name(),
                    new.encodeToByteArray(),
                    coinType
                )
            }
        }
        if (newStoredKey == null)
                throw WalletException.FailedToChangePassword()
        storedKey = newStoredKey
    }

    fun export(): ByteArray {
        val walletData = storedKey.exportJSON()
        val walletJson = try {
            JSONObject(walletData.decodeToString())
        } catch (e: RuntimeException) {
            throw FailedToExportException()
        }

        val container = JSONObject().apply {
            put(WalletContainerParser.JsonKeys.identifier.name, id)
            put(WalletContainerParser.JsonKeys.version.name, version)
            put("name", name)
            put(WalletContainerParser.JsonKeys.wallet.name, walletJson)
            val accountsArray = JSONArray().apply {
                accounts.forEach {
                    val accountJson = JSONObject()
                    accountJson.put(JsonKeys.address.name, it.address)
                    accountJson.put(JsonKeys.index.name, it.index)
                    accountJson.put(JsonKeys.derivationPath.name, it.derivationPath)
                    put(accountJson)
                }
            }
            put(WalletContainerParser.JsonKeys.accounts.name, accountsArray)
        }
        return container.toString().encodeToByteArray()
    }

    companion object {

        private const val currentVersion = 1
    }
}