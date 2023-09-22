package io.zerion.wallet.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.protobuf.ByteString
import io.zerion.wallet.core.extensions.Numeric
import io.zerion.wallet.core.extensions.toHex
import io.zerion.wallet.core.extensions.toHexBytes
import io.zerion.wallet.core.extensions.toHexBytesInByteString
import io.zerion.wallet.core.implementations.Signer
import io.zerion.wallet.core.models.GasPrice.Classic
import io.zerion.wallet.core.models.SignInput.PersonalSign
import io.zerion.wallet.core.models.SignInput.Sign
import io.zerion.wallet.core.models.SignInput.Transaction
import io.zerion.wallet.core.models.SignInput.TypedData
import io.zerion.wallet.core.models.TransactionInput
import org.junit.*
import org.junit.runner.*
import java.io.BufferedReader

/**
 * Created by rolea on 14.12.2022.
 */

@RunWith(AndroidJUnit4::class)
class SignerTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    val privateKey = "15b30fbf6d02f91412755a27ad1402f75a0068dfae968420095c6b632d54f816".toHexBytes()

    val typedData: String by lazy {
        val stream = String.javaClass.classLoader.getResourceAsStream("typedData.json")
        return@lazy stream.bufferedReader().use(BufferedReader::readText)
    }

    @Test
    fun testLegacySign() {
        val message = Numeric.hexStringToByteArray("85cab08f60de613ede14d37927fca4ebeb046b3d040df12dadbd13e59af2db16")
        val signed = "69267087540a8370a23ec6e14f1c2c4d63c8d4f6062ba9ca531b93be2978" +
                "f0d824e26b6cc73ea0f8eea65fb55b351528cd7ba366f422765f7fdb7ba3f6ee27ae1b"
        val result = Signer.sign(Sign(message), privateKey).toHex()
        Assert.assertEquals(signed, result)
    }

    @Test
    fun testPersonalSign() {
        val message = "My email is john@doe.com - Thu, 21 Apr 2022 12:57:50 GMT".encodeToByteArray()
        val signed = "16afa1b697bb2b05ff3bc748449b52e40afe819b8f2db3c8620ae5637544" +
                "b76e7727b86ea3617dde0038b206bc5e22ed895846c8f0679aaf1bbb22f1c0646dd41c"
        val result = Signer.sign(PersonalSign(message), privateKey).toHex()
        Assert.assertEquals(signed, result)
    }

    @Test
    fun testVIs28Or27InPersonalSign() {
        val message = "My email is john@doe.com - Thu, 21 Apr 2022 12:57:50 GMT".encodeToByteArray()
        val result = Signer.sign(PersonalSign(message), privateKey)
        val v = result[64].toInt()
        Assert.assertTrue(v in (27 .. 28))
    }

    @Test
    fun testVIs28Or27InSignTypedData() {
        val message = typedData.encodeToByteArray()
        val result = Signer.sign(TypedData(message), privateKey)
        val v = result[64].toInt()
        Assert.assertTrue(v in (27 .. 28))
    }

    @Test
    fun testVIsNot0Or1InPersonalSign() {
        val message = "My email is john@doe.com - Thu, 21 Apr 2022 12:57:50 GMT".encodeToByteArray()
        val result = Signer.sign(PersonalSign(message), privateKey)
        val v = result[64].toInt()
        Assert.assertFalse(v == 0)
        Assert.assertFalse(v == 1)
    }

    @Test
    fun testVIsNot0r1InSignTypedData() {
        val message = typedData.encodeToByteArray()
        val result = Signer.sign(TypedData(message), privateKey)
        val v = result[64].toInt()
        Assert.assertFalse(v == 0)
        Assert.assertFalse(v == 1)
    }

    @Test
    fun testSignTransaction() {
        val transaction = TransactionInput(
            chainID = "01".toHexBytesInByteString(),
            gasPrice = Classic("0ab5d04c00".toHexBytesInByteString()),
            nonce = "00".toHexBytesInByteString(),
            gas = "5208".toHexBytesInByteString(),
            toAddress = "0x7467594dd44629415864af5bcbf861b0c886afad",
            data = ByteString.EMPTY,
            amount = "00".toHexBytesInByteString()
        )

        val signed = "f86480850ab5d04c00825208947467594dd44629415864af5bcbf861b0c886afad808026a08a" +
                "79f5d3d7bec3670cffdf8f36adbded9f566fdcd41e7628741e6aecca2c761ea0" +
                "40474ba7f53392511de1bfcea364b14956a4b0d8285f08aef6bee284abb24228"

        val result = Signer.sign(Transaction(transaction), privateKey).toHex()
        Assert.assertEquals(signed, result)
    }

    @Test
    fun testSignTypedData() {
        val message = typedData.encodeToByteArray()
        val signed =
            "fd3ce489dcbf26f8b77c40b2ff04e08f7145fac44b22c141146818a2af50938a4e9099df298c4a2b74c34100b625decaaf1bc043d7222df44d344b74550fb1de1b"
        val result = Signer.sign(TypedData(message), privateKey).toHex()
        Assert.assertEquals(signed, result)
    }
}