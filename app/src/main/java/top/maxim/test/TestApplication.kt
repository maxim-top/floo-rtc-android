package top.maxim.test

import androidx.multidex.MultiDexApplication

class TestApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        BaseManager.initBMXSDK(this)
    }
}