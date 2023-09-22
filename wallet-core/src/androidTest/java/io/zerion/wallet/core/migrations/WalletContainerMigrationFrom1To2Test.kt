package io.zerion.wallet.core.migrations

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.zerion.wallet.core.implementations.CipherManager
import io.zerion.wallet.core.implementations.WalletDataSourceImpl
import io.zerion.wallet.core.implementations.WalletRepositoryImpl
import io.zerion.wallet.core.utils.WalletContainerParser
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader

class WalletContainerMigrationFrom1To2Test {

    private lateinit var instrumentationContext: Context
    private lateinit var repo: WalletRepositoryImpl
    private lateinit var dataSource: WalletDataSourceImpl
    private lateinit var cipherManager: CipherManager
    private lateinit var walletContainerMigrator: WalletContainerMigrator

    val password = "12345678"
    val passwordByteArray = password.toByteArray()

    private val containerMnemonicJsonV1: String by lazy {
        val stream = String.javaClass.classLoader.getResourceAsStream("mnemonic_container_v1.json")
        return@lazy stream.bufferedReader().use(BufferedReader::readText)
    }

    private val containerMnemonicJsonV2: String by lazy {
        val stream = String.javaClass.classLoader.getResourceAsStream("mnemonic_container_v2.json")
        return@lazy stream.bufferedReader().use(BufferedReader::readText)
    }

    private val containerPrivateJsonV1: String by lazy {
        val stream = String.javaClass.classLoader.getResourceAsStream("privateKey_container_v1.json")
        return@lazy stream.bufferedReader().use(BufferedReader::readText)
    }

    private val containerPrivateJsonV2: String by lazy {
        val stream = String.javaClass.classLoader.getResourceAsStream("privateKey_container_v2.json")
        return@lazy stream.bufferedReader().use(BufferedReader::readText)
    }

    @Before
    fun setup() {
        walletContainerMigrator = WalletContainerMigrator()
        instrumentationContext = InstrumentationRegistry.getInstrumentation().targetContext
        cipherManager = CipherManager(instrumentationContext)
        dataSource = WalletDataSourceImpl(cipherManager, "com.mywallet")
        repo = WalletRepositoryImpl(dataSource)
    }

    @After
    fun clean() {
        repo.deleteAll()
    }

    @Test
    fun testSuccessfulMnemonicMigrationFromV1() {
        val expected = WalletContainerParser.fromJson(containerMnemonicJsonV2.encodeToByteArray())

        val notMigratedWalletContainer =
            WalletContainerParser.fromJson(containerMnemonicJsonV1.encodeToByteArray())

        val actual = walletContainerMigrator.migrate(2, dataSource, notMigratedWalletContainer, passwordByteArray)


        Assert.assertEquals(expected.primaryAccount, actual.primaryAccount)
        Assert.assertEquals(expected.version, actual.version)
    }

    @Test
    fun testMnemonicContainerMigrated() {
        val expected = WalletContainerParser.fromJson(containerMnemonicJsonV2.encodeToByteArray())

        val actual = walletContainerMigrator.migrate(2, dataSource, expected, passwordByteArray)

        Assert.assertEquals(expected.primaryAccount, actual.primaryAccount)
        Assert.assertEquals(expected.version, actual.version)
    }

    @Test
    fun testSuccessfulPrivateKeyMigrationFromV1() {
        val expected = WalletContainerParser.fromJson(containerPrivateJsonV2.encodeToByteArray())

        val notMigratedWalletContainer =
            WalletContainerParser.fromJson(containerPrivateJsonV1.encodeToByteArray())

        val actual = walletContainerMigrator.migrate(
            newVersion = 2,
            walletDataSource = dataSource,
            walletContainer = notMigratedWalletContainer,
            password = passwordByteArray
        )


        Assert.assertEquals(expected.primaryAccount, actual.primaryAccount)
        Assert.assertEquals(expected.version, actual.version)
    }

    @Test
    fun testPrivateKeyContainerMigrated() {
        val expected = WalletContainerParser.fromJson(containerPrivateJsonV2.encodeToByteArray())

        val actual = walletContainerMigrator.migrate(
            newVersion = 2,
            walletDataSource = dataSource,
            walletContainer = expected,
            password = passwordByteArray
        )

        Assert.assertEquals(expected.primaryAccount, actual.primaryAccount)
        Assert.assertEquals(expected.version, actual.version)
    }

}