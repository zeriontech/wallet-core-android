package io.zerion.wallet.core.implementations

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.zerion.wallet.core.exceptions.CipherException.FailedLoadKeyStore
import io.zerion.wallet.core.utils.FileUtil
import io.zerion.wallet.core.utils.FileUtil.getFilePath
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

/**
 * Created by rolea on 21.11.2021.
 */
class CipherManager(private val context: Context) {

    private val ANDROID_KEY_STORE = "AndroidKeyStore"
    private val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private val CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding"

    fun put(id: String, data: ByteArray) {
        setData(data, id, id, id + "iv")
    }

    fun get(id: String): ByteArray {
        return getData(id, id, id + "iv")
    }

    fun remove(id: String) {
        removeAliasAndFiles(context,id, id, id+ "iv")
    }

    fun getAllAliases(prefix: String): List<String> {
        val keystoreAlias = loadKeyStore()
        return keystoreAlias.aliases().toList().filter { it.startsWith(prefix) }
    }

    fun countEntry(prefix: String): Int {
        return getAllAliases(prefix).size
    }

    /**
     * Load AndroidKeyStore.
     * @return true if keystore loaded successfully
     */
    @Throws(FailedLoadKeyStore::class)
    private fun loadKeyStore(): KeyStore {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore
        } catch (e: Exception) {
            throw FailedLoadKeyStore("Can not load keystore:" + e.message)
        }
    }

    private fun generateKey(keystoreAlias: String, isAuthenticationRequired: Boolean): Boolean {
        return try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            keyGenerator.init(KeyGenParameterSpec.Builder(
                keystoreAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setKeySize(256)
                .setUserAuthenticationRequired(isAuthenticationRequired)
                .setRandomizedEncryptionRequired(true)
                .setEncryptionPaddings(PADDING)
                .build())
            keyGenerator.generateKey()

            true
        } catch (exc: NoSuchAlgorithmException) {
            exc.printStackTrace()
            false
        } catch (exc: NoSuchProviderException) {
            exc.printStackTrace()
            false
        } catch (exc: InvalidAlgorithmParameterException) {
            exc.printStackTrace()
            false
        }
    }

    private fun generateKeyIfNecessary(
        keyStore: KeyStore,
        alias: String,
        isAuthenticationRequired: Boolean,
    ): Boolean {
        try {
            return keyStore.containsAlias(alias) || generateKey(alias, isAuthenticationRequired)
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        }
        return false
    }

    fun isExists(alias: String): Boolean {
        try {
            val keyStore = loadKeyStore()

            return keyStore.containsAlias(alias)
        } catch (e: Exception) {
            throw Exception(e)
        }
    }

    private fun setData(data: ByteArray, alias: String, aliasFile: String, aliasIV: String): Boolean {
        val keyStore: KeyStore

        try {
            keyStore = loadKeyStore()
            generateKeyIfNecessary(keyStore, alias, false)

            val encryptedDataFilePath = getFilePath(context, aliasFile)
            val secretKey = keyStore.getKey(alias, null) ?:
            throw Exception("Secret is null on setData: $alias")

            val inCipher = Cipher.getInstance(CIPHER_ALGORITHM)
            inCipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = inCipher.iv
            val path = getFilePath(context, aliasIV)

            if (!FileUtil.writeBytesToFile(path, iv)) {
                keyStore.deleteEntry(alias)
                throw Exception("Failed to save the iv file for: $alias")
            }

            var cipherOutputStream: CipherOutputStream? = null

            try {
                cipherOutputStream = CipherOutputStream(FileOutputStream(encryptedDataFilePath), inCipher)
                cipherOutputStream.write(data)
            } catch (e: Exception) {
                throw Exception("Failed to save the file for: $alias", e)
            } finally {
                cipherOutputStream?.close()
            }

            return true
        } catch (e: Exception) {
            throw Exception("KeyStore error!", e)
        }
    }

    private fun getData(alias: String, aliasFile: String, aliasIV: String): ByteArray {

        val keyStore: KeyStore
        val encryptedDataFilePath = getFilePath(context, aliasFile)

        try {
            keyStore = loadKeyStore()

            val secretKey = keyStore.getKey(alias, null) ?:
            throw Exception("SecretKey is gone!")

            val ivExists = File(getFilePath(context, aliasIV)).exists()
            val aliasExists = File(getFilePath(context, aliasFile)).exists()

            if (!ivExists || !aliasExists) {
                removeAliasAndFiles(context, alias, aliasFile, aliasIV)

                if (ivExists != aliasExists)
                    throw Exception("File is present but the key is gone: $alias")
                else
                    throw Exception("File and key is gone: $alias")
            }

            val iv = FileUtil.readBytesFromFile(getFilePath(context, aliasIV)) ?: throw NullPointerException("iv is missing for $alias")

            val outCipher = Cipher.getInstance(CIPHER_ALGORITHM)
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val cipherInputStream = CipherInputStream(FileInputStream(encryptedDataFilePath), outCipher)
            return FileUtil.readBytesFromStream(cipherInputStream)
        } catch (e: Exception) {
            throw Exception("KeyStore error!", e)
        }
    }

    private fun removeAliasAndFiles(context: Context, alias: String, dataFileName: String, ivFileName: String) {
        val keyStore: KeyStore
        try {
            keyStore = loadKeyStore()
            keyStore.deleteEntry(alias)
            File(getFilePath(context, dataFileName)).delete()
            File(getFilePath(context, ivFileName)).delete()
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }
}