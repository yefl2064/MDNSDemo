package com.yefl.mdnsdemo

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.util.Log
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.coolweather.coolweatherjetpack.util.ThreadExecutors
import com.hprt.lib_base.base.BaseActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity: BaseActivity() {


    lateinit var nsdmanager: NsdManager

    override fun getAct(): Context = getAct()

    override fun getContentView(): Int = R.layout.activity_main

    var nsdinfoList = ArrayList<NsdServiceInfo>()

    var nsdAdapter:NsdServiceInfoAdapter?=null

    var isScaning = false;


    override fun initView() {
        nsdmanager =  applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdAdapter = NsdServiceInfoAdapter(R.layout.item_nsdserviceinfo, nsdinfoList)

        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = nsdAdapter

        nsdAdapter?.setOnItemClickListener { adapter, view, position ->
            nsdmanager.resolveService(nsdinfoList.get(position), resolveListener)
        }

        btn_scan.setOnClickListener {
            stop()
            if(!isScaning) {
                doDiscovery()
            }else{
                ToastUtils.showShort("上次搜索未释放,稍后再试")
            }
        }

        btn_stop.setOnClickListener {
            stop()
        }
    }

    override fun initData() {


    }

    override fun onResume() {
        super.onResume()
        if(!isScaning)
            doDiscovery()
    }

    override fun onPause() {
        super.onPause()
        stop();
    }

    override fun onDestroy() {
        stop()
        isScaning = false
        super.onDestroy()
    }

    private fun doDiscovery(){
        nsdinfoList.clear()
        nsdAdapter?.notifyDataSetChanged()
        try {
            nsdmanager.discoverServices("_ipp._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }catch (e:java.lang.Exception){
            e.printStackTrace()
        }


        val nsdServiceInfo = NsdServiceInfo()
        nsdServiceInfo.serviceName = "1234"
        nsdServiceInfo.serviceType = "_ipp._tcp."
        nsdServiceInfo.port = 631
        try {
            nsdmanager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, nsRegListener)
        }catch (e:java.lang.Exception){
            e.printStackTrace()
        }
    }


    fun stop(){
        try {
            if (isScaning) {
                nsdmanager.stopServiceDiscovery(discoveryListener)
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
        try {
            nsdmanager.unregisterService(nsRegListener)
        }catch (e:java.lang.Exception){
            e.printStackTrace()
        }
    }

    val nsRegListener: RegistrationListener = object:RegistrationListener{
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {

        }
    }


    val discoveryListener: NsdManager.DiscoveryListener = object: NsdManager.DiscoveryListener {
        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            LogUtils.d("onServiceFound--->"+serviceInfo)
            for(info in nsdinfoList){
                if(info.serviceName.equals(serviceInfo?.serviceName)){
                    return
                }
            }
            nsdinfoList.add(serviceInfo!!)
            ThreadExecutors.mainThread.execute{
                nsdAdapter?.notifyDataSetChanged()
            }
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            LogUtils.d("onStopDiscoveryFailed, errCode =="+errorCode)
            LogUtils.file("onStopDiscoveryFailed, errCode =="+errorCode)
            ThreadExecutors.mainThread.execute { tv_status.setText("onStopDiscoveryFailed...errCode =="+errorCode) }
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            LogUtils.d("onStartDiscoveryFailed, errCode =="+errorCode)
            LogUtils.file("onStartDiscoveryFailed, errCode =="+errorCode)
            ThreadExecutors.mainThread.execute { tv_status.setText("onStartDiscoveryFailed...errCode =="+errorCode) }
        }

        override fun onDiscoveryStarted(serviceType: String?) {
            LogUtils.d("onDiscoveryStarted")
            LogUtils.file("onDiscoveryStarted")
            isScaning = true
            ThreadExecutors.mainThread.execute { tv_status.setText("开始搜索...") }
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            LogUtils.d("onDiscoveryStopped")
            LogUtils.file("onDiscoveryStopped")
            isScaning = false;
            ThreadExecutors.mainThread.execute { tv_status.setText("停止搜索...") }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            LogUtils.d("onServiceLost--->"+serviceInfo)
            LogUtils.file("onServiceLost--->"+serviceInfo)
            for(info in nsdinfoList){
                if(info.serviceName.equals(serviceInfo?.serviceName)){
                    nsdinfoList.remove(serviceInfo!!)
                    ThreadExecutors.mainThread.execute{
                        nsdAdapter?.notifyDataSetChanged()
                    }
                }
            }

        }

    }


    val resolveListener: NsdManager.ResolveListener = object:NsdManager.ResolveListener{
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            LogUtils.d("onResolveFailed--->"+serviceInfo +" errCode-->"+errorCode)
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
            LogUtils.d("onServiceResolved--->"+serviceInfo)
            for(info in nsdinfoList){
                if(info.serviceName.equals(serviceInfo?.serviceName)){
                    info.host = serviceInfo?.host
                    ThreadExecutors.mainThread.execute {
                        nsdAdapter?.notifyDataSetChanged()
                    }
                }
            }
        }

    }

}