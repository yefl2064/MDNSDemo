package com.yefl.mdnsdemo

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.coolweather.coolweatherjetpack.util.ThreadExecutors
import com.hprt.lib_base.base.BaseActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity: BaseActivity() {


    lateinit var nsdmanager: NsdManager

    override fun getAct(): Context = getAct()

    override fun getContentView(): Int = R.layout.activity_main

    var nsdinfoList = ArrayList<NsdServiceInfo>()

    var nsdAdapter:NsdServiceInfoAdapter?=null


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
            doDiscovery()
        }

        btn_stop.setOnClickListener {
            nsdmanager.stopServiceDiscovery(discoveryListener)
        }
    }

    override fun initData() {
        val nsdServiceInfo = NsdServiceInfo()
        nsdServiceInfo.serviceName = "1234"
        nsdServiceInfo.serviceType = "_ipp._tcp."
        nsdServiceInfo.port = 631
        nsdmanager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, nsRegListener)

        doDiscovery()
    }

    private fun doDiscovery(){
        nsdinfoList.clear()
        nsdmanager.discoverServices("_ipp._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
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
            for(info in nsdinfoList){
                if(info.serviceName.equals(serviceInfo?.serviceName)){
                    return
                }
            }
            nsdinfoList.add(serviceInfo!!)
            ThreadExecutors.mainThread.execute{
                nsdAdapter?.notifyDataSetChanged()
            }
            LogUtils.d(serviceInfo)
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
        }

        override fun onDiscoveryStarted(serviceType: String?) {
        }

        override fun onDiscoveryStopped(serviceType: String?) {
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
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