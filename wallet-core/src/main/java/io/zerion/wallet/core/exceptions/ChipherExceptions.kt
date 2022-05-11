package io.zerion.wallet.core.exceptions

import java.lang.RuntimeException

/**
 * Created by rolea on 21.11.2021.
 */
sealed class CipherException(reason: String): RuntimeException(reason) {
    class FailedLoadKeyStore(reason: String) : CipherException(reason)
    class CorruptedFileName(reason: String): CipherException(reason)
}