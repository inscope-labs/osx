package com.inscopelabs.osx

import android.app.Application
import com.inscopelabs.osx.di.AppModule

class OsxApplication : Application() {
    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()
        appModule = AppModule(this)
    }
}
