package io.zerion.wallet.core.exceptions

/**
 * Created by rolea on 28.11.2021.
 */
sealed class WalletException : RuntimeException() {

    class MailformedContainerException : WalletException()
    class UnableToDeriveAccountException : WalletException()
    class InvalidDerivationPathException : WalletException()
    class InvalidPasswordException : WalletException()
    class InvalidMnemonicException : WalletException()
    class InvalidPrivateKeyException : WalletException()
    class InvalidInputException : WalletException()
    class FailedToDecryptPrivateKeyException : WalletException()
    class FailedToDecryptMnemonicException : WalletException()
    class FailedToExportException : WalletException()
    class FailedToImportException : WalletException()
    class FailedToImportMnemonicException : WalletException()
    class FailedToImportPrivateKeyException : WalletException()
    class FailedToAddAccountException : WalletException()
    class FailedToRemoveAccountException : WalletException()
    class FailedToChangePassword: WalletException()
}