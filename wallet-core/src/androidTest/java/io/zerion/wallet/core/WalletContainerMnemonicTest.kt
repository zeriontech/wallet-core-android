package io.zerion.wallet.core

import io.zerion.wallet.core.extensions.toHex
import io.zerion.wallet.core.models.WalletContainer
import io.zerion.wallet.core.utils.WalletContainerParser
import org.junit.*
import java.io.BufferedReader

/**
 * Created by rolea on 5.12.2021.
 */

class WalletContainerMnemonicTest {

    private lateinit var container: WalletContainer
    private lateinit var data: ByteArray

    val mnemonic = "genre allow company blind security cluster cost stock skate wait debris subway"
    val password = "12345678"
    val passwordByteArray = password.toByteArray()

    val containerJson: String by lazy {
        val stream = String.javaClass.classLoader.getResourceAsStream("mnemonic_container_v2.json")
        return@lazy stream.bufferedReader().use(BufferedReader::readText)
    }

    @Before
    fun setup() {
        data = containerJson.encodeToByteArray()
        container = WalletContainerParser.fromJson(data)
    }

    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Test
    fun testImport() {
        WalletContainerParser.fromJson(data)
    }



    @Test
    fun testType() {
        Assert.assertEquals(WalletContainer.Type.Mnemonic, container.type)
    }

    @Test
    fun testPrimaryAccount() {
        Assert.assertEquals(
            "0xED4a971eA7948B79265C3CA0b9F79D9b56c0022d",
            container.primaryAccount
        )
    }

    @Test
    fun testMnemonicDecrypt() {
        val data = containerJson.encodeToByteArray()
        val container = WalletContainerParser.fromJson(data)
        Assert.assertEquals(container.decryptMnemonic(passwordByteArray), mnemonic)
    }



    @Test
    fun testAccounts() {

        Assert.assertEquals(container.accounts.size, 3)
        container.removeAccount(container.derivationPath(0))
        Assert.assertEquals(container.accounts.size, 2)
        container.addAccount(container.derivationPath(3), passwordByteArray)
        container.addAccount(passwordByteArray)
        Assert.assertEquals(container.accounts.size, 4)
    }

    @Test
    fun testAddressesFromIndex() {
        Assert.assertEquals(container.deriveAccount(0, passwordByteArray).address,
            "0xED4a971eA7948B79265C3CA0b9F79D9b56c0022d"
        )

        Assert.assertEquals(
            container.deriveAccount(1, passwordByteArray).address,
            "0x7467594Dd44629415864Af5BcBf861b0C886afAD"
        )

        Assert.assertEquals(
            container.deriveAccount(2, passwordByteArray).address,
            "0x04b9aB3Be467cbB98f275B266952977116FF59b7"
        )
    }

    @Test
    fun testAddressesFromPaths() {
        Assert.assertEquals(
            container.deriveAccount("m/44'/60'/0'/0/0", passwordByteArray).address,
            "0xED4a971eA7948B79265C3CA0b9F79D9b56c0022d"
        )

        Assert.assertEquals(
            container.deriveAccount("m/44'/60'/0'/0/1", passwordByteArray).address,
            "0x7467594Dd44629415864Af5BcBf861b0C886afAD"
        )

        Assert.assertEquals(
            container.deriveAccount("m/44'/60'/0'/0/2", passwordByteArray).address,
            "0x04b9aB3Be467cbB98f275B266952977116FF59b7"
        )
    }

    @Test
    fun testAddressesDeriveBatch() {
        val accounts = container.deriveAccounts(0, 99, password)

        Assert.assertEquals(accounts.size, 100)

        Assert.assertEquals(
            accounts[0].address,
            "0xED4a971eA7948B79265C3CA0b9F79D9b56c0022d"
        )

        Assert.assertEquals(
            accounts[1].address,
            "0x7467594Dd44629415864Af5BcBf861b0C886afAD"
        )

        Assert.assertEquals(
            accounts[2].address,
            "0x04b9aB3Be467cbB98f275B266952977116FF59b7"
        )
    }

    @Test
    fun testPrivateKeysFromIndex() {
        Assert.assertEquals(
            "dbe95804848004ef312ee1877eb5af4eaf4692a8e04ff97649edbc3c71f4f656",
            container.decryptPrivateKey(0, password).toHex(),
        )

        Assert.assertEquals(
            "15b30fbf6d02f91412755a27ad1402f75a0068dfae968420095c6b632d54f816",
            container.decryptPrivateKey(1, password).toHex()
        )

        Assert.assertEquals(
            "5bceb69fcc15f63cc30f44f403f9899638aa2a0758ae55719e5690e26f0ccb3b",
            container.decryptPrivateKey(2, password).toHex()
        )
    }

    @Test
    fun testPrivateKeysFromPaths() {
        Assert.assertEquals(
            "dbe95804848004ef312ee1877eb5af4eaf4692a8e04ff97649edbc3c71f4f656",
            container.decryptPrivateKey("m/44'/60'/0'/0/0", password).toHex()
        )

        Assert.assertEquals(
            "15b30fbf6d02f91412755a27ad1402f75a0068dfae968420095c6b632d54f816",
            container.decryptPrivateKey("m/44'/60'/0'/0/1", password).toHex(),
        )

        Assert.assertEquals(
            "5bceb69fcc15f63cc30f44f403f9899638aa2a0758ae55719e5690e26f0ccb3b",
            container.decryptPrivateKey("m/44'/60'/0'/0/2", password).toHex()
        )
    }

    @Test
    fun testDerivation() {

        Assert.assertEquals(
            container.derivationPath(0),
            "m/44'/60'/0'/0/0"
        )

        Assert.assertEquals(
            container.derivationPath(1),
            "m/44'/60'/0'/0/1"
        )

        Assert.assertEquals(
            container.derivationPath(2),
            "m/44'/60'/0'/0/2"
        )
    }

    @Test
    fun testExport() {
        container.addAccount(passwordByteArray)
        val exportedData = container.export()
        val reimported = WalletContainerParser.fromJson(exportedData)
        Assert.assertEquals(reimported.decryptMnemonic(passwordByteArray), mnemonic)
        Assert.assertEquals(container.accounts.size, 4)
        Assert.assertEquals("0xED4a971eA7948B79265C3CA0b9F79D9b56c0022d", container.primaryAccount)
        Assert.assertEquals(2, container.version)
    }

    @Test
    fun testPasswordChange() {
        Assert.assertEquals(
            "dbe95804848004ef312ee1877eb5af4eaf4692a8e04ff97649edbc3c71f4f656",
            container.decryptPrivateKey(0, password).toHex(),
        )

        val newPassword = "abcdefg"
        val newPasswordEncoded = newPassword.toByteArray()

        container.changePassword(password.toByteArray(), newPasswordEncoded)

        Assert.assertEquals(
            "dbe95804848004ef312ee1877eb5af4eaf4692a8e04ff97649edbc3c71f4f656",
            container.decryptPrivateKey(0, newPassword).toHex()
        )
    }
}