package com.test.wifidemo;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.coolweather.coolweatherjetpack.util.ThreadExecutors;
import com.hprt.lib_base.base.BaseActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.test.wifidemo.utils.PermissionPageUtils;
import com.test.wifidemo.utils.WifiAutoConnectManager;
import com.test.wifidemo.utils.YLog;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

/**
 * @author yefl
 * @date 2019/8/26.
 * description：
 */
public class ScanActivity extends BaseActivity implements QRCodeView.Delegate, WifiAutoConnectManager.ConnectListener  {

    ZXingView mZXingView;
    Context mContext;
    WifiAutoConnectManager mWifiAutoConnectManager;
    private boolean enableScan = true;
    Dialog mConDialog;
    @NotNull
    @Override
    public Context getAct() {
        return this;
    }

    @Override
    public int getContentView() {
        return R.layout.activity_scan_qrcode;
    }

    @Override
    public void initView() {
        mContext = this;
        mZXingView = findViewById(R.id.zxingview);
        mWifiAutoConnectManager = new WifiAutoConnectManager(mContext.getApplicationContext(), this);
        mZXingView.setDelegate(ScanActivity.this);
        mZXingView.getScanBoxView().setOnlyDecodeScanBoxArea(false);
        String ssid = getConnectWifiSsid();
        YLog.d(ssid);
    }

    @Override
    public void initData() {
        mConDialog = new QMUITipDialog.Builder(mContext)
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                .setTipWord("正在连接...")
                .create();
        mConDialog.setCancelable(false);
        mConDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new RxPermissions(this)
                .request(Manifest.permission.CAMERA,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.VIBRATE)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if(aBoolean){
                            mZXingView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
                            mZXingView.startSpotAndShowRect();
                        }else{
                            ToastUtils.showShort("拒绝");
                            new PermissionPageUtils(mContext).jumpPermissionPage();
                            finish();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    protected void onStop() {
        mZXingView.stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mZXingView.onDestroy();
        if(mConDialog != null && mConDialog.isShowing()){
            mConDialog.dismiss();
        }
        dismissProgress();
        super.onDestroy();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        if(enableScan) {
            vibrate();
            mZXingView.startSpot(); // 延迟0.5秒后开始识别
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
            mZXingView.stopSpot();
            enableScan = false;
            showProgressDialog();
            mWifiAutoConnectManager.connect(mContext,netWorkName, password, net_type);

//            if (wifiDialog == null) {
//                wifiDialog = wifiInfoDialog(netWorkName, password, netWorkType);
//            }
//            if (wifiDialog != null && !wifiDialog.isShowing()) {
//                wifiDialog.show();
//                mZXingView.stopSpot();
//                enableScan = false;
//            }
        }
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {

    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        ToastUtils.showShort( "err---");
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

    private void checkWifiState(){
        if(isWifi()){
            ToastUtils.showShort("连接成功");
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mConDialog != null && !mConDialog.isShowing()){
                        mConDialog.show();
                    }
                }
            });
            showProgress();
            ThreadExecutors.INSTANCE.getDiskIO().execute(getIpAddrRunnable);
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


    Runnable getIpAddrRunnable = new Runnable() {
        @Override
        public void run() {
            Callable<Boolean> task = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    while(!isWifi()){
                        Thread.sleep(100);
                        YLog.d(System.currentTimeMillis() + "==in while con ==" + isWifi());
                    }
                    boolean connectState = isWifi();
                    YLog.d( System.currentTimeMillis() + "==con==" + connectState);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mConDialog != null && mConDialog.isShowing()){
                                mConDialog.dismiss();
                            }
                        }

                    });
                    ToastUtils.showShort("连接成功");
                    return connectState;
                }
            };
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executorService.submit(task);
            try{
                boolean result = future.get(20, TimeUnit.SECONDS);
                YLog.d("==result==" + result);
            }catch (Exception e){
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timeOutDialog();
                    }
                });
            }finally {
                try {
                    future.cancel(true);
                }catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    executorService.shutdownNow();
                }catch (Exception e){
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mConDialog != null && mConDialog.isShowing()){
                            mConDialog.dismiss();
                        }
                    }
                });
            }
        }
    };



    private void timeOutDialog() {
        new MaterialDialog.Builder(mContext)
                .canceledOnTouchOutside(false)
                .cancelable(false)
                .content("超时")
                .contentColor(ContextCompat.getColor(mContext, R.color.black))
                .backgroundColor(ContextCompat.getColor(mContext, R.color.white))
                .positiveText("确定")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .build()
                .show();
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

        ConnectivityManager ctm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = ctm.getActiveNetworkInfo();
        String ssid = networkInfo.getExtraInfo();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if(wifiInfo != null && wifiInfo.getSSID().length()>2) {
            return wifiInfo.getSSID().substring(1, wifiInfo.getSSID().length()-1);
        }else{
            return "";
        }

        NetworkUtils.isWifiConnected()

    }

}
