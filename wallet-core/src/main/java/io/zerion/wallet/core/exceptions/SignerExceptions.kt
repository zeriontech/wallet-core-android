package io.zerion.wallet.core.exceptions

import java.lang.RuntimeException

/**
 * Created by rolea on 21.04.2022.
 */
sealed class SignerExceptions: RuntimeException() {
    class FailedToSign : SignerExceptions()
}
