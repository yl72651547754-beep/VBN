package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.VpnRepository
import com.example.worker.RefreshServersWorker

class FreeVpnApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { VpnRepository(database.vpnServerDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // جدولة التحديث الدوري للخوادم عبر WorkManager
        RefreshServersWorker.schedule(this, enabled = true)
    }

    companion object {
        lateinit var instance: FreeVpnApplication
            private set
    }
}
