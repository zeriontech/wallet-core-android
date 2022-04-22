# Zerion Wallet Core

This repository contains Wallet Core used by Zerion Android app.

## Examples
> Check androidTests to see all examples.

Define wallet data source, repository and cipherManager:
```kotlin
val cipherManager = CipherManager(context)
val dataSource = WalletDataSourceImpl(cipherManager, "com.mywallet")
val repository = WalletRepositoryImpl(dataSource)
```

Import or create wallet:
```kotlin
val walletContainer = repository.importWalletPersist(mnemonic, password, "My wallet")
// or
val walletContainer = repository.importWalletPersist(privateKey, password, "My wallet")
// 
val walletContainer = repository.createWallet(password, "My wallet")
```

Derive accounts with index or derivation path:
```kotlin
val account0 = walletContainer.deriveAccount(0, password)
val account1 = walletContainer.deriveAccount("m/44'/60'/0'/0/0", password)
```

Access private key via index or derivation path:
```kotlin
val privateKey0 = walletContainer.decryptPrivateKey(0, password).toHex()
val privateKey1 = walletContainer.decryptPrivateKey("m/44'/60'/0'/0/0", password).toHex()
```

Access seed phrase:
```kotlin
val mnemonic = walletContainer.decryptMnemonic(password)
```

Sign transaction:
```kotlin
val transaction = TransactionInput(
            chainID =  ...,
            gasPrice =  ...,
            nonce =  ...,
            gas =  ...,
            toAddress =  ...,
            data =  ...,
            amount = ...,
        )

val signed = Signer.sign(Transaction(transaction), privateKey).toHex()
```

## Dependencies

[TrustWalletCore](https://github.com/trustwallet/wallet-core) - low level wallet cryptography functionality, written in C++ with Java wrappers.
