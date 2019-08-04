package com.yefl.mdnsdemo

import android.net.nsd.NsdServiceInfo
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder

class NsdServiceInfoAdapter(layoutResId:Int, data:List<NsdServiceInfo>):BaseQuickAdapter<NsdServiceInfo, BaseViewHolder>(layoutResId, data){
    override fun convert(helper: BaseViewHolder, item: NsdServiceInfo) {
        helper.setText(R.id.tv_name, item.serviceName)
        if(item.host != null) {
            helper.setText(R.id.tv_host, item.host.hostAddress)
        }
    }

}