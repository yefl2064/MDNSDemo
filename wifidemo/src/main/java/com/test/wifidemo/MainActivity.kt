package com.test.wifidemo

import android.content.Context
import android.content.Intent
import com.hprt.lib_base.base.BaseActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {
    override fun getAct(): Context {
        return this
    }

    override fun getContentView(): Int {
        return R.layout.activity_main
    }

    override fun initView() {

        btn_scan.setOnClickListener {
            startActivity(Intent(getAct(), ScanActivity::class.java))
        }
    }

    override fun initData() {

    }


}
