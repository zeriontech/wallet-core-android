package io.zerion.wallet.core

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.zerion.wallet.core.extensions.toHex
import io.zerion.wallet.core.implementations.CipherManager
import io.zerion.wallet.core.implementations.WalletDataSourceImpl
import io.zerion.wallet.core.implementations.WalletRepositoryImpl
import io.zerion.wallet.core.models.WalletContainer
import io.zerion.wallet.core.utils.WalletContainerParser
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.util.UUID

/**
 * Created by rolea on 15.12.2021.
 */
class WalletRepositoryTest {


    private lateinit var instrumentationContext: Context
    private lateinit var repo: WalletRepositoryImpl
    private lateinit var dataSource: WalletDataSource
    private lateinit var password: String
    private lateinit var passwordBytes: ByteArray

    val mnemonic = "genre allow company blind security cluster cost stock skate wait debris subway"
    val privateKey = "15b30fbf6d02f91412755a27ad1402f75a0068dfae968420095c6b632d54f816"

    val containerMnemonicJsonV1: String by lazy {
        val stream = String.javaClass.classLoader.getResourceAsStream("mnemonic_container_v1.json")
        return@lazy stream.bufferedReader().use(BufferedReader::readText)
    }

    val containerPrivateKeyJsonV1: String by lazy {
        val stream =
            String.javaClass.classLoader.getResourceAsStream("privateKey_container_v1.json")
        return@lazy stream.bufferedReader().use(BufferedReader::readText)
    }

