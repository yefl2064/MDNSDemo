package com.hprt.lib_base.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.multidex.MultiDex
import com.hprt.lib_base.util.AcitivityManager
import com.hprt.lib_base.util.MyUtil
import me.jessyan.autosize.AutoSize

open class BaseApp: Application(){

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(lifecycleCallbacks)
        AutoSize.initCompatMultiProcess(this)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base)
    }


    /**
     * 以下为防止短时间内两次点击button
     */
    private val lifecycleCallbacks = object : ActivityLifecycleCallbacks{
        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
            MyUtil.fixViewMutiClickInShortTime(activity)
            AcitivityManager.instance.addActivity(activity)
        }

        override fun onActivityStarted(activity: Activity) {

        }

        override fun onActivityResumed(activity: Activity) {

        }

        override fun onActivityPaused(activity: Activity) {

        }

        override fun onActivityStopped(activity: Activity) {

        }

        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle?) {

        }

        override fun onActivityDestroyed(activity: Activity) {
            AcitivityManager.instance.removeActivity(activity)
        }
    }

}