package com.repisa.app

import android.app.Application
import android.content.Context
import com.repisa.app.i18n.L10n

class RepisaApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(L10n.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        L10n.init(this)
    }
}
