package io.zerion.wallet.core.extensions

import wallet.core.jni.CoinType
import wallet.core.jni.Mnemonic
import wallet.core.jni.PrivateKey

/**
 * Created by rolea on 2.12.2021.
 */

fun String.isValidMnemonic(): Boolean {
    return Mnemonic.isValid(this)
}

fun String.isValidPrivateKey(): Boolean {
    if (!this.isHex()) return false
    val data = this.toHexByteArray()
    val coinType = CoinType.ETHEREUM

    return PrivateKey.isValid(data, coinType.curve())
}

fun String.isValidMnemonicWord(): Boolean {
    return Mnemonic.isValidWord(this)
}

fun String.mnemonicWordSuggestions(): String {
    return Mnemonic.suggest(this)
}