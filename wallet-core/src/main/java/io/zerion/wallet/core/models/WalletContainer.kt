package io.zerion.wallet.core.models

import io.zerion.wallet.core.exceptions.WalletException
import io.zerion.wallet.core.exceptions.WalletException.FailedToAddAccountException
import io.zerion.wallet.core.exceptions.WalletException.FailedToDecryptMnemonicException
import io.zerion.wallet.core.exceptions.WalletException.FailedToDecryptPrivateKeyException
import io.zerion.wallet.core.exceptions.WalletException.FailedToExportException
import io.zerion.wallet.core.exceptions.WalletException.FailedToRemoveAccountException
import io.zerion.wallet.core.exceptions.WalletException.UnableToDeriveAccountException
import io.zerion.wallet.core.models.WalletContainer.Type.Mnemonic
import io.zerion.wallet.core.utils.DerivationPathFactory
import io.zerion.wallet.core.utils.WalletContainerParser
import io.zerion.wallet.core.utils.WalletContainerParser.JsonKeys
import org.json.JSONArray
import org.json.JSONObject
import wallet.core.jni.CoinType
import wallet.core.jni.CoinType.ETHEREUM
import wallet.core.jni.HDVersion.XPUB
import wallet.core.jni.HDWallet
import wallet.core.jni.PrivateKey
import wallet.core.jni.StoredKey

/**
 * Created by rolea on 24.11.2021.
 */
class WalletContainer private constructor(
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

    var primaryAccount: String? = null
        private set

    var version: Int
        private set

    var accounts: MutableList<WalletAccount>
        private set

    private var coinType: CoinType


    constructor(
        id: String,
        storedKey: StoredKey,
        password: ByteArray
    ) : this(id, storedKey) {
        addPrimaryAccount(password)
    }

    init {
        name = storedKey.name()
        version = currentVersion
        accounts = mutableListOf()
        coinType = ETHEREUM
    }


    constructor(
        id: String,
        storedKey: StoredKey,
        version: Int,
        name: String,
        accounts: MutableList<WalletAccount>,
        primaryAccount: String?,
    ) : this(id, storedKey) {
        this.name = name
        this.version = version
        this.accounts = accounts
        this.coinType = ETHEREUM
        this.primaryAccount = primaryAccount
    }

    private fun deriveAccount(derivationPath: DerivationPath, password: ByteArray): WalletAccount {
        val wallet = try {
            storedKey.wallet(password)
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
        val path = DerivationPathFactory.fromIndex(accountIndex)
        return path.toString()
    }

    fun deriveAccount(derivationPath: String, password: ByteArray): WalletAccount {
        val path = DerivationPathFactory.fromString(derivationPath)
        return deriveAccount(path, password)
    }

    fun deriveAccount(accountIndex: Int, password: ByteArray): WalletAccount {
        val path = DerivationPathFactory.fromIndex(accountIndex)
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
            val path = DerivationPathFactory.fromIndex(index)
            accounts.add(
                createWalletAccount(xpub, path)
            )
        }

        return accounts
    }

    fun addPrimaryAccount(password: ByteArray) {
        primaryAccount = derivePrimaryAccount(password).address
    }

    fun derivePrimaryAccount(password: ByteArray): WalletAccount {
        return when (type) {
            Mnemonic -> {
                deriveAccount(
                    accountIndex = 0,
                    password = password
                )
            }

            Type.PrivateKey -> {
                val keyData = decryptPrimaryPrivateKey(password)
                val privateKey = try {
                    PrivateKey(keyData)
                } catch (e: RuntimeException) {
                    throw UnableToDeriveAccountException()
                }

                WalletAccount(
                    address = coinType.deriveAddress(privateKey),
                    index = null,
                    derivationPath = null
                )
            }
        }
    }

    fun addAccount(password: ByteArray): WalletAccount {
        val index = (accounts.mapNotNull { it.index }.maxByOrNull { it } ?: -1) + 1
        val path = DerivationPathFactory.fromIndex(index)
        return addAccount(path.toString(), password)
    }

    fun addAccount(derivationPath: String, password: ByteArray): WalletAccount {
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
        val path = DerivationPathFactory.fromString(derivationPath)
        return decryptPrivateKey(path, password).data()
    }

    fun decryptPrivateKey(accountIndex: Int, password: String): ByteArray {
        val path = DerivationPathFactory.fromIndex(accountIndex)
        return decryptPrivateKey(path, password).data()
    }

    fun decryptPrimaryPrivateKey(password: ByteArray): ByteArray {
        val privateKey = try {
            storedKey.decryptPrivateKey(password)
        } catch (e: RuntimeException) {
            throw FailedToDecryptPrivateKeyException()
        }
        return privateKey
    }

    fun decryptMnemonic(password: ByteArray): String {
        val mnemonic = try {
            storedKey.decryptMnemonic(password)
        } catch (e: RuntimeException) {
            throw  FailedToDecryptMnemonicException()
        }
        return mnemonic
    }

    fun changePassword(old: ByteArray, new: ByteArray) {
        val newStoredKey: StoredKey?
        when (type) {
            Type.PrivateKey -> {
                val privateKeyData = decryptPrimaryPrivateKey(old)
                newStoredKey = StoredKey.importPrivateKey(
                    privateKeyData,
                    storedKey.name(),
                    new,
                    coinType
                )
            }
            Mnemonic -> {
                val mnemonic = decryptMnemonic(old)
                newStoredKey = StoredKey.importHDWallet(
                    mnemonic,
                    storedKey.name(),
                    new,
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
            put(WalletContainerParser.JsonKeys.primaryAccount.name, primaryAccount)
            put(WalletContainerParser.JsonKeys.accounts.name, accountsArray)
        }
        return container.toString().encodeToByteArray()
    }

    fun incrementVersion() {
        version++
    }


    companion object {

        const val currentVersion = 2

        // load TrustWalletCore
        // essential for all parts of app
        init {
            System.loadLibrary("TrustWalletCore")
        }
    }
}