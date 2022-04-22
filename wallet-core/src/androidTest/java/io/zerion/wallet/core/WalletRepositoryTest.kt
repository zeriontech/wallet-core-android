package io.zerion.wallet.core

import io.zerion.wallet.core.extensions.toHex
import io.zerion.wallet.core.implementations.WalletRepositoryImpl
import io.zerion.wallet.core.models.WalletContainer
import org.junit.*
import org.mockito.Mockito.*
import java.util.UUID

/**
 * Created by rolea on 15.12.2021.
 */
class WalletRepositoryTest {

    private lateinit var repo: WalletRepositoryImpl
    private lateinit var dataSource: WalletDataSource
    private lateinit var password: String

    val mnemonic = "genre allow company blind security cluster cost stock skate wait debris subway"
    val privateKey = "15b30fbf6d02f91412755a27ad1402f75a0068dfae968420095c6b632d54f816"

    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Before
    fun setup() {
        password = UUID.randomUUID().toString()
        dataSource = mock(WalletDataSource::class.java)
        repo = WalletRepositoryImpl(dataSource)
    }

    @Test
    fun testImportMnemonic() {
        val container: WalletContainer = repo.importWalletPersist(mnemonic, password, null)

        Assert.assertEquals(container.type, WalletContainer.Type.Mnemonic)
        Assert.assertEquals(container.decryptMnemonic(password), mnemonic)
        container.export() // assert no throw
        assert(container.accounts.isEmpty())
        Assert.assertEquals(
            container.deriveAccount(0, password).address,
            "0xED4a971eA7948B79265C3CA0b9F79D9b56c0022d"
        )

        Assert.assertEquals(
            container.deriveAccount(1, password).address,
            "0x7467594Dd44629415864Af5BcBf861b0C886afAD"
        )

        Assert.assertEquals(
            container.deriveAccount(2, password).address,
            "0x04b9aB3Be467cbB98f275B266952977116FF59b7"
        )

        Assert.assertEquals(
            container.decryptPrivateKey(0, password).toHex(true),
            "dbe95804848004ef312ee1877eb5af4eaf4692a8e04ff97649edbc3c71f4f656"
        )

        Assert.assertEquals(
            container.decryptPrivateKey(1, password).toHex(true),
            "15b30fbf6d02f91412755a27ad1402f75a0068dfae968420095c6b632d54f816"
        )

        Assert.assertEquals(
            container.decryptPrivateKey(2, password).toHex(true),
            "5bceb69fcc15f63cc30f44f403f9899638aa2a0758ae55719e5690e26f0ccb3b"
        )
    }

    @Test
    fun testImportPrivateKey() {
        val container = repo.importWalletPersist(privateKey, password, null)
        Assert.assertEquals(container.type, WalletContainer.Type.PrivateKey)
        Assert.assertEquals(container.accounts.size, 1)
        Assert.assertEquals(container.decryptPrimaryPrivateKey(password).toHex(cleanPrefix = true), privateKey)
        container.export() // not throw
        Assert.assertEquals(
            container.derivePrimaryAccount(password).address,
            "0x7467594Dd44629415864Af5BcBf861b0C886afAD"
        )
    }

    @Test
    fun testCreateWallet() {
        val container = repo.createWallet(password, null)
        Assert.assertEquals(container.type, WalletContainer.Type.Mnemonic)
        Assert.assertEquals(container.accounts.size, 1)
        container.decryptMnemonic(password) // assert not throw
        container.export()
    }
}