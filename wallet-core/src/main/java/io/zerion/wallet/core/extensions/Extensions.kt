package io.zerion.wallet.core.extensions

import com.google.protobuf.ByteString

/**
 * Created by rolea on 24.11.2021.
 */
fun ByteArray.toHex(cleanPrefix: Boolean = true): String {
    val hex = Numeric.toHexString(this)
    return if (cleanPrefix) Numeric.cleanHexPrefix(hex)
    else hex
}

fun String.toHexBytes(): ByteArray {
    return Numeric.hexStringToByteArray(this)
}

fun String.toHexByteArray(): ByteArray {
    return Numeric.hexStringToByteArray(this)
}

fun String.isHex():Boolean {
    val regex = Regex("-?[0-9a-fA-F]+")
    return Numeric.cleanHexPrefix(this).matches(regex)
}

fun String.toByteString(): ByteString {
    return ByteString.copyFrom(this, Charsets.UTF_8)
}

fun String.toHexBytesInByteString(): ByteString {
    return ByteString.copyFrom(this.toHexBytes())
}