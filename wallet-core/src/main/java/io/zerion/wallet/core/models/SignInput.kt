package io.zerion.wallet.core.models

import com.google.protobuf.ByteString
import io.zerion.wallet.core.extensions.Numeric
import io.zerion.wallet.core.extensions.toHexBytesInByteString

/**
 * Created by rolea on 20.02.2022.
 */
sealed class SignInput {

    class Sign(val data: ByteArray) : SignInput()
    class PersonalSign(val data: ByteArray) : SignInput()
    class TypedData(val data: ByteArray) : SignInput()
    class Transaction(val data: TransactionInput) : SignInput()
}

data class TransactionInput(
    val chainID: ByteString,
    val nonce: ByteString,
    val gasPrice: ByteString,
    val gas: ByteString,
    val toAddress: String,
    val data: ByteString,
    val amount: ByteString,
) {

    companion object {

        fun fromString(
            chainID: String,
            gasPrice: String,
            nonce: String,
            gas: String,
            toAddress: String,
            data: String,
            amount: String,
        ): TransactionInput {
            return TransactionInput(
                chainID = Numeric.cleanHexPrefix(chainID).toHexBytesInByteString(),
                gasPrice = Numeric.cleanHexPrefix(gasPrice).toHexBytesInByteString(),
                nonce = Numeric.cleanHexPrefix(nonce).toHexBytesInByteString(),
                gas = Numeric.cleanHexPrefix(gas).toHexBytesInByteString(),
                toAddress = toAddress,
                data = if (data.isBlank()) ByteString.EMPTY else Numeric.cleanHexPrefix(data)
                    .toHexBytesInByteString(),
                amount = Numeric.cleanHexPrefix(amount).toHexBytesInByteString()
            )
        }
    }
}
