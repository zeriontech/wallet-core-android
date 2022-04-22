package io.zerion.wallet.core.models

import com.google.protobuf.ByteString

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
    val chainID : ByteString,
    val nonce : ByteString,
    val gasPrice : ByteString,
    val gas : ByteString,
    val toAddress: String,
    val data : ByteString,
    val amount : ByteString,
)
