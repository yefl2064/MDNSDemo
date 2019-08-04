package com.hprt.lib_base.base

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_base.data.Resource
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog


abstract class BaseActivity: AppCompatActivity(){
    private var progressdialog: QMUITipDialog?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getContentView())
        initView()
        initData()
    }

    abstract fun getAct(): Context

    abstract fun getContentView():Int

    abstract fun initView()

    abstract fun initData()

    fun showProgress(){
        if (progressdialog == null) {
            progressdialog = QMUITipDialog.Builder(getAct())
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                .setTipWord("正在加载")
                .create()
            progressdialog?.setCancelable(true)
            progressdialog?.setCanceledOnTouchOutside(true)
        }
        progressdialog?.show()
    }

    fun dismissProgress(){
        if(progressdialog?.isShowing?:false) {
            progressdialog?.dismiss()
        }
    }


    fun <T> handleData(liveData: LiveData<Resource<T>>, action: (T) -> Unit) = liveData.observe(this, Observer { result ->
        if (result?.code == Resource.LOADING) {
            showProgress()
        } else if (result?.data != null && result.code == Resource.SUCCESS) {
            dismissProgress()
            action(result.data)
        } else {
            if(!result?.msg.isNullOrEmpty()) {
                ToastUtils.showShort(result.msg)
            }
            dismissProgress()
        }
    })

    override fun onDestroy() {
        progressdialog?.dismiss()
        progressdialog = null
        super.onDestroy()
    }

}