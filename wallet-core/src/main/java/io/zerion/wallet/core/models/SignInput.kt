package io.zerion.wallet.core.models

import com.google.protobuf.ByteString
import io.zerion.wallet.core.extensions.Numeric
import io.zerion.wallet.core.extensions.toHex
import io.zerion.wallet.core.extensions.toHexBytesInByteString
import io.zerion.wallet.core.models.GasPrice.Classic
import io.zerion.wallet.core.models.GasPrice.EIP1559
import wallet.core.jni.proto.Ethereum
import wallet.core.jni.proto.Ethereum.SigningInput.Builder
import wallet.core.jni.proto.Ethereum.TransactionMode.Enveloped
import wallet.core.jni.proto.Ethereum.TransactionMode.Legacy

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
    val gasPrice: GasPrice,
    val gas: ByteString,
    val toAddress: String,
    val data: ByteString,
    val amount: ByteString,
) {

    fun toSignInput(): Builder {
        return Ethereum.SigningInput.newBuilder()
            .let {
                it.chainId = chainID
                it.nonce = nonce
                it.gasLimit = gas
                it.toAddress = toAddress
                when (gasPrice) {
                    is Classic -> {
                        it.gasPrice = this@TransactionInput.gasPrice.gasPrice
                        it.txMode = Legacy
                    }
                    is EIP1559 -> {
                        it.maxInclusionFeePerGas =this@TransactionInput. gasPrice.priorityFeePerGas
                        it.maxFeePerGas = this@TransactionInput.gasPrice.maxFeePerGas
                        it.txMode = Enveloped
                    }
                }
                it.transaction = Ethereum.Transaction.newBuilder().let {
                    it.contractGeneric = Ethereum.Transaction.ContractGeneric.newBuilder().let {
                        it.data = data
                        it.amount = amount
                        it
                    }.build()
                    it
                }.build()
                it
            }
    }

    companion object {

        fun fromString(
            chainID: String,
            classiGasPrice: String?,
            priorityFeePerGas: String?,
            maxFeePerGas: String?,
            nonce: String,
            gas: String,
            toAddress: String,
            data: String,
            amount: String,
        ): TransactionInput {
            val gasPrice = when {
                priorityFeePerGas != null && maxFeePerGas != null -> {
                    EIP1559(
                        Numeric.cleanHexPrefix(priorityFeePerGas).toHexBytesInByteString(),
                        Numeric.cleanHexPrefix(maxFeePerGas).toHexBytesInByteString(),
                    )
                }
                classiGasPrice != null -> {
                    Classic(Numeric.cleanHexPrefix(classiGasPrice).toHexBytesInByteString())
                }
                else -> {
                    throw IllegalStateException("None of the gas price formats are not provided")
                }
            }

            return TransactionInput(
                chainID = Numeric.cleanHexPrefix(chainID).toHexBytesInByteString(),
                gasPrice = gasPrice,
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

sealed class GasPrice {
    data class Classic(val gasPrice: ByteString) : GasPrice()
    data class EIP1559(val priorityFeePerGas: ByteString, val maxFeePerGas: ByteString) : GasPrice()
}
