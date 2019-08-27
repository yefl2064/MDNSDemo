package com.test.wifidemo;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blankj.utilcode.util.ToastUtils;
import com.coolweather.coolweatherjetpack.util.ThreadExecutors;
import com.hprt.lib_base.base.BaseActivity;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.test.wifidemo.utils.PermissionPageUtils;
import com.test.wifidemo.utils.WifiAutoConnectManager;
import com.test.wifidemo.utils.YLog;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

/**
 * @author yefl
 * @date 2019/8/26.
 * description：
 */
public class ConActivity extends BaseActivity implements  WifiAutoConnectManager.ConnectListener  {

    Context mContext;
    WifiAutoConnectManager mWifiAutoConnectManager;
    private boolean enableScan = true;
    Dialog mConDialog;

    private String net = "WIFI:T:WPA2-PSK AES;P:12345678;S:DIRECT-nR-CP4000;";

    @NotNull
    @Override
    public Context getAct() {
        return this;
    }

    @Override
    public int getContentView() {
        return R.layout.activity_con;
    }

    @Override
    public void initView() {
        mContext = this;
        mWifiAutoConnectManager = new WifiAutoConnectManager(mContext.getApplicationContext(), this);
    }

    @Override
    public void initData() {
        mConDialog = new QMUITipDialog.Builder(mContext)
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                .setTipWord("正在连接...")
                .create();
        mConDialog.setCancelable(false);
        mConDialog.setCanceledOnTouchOutside(false);
        connect(net);
    }



    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void connect(String result) {
        if(enableScan) {
            vibrate();
            String passwordTemp = result.substring(result.indexOf("P:"));
            final String password = passwordTemp.substring(2, passwordTemp.indexOf(";")).replace("\"", "");
            String netWorkTypeTemp = result.substring(result.indexOf("T:"));
            final String netWorkType = netWorkTypeTemp.substring(2, netWorkTypeTemp.indexOf(";"));
            String netWorkNameTemp = result.substring(result.indexOf("S:"));
            final String netWorkName;
            if(netWorkNameTemp.contains(";")) {
                netWorkName = netWorkNameTemp.substring(2, netWorkNameTemp.indexOf(";"));
            }else{
                netWorkName = netWorkNameTemp.substring(2, netWorkNameTemp.length());
            }

            int net_type = 0x13;
            if (netWorkType.toLowerCase().contains("wpa")) {
                net_type = WifiAutoConnectManager.TYPE_WPA;// wpa
            } else if (netWorkType.toLowerCase().contains("wep")) {
                net_type = WifiAutoConnectManager.TYPE_WEP;// wep
            } else {
                net_type = WifiAutoConnectManager.TYPE_NO_PASSWD;// 无加密
            }
            enableScan = false;
            showProgressDialog();
            mWifiAutoConnectManager.connect(mContext,netWorkName, password, net_type);
        }
    }



    private void showProgressDialog(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mConDialog != null && !mConDialog.isShowing()){
                    mConDialog.show();
                }
            }
        });
    }
    private void dismissProgressDialog(){
        if(mConDialog != null && mConDialog.isShowing()){
            mConDialog.dismiss();
        }
    }

    private Dialog wifiInfoDialog(final String netWorkName, final String password, final String netWorkType){
        Dialog dialog = new MaterialDialog.Builder(mContext)
                .canceledOnTouchOutside(false)
                .cancelable(false)
                .content(netWorkName)
                .contentColor(ContextCompat.getColor(mContext, R.color.black))
                .backgroundColor(ContextCompat.getColor(mContext, R.color.white))
                .negativeText("取消")
                .positiveText("连接")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        int net_type = 0x13;
                        if (netWorkType.toLowerCase().contains("wpa")) {
                            net_type = WifiAutoConnectManager.TYPE_WPA;// wpa
                        } else if (netWorkType.toLowerCase().contains("wep")) {
                            net_type = WifiAutoConnectManager.TYPE_WEP;// wep
                        } else {
                            net_type = WifiAutoConnectManager.TYPE_NO_PASSWD;// 无加密
                        }
                        mWifiAutoConnectManager.connect(mContext,netWorkName, password, net_type);
                    }
                })
                .build();
        return dialog;
    }

    @Override
    public void accept(boolean connect) {
        dismissProgressDialog();
        if(connect){
            showMsg("连接WiFi成功");
        }else{
            showMsg("连接WiFi失败，请重试");
            enableScan = true;
        }
    }

    public boolean isWifi(){
        ConnectivityManager manager = (ConnectivityManager) getAct().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo.State wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if (wifi == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTING) {
            return true;
        }else {
            return false;
        }
    }


    private void showMsg(final String msg) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            new MaterialDialog.Builder(mContext)
                    .canceledOnTouchOutside(false)
                    .cancelable(true)
                    .content(msg+getConnectWifiSsid())
                    .backgroundColor(ContextCompat.getColor(mContext, R.color.white))
                    .positiveText("确定")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .build()
                    .show();
            }
        });
    }

    private String getConnectWifiSsid(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if(wifiInfo != null && wifiInfo.getSSID().length()>2) {
            return wifiInfo.getSSID().substring(1, wifiInfo.getSSID().length()-1);
        }else{
            return "";
        }

    }


}
