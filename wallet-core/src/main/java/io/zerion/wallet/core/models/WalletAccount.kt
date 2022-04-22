package io.zerion.wallet.core.models

/**
 * Created by rolea on 23.11.2021.
 */
class WalletAccount(
    val address: String,
    val index: Int?,
    val derivationPath: String?,
)