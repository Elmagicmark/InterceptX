package com.interceptx

import android.app.Application
import com.interceptx.data.db.AppDatabase
import com.interceptx.data.repository.InterceptXRepository
import com.interceptx.proxy.CertificateAuthority
import com.interceptx.proxy.InterceptQueue
import com.interceptx.proxy.ProxyEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Lightweight manual dependency container. Kept intentionally free of a DI
 * framework (Hilt/Koin) to keep the project buildable with a plain Gradle/AGP
 * toolchain and no extra annotation processors beyond Room/KSP.
 */
class InterceptXApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set
    lateinit var repository: InterceptXRepository
        private set
    lateinit var certificateAuthority: CertificateAuthority
        private set
    lateinit var interceptQueue: InterceptQueue
        private set
    lateinit var proxyEngine: ProxyEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = InterceptXRepository(database)
        certificateAuthority = CertificateAuthority(this).apply { init() }
        interceptQueue = InterceptQueue()
        proxyEngine = ProxyEngine(repository, certificateAuthority, interceptQueue, appScope)
    }
}
