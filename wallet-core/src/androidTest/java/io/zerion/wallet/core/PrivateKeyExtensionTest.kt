package io.zerion.wallet.core

import io.zerion.wallet.core.extensions.isValidPrivateKey
import org.junit.*

/**
 * Created by rolea on 23.05.2022.
 */
class PrivateKeyExtensionTest {
    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Test
    fun testIsValidPrivateKeyFail() {
        val result = false
        val action = "genre allow company blind security cluster cost stock skate wait debris".isValidPrivateKey()
        Assert.assertEquals(action, result)
    }

    @Test
    fun testIsValidPrivateKeySucceed() {
        val result = true
        val action = "15b30fbf6d02f91412755a27ad1402f75a0068dfae968420095c6b632d54f816".isValidPrivateKey()
        Assert.assertEquals(action, result)
    }
}