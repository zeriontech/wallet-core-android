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
 * Created by rolea on 14.12.2022.
 */
object Signer {

    init {
        System.loadLibrary("TrustWalletCore")
    }

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

        val signed = privateKey.sign(digest, ETHEREUM.curve())
        // transform V to yellow paper
        // https://github.com/ethereum/EIPs/issues/155 this rule applies to trx and doesn't require fix
        // learning paper: https://medium.com/mycrypto/the-magic-of-digital-signatures-on-ethereum-98fe184dc9c7
        signed[64] = (signed[64] + 27).toByte()
        return signed
    }

    private fun createPrivateKey(privateKey: ByteArray): PrivateKey {
        return PrivateKey(privateKey)
    }

    private fun sign(transaction: TransactionInput, privateKey: ByteArray): ByteArray {
        val input = transaction.toSignInput()
            .apply {
                this.privateKey = ByteString.copyFrom(privateKey)
            }.build()

        val output = AnySigner.sign(input, CoinType.FANTOM, Ethereum.SigningOutput.parser())
        return output.encoded.toByteArray()
    }

    private fun sign(typedData: ByteArray, privateData: ByteArray): ByteArray {
        val privateKey = createPrivateKey(privateData)
        val typedDataJson = typedData.decodeToString()
        val digest = EthereumAbi.encodeTyped(typedDataJson)

        val signed = privateKey.sign(digest, ETHEREUM.curve())
        // transform V to yellow paper
        // https://github.com/ethereum/EIPs/issues/155 this rule applies to trx and doesn't require fix
        // learning paper: https://medium.com/mycrypto/the-magic-of-digital-signatures-on-ethereum-98fe184dc9c7
        signed[64] = (signed[64] + 27).toByte()
        return signed
    }
}