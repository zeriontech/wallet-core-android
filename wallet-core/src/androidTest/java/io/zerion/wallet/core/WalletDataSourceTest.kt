package io.zerion.wallet.core

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.zerion.wallet.core.implementations.CipherManager
import io.zerion.wallet.core.implementations.WalletDataSourceImpl
import io.zerion.wallet.core.implementations.WalletRepositoryImpl
import org.junit.*
import java.util.UUID

/**
 * Created by rolea on 15.01.2022.
 */

class WalletDataSourceTest {
    init {
        System.loadLibrary("TrustWalletCore")
    }

    private lateinit var repo: WalletRepositoryImpl
    private lateinit var dataSource: WalletDataSourceImpl
    private lateinit var cipherManager: CipherManager
    private lateinit var password: String
    lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().targetContext
        password = UUID.randomUUID().toString()
        cipherManager = CipherManager(instrumentationContext)
        dataSource = WalletDataSourceImpl(cipherManager, "com.mywallet")
        repo = WalletRepositoryImpl(dataSource)
        repo.deleteAll()
    }

    @Test
    fun testSave() {
        val wallet1 =  repo.createWalletPersist(password, null)
        val wallet2 =  repo.createWalletPersist(password, null)
        Assert.assertEquals(2, repo.count(), )
        Assert.assertNotNull(repo.load(wallet1.id))
        Assert.assertNotNull(repo.load(wallet2.id))
    }

    @Test
    fun testLoadAll() {
        val wallet1 =  repo.createWalletPersist(password, null)
        val wallet2 =  repo.createWalletPersist(password, null)
        Assert.assertEquals(2, repo.loadAll().size)
    }

    @Test
    fun testDeleteAll() {
        val wallet1 =  repo.createWalletPersist(password, null)
        val wallet2 =  repo.createWalletPersist(password, null)
        Assert.assertEquals(2, repo.count())
        repo.deleteAll()
        Assert.assertEquals(0, repo.count())
    }

    @Test
    fun testDeleteById() {
        val wallet1 =  repo.createWalletPersist(password, null)
        Assert.assertNotNull(repo.load(wallet1.id))
        repo.delete(wallet1.id)
        Assert.assertEquals(1, repo.count())
    }

    @Test
    fun testResavingWalletNotDoubled() {
        val wallet1 =  repo.createWalletPersist(password, null)
        Assert.assertEquals(1, repo.count(),)
        repo.save(wallet1)
        Assert.assertEquals(1, repo.count(), )
    }
}