    val containerMnemonicJsonV2: String by lazy {
        val stream = String.javaClass.classLoader.getResourceAsStream("mnemonic_container_v2.json")
        return@lazy stream.bufferedReader().use(BufferedReader::readText)
    }

    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().targetContext
        password = UUID.randomUUID().toString()
        passwordBytes = password.toByteArray()
        val cipherManager = CipherManager(instrumentationContext)
        dataSource = WalletDataSourceImpl(cipherManager, "com.mywallet")
        repo = WalletRepositoryImpl(dataSource)
    }

    @After
    fun cleanup() {
        repo.deleteAll()
    }

    @Test
    fun testSuccessfullMigration() {
        val password = "12345678".toByteArray()
        val expectedVersion = 2

        repo.save(WalletContainerParser.fromJson(containerMnemonicJsonV1.encodeToByteArray()))
        repo.save(WalletContainerParser.fromJson(containerPrivateKeyJsonV1.encodeToByteArray()))

        repo.migrateVersionsIfNeeded(password)

        repo.loadAll()
            .forEach {
            Assert.assertEquals(expectedVersion, it.version)
            when (it.type) {
                WalletContainer.Type.Mnemonic -> {
                    Assert.assertEquals(
                        "0xED4a971eA7948B79265C3CA0b9F79D9b56c0022d",
                        it.primaryAccount
                    )
                }

                WalletContainer.Type.PrivateKey -> {
                    Assert.assertEquals(
                        "0x7467594Dd44629415864Af5BcBf861b0C886afAD",
                        it.primaryAccount
                    )
                }
            }
        }
    }

    @Test
    fun testImportMnemonic() {
        val container: WalletContainer = repo.importWalletPersist(mnemonic, password, null)

        Assert.assertEquals(container.type, WalletContainer.Type.Mnemonic)
        Assert.assertEquals(container.decryptMnemonic(passwordBytes), mnemonic)
        container.export() // assert no throw
        assert(container.accounts.isEmpty())
        Assert.assertEquals(
            container.deriveAccount(0, passwordBytes).address,
            "0xED4a971eA7948B79265C3CA0b9F79D9b56c0022d"
        )

        Assert.assertEquals(
            "0xED4a971eA7948B79265C3CA0b9F79D9b56c0022d",
            container.primaryAccount
        )

        Assert.assertEquals(
            container.deriveAccount(1, passwordBytes).address,
            "0x7467594Dd44629415864Af5BcBf861b0C886afAD"
        )

        Assert.assertEquals(
            container.deriveAccount(2, passwordBytes).address,
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

        Assert.assertEquals(2, container.version)
    }

    @Test
    fun testImportPrivateKey() {
        val container = repo.importWalletPersist(privateKey, password, null)
        Assert.assertEquals(container.type, WalletContainer.Type.PrivateKey)
        Assert.assertEquals(container.accounts.size, 1)
        Assert.assertEquals(
            container.decryptPrimaryPrivateKey(passwordBytes).toHex(cleanPrefix = true), privateKey
        )
        container.export() // not throw
        Assert.assertEquals(
            "0x7467594Dd44629415864Af5BcBf861b0C886afAD",
            container.derivePrimaryAccount(passwordBytes).address,
        )
        Assert.assertEquals(
            "0x7467594Dd44629415864Af5BcBf861b0C886afAD",
            container.primaryAccount
        )
        Assert.assertEquals(
            2,
            container.version
        )
    }

    @Test
    fun testCreateWallet() {
        val container = repo.createWallet(password, null)
        Assert.assertEquals(2, container.version)
        Assert.assertEquals(
            container.derivePrimaryAccount(passwordBytes).address,
            container.primaryAccount
        )
        Assert.assertEquals(container.type, WalletContainer.Type.Mnemonic)
        Assert.assertEquals(container.accounts.size, 1)
        container.decryptMnemonic(passwordBytes) // assert not throw
        container.export()
    }

    @Test
    fun testExportNotMigratedWalletAsMigrated() {
        val expectedContainer: WalletContainer =
            WalletContainerParser.fromJson(containerMnemonicJsonV2.toByteArray())

        val walletV1 = WalletContainerParser.fromJson(containerMnemonicJsonV1.toByteArray())
        val password = "12345678".toByteArray()
        repo.save(walletV1)

        val newPassword = UUID.randomUUID().toString().toByteArray()

        val actualWalletContainer: WalletContainer = repo.exportWalletContainer(
            walletV1.id,
            password,
            newPassword
        )

        Assert.assertEquals(2, actualWalletContainer.version)
        Assert.assertEquals(expectedContainer.primaryAccount, actualWalletContainer.primaryAccount)
    }

    @Test
    fun testExportWallet() {
        val expectedContainer: WalletContainer =
            WalletContainerParser.fromJson(containerMnemonicJsonV2.toByteArray())
        val password = "12345678".toByteArray()
        repo.save(expectedContainer)
        val expectedWalletContainerId = expectedContainer.id
        val newPassword = UUID.randomUUID().toString().toByteArray()

        val actualWalletContainer: WalletContainer = repo.exportWalletContainer(
            expectedWalletContainerId,
            password,
            newPassword
        )

        actualWalletContainer.decryptMnemonic(newPassword) // assert not throw
        Assert.assertEquals(expectedWalletContainerId, actualWalletContainer.id)
        Assert.assertEquals(expectedContainer.decryptMnemonic(password) , actualWalletContainer.decryptMnemonic(newPassword))
        Assert.assertEquals(expectedContainer.type, actualWalletContainer.type)
        Assert.assertEquals(expectedContainer.accounts.size, actualWalletContainer.accounts.size)
        Assert.assertEquals(expectedContainer.accounts[0].address, actualWalletContainer.accounts[0].address)
        Assert.assertEquals(expectedContainer.version, actualWalletContainer.version)
    }

    @Test
    fun testImportFromByteArrayWallet() {
        val expectedContainer: WalletContainer =
            WalletContainerParser.fromJson(containerMnemonicJsonV2.toByteArray())
        val password = "12345678".toByteArray()
        val expectedContainerId = expectedContainer.id
        val containerExported = expectedContainer.export()

        val newPassword = UUID.randomUUID().toString().toByteArray()

        val actualWalletContainer = repo.importWalletContainer(
            walletContainerByteArray = containerExported,
            password = newPassword,
            walletContainerPassword = password
        )

        actualWalletContainer.decryptMnemonic(newPassword) // assert not throw
        Assert.assertEquals(expectedContainerId, actualWalletContainer.id)
        Assert.assertEquals(
            expectedContainer.decryptMnemonic(password),
            actualWalletContainer.decryptMnemonic(newPassword)
        )
        Assert.assertEquals(expectedContainer.type, actualWalletContainer.type)
        Assert.assertEquals(expectedContainer.accounts.size, actualWalletContainer.accounts.size)
        Assert.assertEquals(
            expectedContainer.accounts[0].address,
            actualWalletContainer.accounts[0].address
        )
        Assert.assertEquals(expectedContainer.version, actualWalletContainer.version)
        Assert.assertEquals(expectedContainer.primaryAccount, actualWalletContainer.primaryAccount)
        Assert.assertEquals(expectedContainer.version, actualWalletContainer.version)
    }
}