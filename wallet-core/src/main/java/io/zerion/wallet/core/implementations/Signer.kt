package io.zerion.wallet.core.implementations

import com.google.protobuf.ByteString
import io.zerion.wallet.core.models.SignInput
import io.zerion.wallet.core.models.SignInput.PersonalSign
import io.zerion.wallet.core.models.SignInput.Sign
import io.zerion.wallet.core.models.SignInput.Transaction
import io.zerion.wallet.core.models.SignInput.TypedData
import io.zerion.wallet.core.models.TransactionInput
import wallet.core.java.AnySigner
import wallet.core.jni.CoinType
import wallet.core.jni.CoinType.ETHEREUM
import wallet.core.jni.EthereumAbi
import wallet.core.jni.Hash
import wallet.core.jni.PrivateKey
import wallet.core.jni.proto.Ethereum

/**
 * Created by rolea on 21.04.2022.
 */
object Signer {

    fun sign(input: SignInput, privateKey: ByteArray): ByteArray {
        return when (input) {
            is PersonalSign -> sign(input.data, privateKey, true)
            is Sign -> sign(input.data, privateKey, false)
            is Transaction -> sign(input.data, privateKey)
            is TypedData -> sign(input.data, privateKey)
        }
    }

    private fun makePrefixedDataHash(data: ByteArray): ByteArray {
        val prefixString = "\u0019Ethereum Signed Message:\n" + data.count().toString()
        val prefixData = prefixString.encodeToByteArray()
        return Hash.keccak256(prefixData + data)
    }

    private fun sign(data: ByteArray, privateData: ByteArray, addPrefix: Boolean): ByteArray {
        val privateKey = createPrivateKey(privateData)
        val digest = if (addPrefix) makePrefixedDataHash(data) else data

        return privateKey.sign(digest, ETHEREUM.curve())
    }

    private fun createPrivateKey(privateKey: ByteArray): PrivateKey {
        return PrivateKey(privateKey)
    }

    private fun sign(transaction: TransactionInput, privateKey: ByteArray): ByteArray {
        val input = Ethereum.SigningInput.newBuilder()
            .apply {
                chainId = transaction.chainID
                nonce = transaction.nonce
                gasPrice = transaction.gasPrice
                gasLimit = transaction.gas
                toAddress = transaction.toAddress
                this.privateKey = ByteString.copyFrom(privateKey)
                this.transaction = Ethereum.Transaction.newBuilder().apply {
                    contractGeneric = Ethereum.Transaction.ContractGeneric.newBuilder().apply {
                        data = transaction.data
                        amount = transaction.amount
                    }.build()
                }.build()
            }.build()

        val output = AnySigner.sign(input, CoinType.FANTOM, Ethereum.SigningOutput.parser())
        return output.encoded.toByteArray()
    }

    private fun sign(typedData: ByteArray, privateData: ByteArray): ByteArray {
        val privateKey = createPrivateKey(privateData)
        val typedDataJson = typedData.decodeToString()
        val digest = EthereumAbi.encodeTyped(typedDataJson)

        return privateKey.sign(digest, ETHEREUM.curve())
    }
}