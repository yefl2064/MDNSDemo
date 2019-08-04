package com.hprt.lib_base.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import java.util.*

class AcitivityManager private constructor()  {

    public val activityStack: Stack<Activity> = Stack()

    companion object {
        val instance: AcitivityManager by lazy { AcitivityManager() }
    }

    /**
     * 添加Activity
     */
    fun addActivity(activity: Activity) {
        activityStack.add(activity)
    }

    /**
     * 移出存在的Activity
     */
    fun removeActivity(activity: Activity) {
        if (activityStack.contains(activity)) {
            activity.finish()
            activityStack.remove(activity)
        }
    }

    /**
     * 获取最上面的Activity
     */
    fun getTopActivity(): Activity {
        return activityStack.lastElement()
    }

    /**
     * 清除Activity栈
     */
    fun clearActivity() {
        for (activity in activityStack) {
            activity.finish()
        }
        activityStack.clear()
    }

    /*
        退出应用程序
     */
    fun exitApp(context: Context) {
        //先清除Activity
        clearActivity()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        //再killProcesses
        activityManager.killBackgroundProcesses(context.packageName)
        System.exit(0)
    }
}