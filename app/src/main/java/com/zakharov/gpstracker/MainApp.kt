package com.zakharov.gpstracker

import android.app.Application
import com.zakharov.gpstracker.db.MainDb

class MainApp : Application() {
    val database by lazy {
        MainDb.getDataBAse(this)
    }
}