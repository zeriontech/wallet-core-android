package io.zerion.wallet.core

import io.zerion.wallet.core.extensions.toHex
import io.zerion.wallet.core.models.WalletContainer
import io.zerion.wallet.core.utils.WalletContainerParser
import org.junit.*
import java.io.BufferedReader

/**
 * Created by rolea on 6.12.2021.
 */
class WalletContainerPrivateTest {

    private lateinit var container: WalletContainer
    private lateinit var data: ByteArray

    val privateKey = "15b30fbf6d02f91412755a27ad1402f75a0068dfae968420095c6b632d54f816"
    val password = "12345678"

    val containerJson: String by lazy {
        val stream = String.javaClass.classLoader.getResourceAsStream("privateKey.json")
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
        Assert.assertEquals(container.type, WalletContainer.Type.PrivateKey)
    }

    @Test
    fun testPrivateKeyDecrypt() {
        Assert.assertEquals(container.decryptPrimaryPrivateKey(password).toHex(true), privateKey)
    }

    @Test
    fun testAddress() {
        Assert.assertEquals(
            container.derivePrimaryAccount(password).address,
            "0x7467594Dd44629415864Af5BcBf861b0C886afAD"
        )
    }

    @Test
    fun testExport() {
        val exportedData = container.export()
        val reimported = WalletContainerParser.fromJson(exportedData)
        Assert.assertEquals(reimported.decryptPrimaryPrivateKey(password).toHex(true), privateKey)
    }

    @Test
    fun testPasswordChange() {
        Assert.assertEquals(container.decryptPrimaryPrivateKey(password).toHex(), privateKey)
            val newPassword = "abcdefg"
        container.changePassword(password, newPassword)
        Assert.assertEquals(container.decryptPrimaryPrivateKey(newPassword).toHex(), privateKey)
    }
}