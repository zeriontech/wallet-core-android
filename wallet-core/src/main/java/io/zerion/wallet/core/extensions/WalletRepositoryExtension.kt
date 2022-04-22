package io.zerion.wallet.core.extensions

import io.zerion.wallet.core.models.WalletContainer
import io.zerion.wallet.core.WalletRepository

/**
 * Created by rolea on 11.01.2022.
 */

fun WalletRepository.delete(walletContainer: WalletContainer) {
    this.delete(walletContainer.id)
}