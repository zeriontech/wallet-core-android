package io.zerion.wallet.core.utils

import io.zerion.wallet.core.exceptions.WalletException
import io.zerion.wallet.core.models.DerivationPath
import io.zerion.wallet.core.models.Index
import wallet.core.jni.CoinType
import wallet.core.jni.Purpose

object DerivationPathFactory {
    init {
        System.loadLibrary("TrustWalletCore")
    }

    fun fromAccount(accountIndex: Int): DerivationPath {
        return DerivationPath(
            listOf(
                Index(Purpose.BIP44.value(), hardened = true), // purpose
                Index(CoinType.ETHEREUM.slip44Id(), hardened = true), // coinType
                Index(accountIndex, hardened = true), // account
                Index(0, hardened = false), // change
                Index(0, hardened = false), // address
            )
        )
    }

    fun fromIndex(addressIndex: Int): DerivationPath {
        return DerivationPath(
            purpose = Purpose.BIP44.value(),
            coinType = CoinType.ETHEREUM.slip44Id(),
            account = 0,
            change = 0,
            address = addressIndex
        )
    }

    fun fromString(pathString: String): DerivationPath {
        try {
            return DerivationPath(pathString)
        } catch (e: RuntimeException) {
            throw WalletException.InvalidDerivationPathException()
        }
    }
}