package com.hprt.lib_base.util

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView

class MyUtil{

    companion object{
        //防止短时间内多次点击，弹出多个activity 或者 dialog ，默认500ms
        public fun fixViewMutiClickInShortTime(activity: Activity) {
            activity.window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
                proxyOnlick(
                    activity.window.decorView,
                    5
                )
            }
        }

        private fun proxyOnlick(view: View, recycledContainerDeep: Int) {
            var recycledContainerDeep = recycledContainerDeep
            if (view.visibility == View.VISIBLE) {
                val forceHook = recycledContainerDeep == 1
                if (view is ViewGroup) {
                    val existAncestorRecycle = recycledContainerDeep > 0
                    if (!(view is AbsListView || view is ListView) || existAncestorRecycle) {
                        getClickListenerForView(view)
                        if (existAncestorRecycle) {
                            recycledContainerDeep++
                        }
                    } else {
                        recycledContainerDeep = 1
                    }
                    val childCount = view.childCount
                    for (i in 0 until childCount) {
                        val child = view.getChildAt(i)
                        proxyOnlick(child, recycledContainerDeep)
                    }
                } else {
                    getClickListenerForView(view)
                }
            }
        }

        /**
         * 通过反射  查找到view 的clicklistener
         * @param view
         */
        fun getClickListenerForView(view: View) {
            try {
                val viewClazz = Class.forName("android.view.View")
                //事件监听器都是这个实例保存的
                val listenerInfoMethod = viewClazz.getDeclaredMethod("getListenerInfo")
                if (!listenerInfoMethod.isAccessible) {
                    listenerInfoMethod.isAccessible = true
                }
                val listenerInfoObj = listenerInfoMethod.invoke(view)

                val listenerInfoClazz = Class.forName("android.view.View\$ListenerInfo")

                val onClickListenerField = listenerInfoClazz.getDeclaredField("mOnClickListener")

                if (onClickListenerField != null) {
                    if (!onClickListenerField.isAccessible) {
                        onClickListenerField.isAccessible = true
                    }
                    val mOnClickListener = onClickListenerField.get(listenerInfoObj) as View.OnClickListener?
                    if (mOnClickListener !is ProxyOnclickListener) {
                        //自定义代理事件监听器
                        val onClickListenerProxy = ProxyOnclickListener(mOnClickListener)
                        //更换
                        onClickListenerField.set(listenerInfoObj, onClickListenerProxy)
                    } else {
//                        Log.e("OnClickListenerProxy", "setted proxy listener ")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        //自定义的代理事件监听器
        internal class ProxyOnclickListener(private val onclick: View.OnClickListener?) : View.OnClickListener {

            private val MIN_CLICK_DELAY_TIME = 500

            private var lastClickTime: Long = 0

            override fun onClick(v: View) {
                //点击时间控制
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                    lastClickTime = currentTime
//                    Log.e("OnClickListenerProxy", "OnClickListenerProxy$this")
                    onclick?.onClick(v)
                }
            }
        }

    }
}