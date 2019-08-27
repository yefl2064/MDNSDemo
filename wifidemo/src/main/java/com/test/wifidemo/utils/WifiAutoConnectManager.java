package com.test.wifidemo.utils;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;

import com.blankj.utilcode.util.LogUtils;
import com.coolweather.coolweatherjetpack.util.ThreadExecutors;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by yefl on 2018/2/26.
 */
public class WifiAutoConnectManager {

    private static final String TAG = WifiAutoConnectManager.class.getSimpleName();
    public static final int TYPE_NO_PASSWD = 0x11;
    public static final int TYPE_WEP = 0x12;
    public static final int TYPE_WPA = 0x13;

    WifiManager wifiManager;
    ConnectListener connectListener;

    private List<WifiConfiguration> existingConfigs;

    // 定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
    public WifiAutoConnectManager(Context context, ConnectListener connectListener) {
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.connectListener = connectListener;
    }

    // 构造函数
    public WifiAutoConnectManager(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    // 提供一个外部接口，传入要连接的无线网
    public void connect(Context context, String ssid, String password, int type) {
        Runnable runnable = new ConnectRunnable(context,ssid, password, type);
        ThreadExecutors.INSTANCE.getDiskIO().execute(runnable);
    }

    // 查看以前是否也配置过这个网络
    private WifiConfiguration isExsits(String SSID) {
        if (existingConfigs != null && !existingConfigs.isEmpty()) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;
                }
            }
        }
        return null;
    }

    private WifiConfiguration createWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = isExsits(SSID);
        if (tempConfig != null) {
            wifiManager.removeNetwork(tempConfig.networkId);
            wifiManager.saveConfiguration();
        }

        switch (Type){
            case TYPE_NO_PASSWD:
                config.wepKeys[0] = "";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                break;
            case TYPE_WEP:
                if (!TextUtils.isEmpty(Password)) {
                    if (isHexWepKey(Password)) {
                        config.wepKeys[0] = Password;
                    } else {
                        config.wepKeys[0] = "\"" + Password + "\"";
                    }
                }
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                break;
            case TYPE_WPA:
                config.preSharedKey = "\"" + Password + "\"";
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.status = WifiConfiguration.Status.ENABLED;
                break;
        }
        return config;
    }

    // 打开wifi功能
    private boolean openWifi() {
        boolean bRet = true;
        if (!wifiManager.isWifiEnabled()) {
            bRet = wifiManager.setWifiEnabled(true);
        }
        return bRet;
    }

    class ConnectRunnable implements Runnable {
        private String ssid;

        private String password;

        private int type;
        private Context context;

        public ConnectRunnable(Context context,String ssid, String password, int type) {
            this.ssid = ssid;
            this.password = password;
            this.type = type;
            this.context = context;
        }

        @Override
        public void run() {
            // 打开wifi
            openWifi();
            // 开启wifi功能需要一段时间
            while (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(10*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            existingConfigs = wifiManager.getConfiguredNetworks();
            WifiConfiguration tempConfig = isExsits(ssid);
            if (tempConfig != null) {
                int netId = tempConfig.networkId;
                boolean enabled = wifiManager.enableNetwork(netId, true);
                connectListener.accept(enabled);
            }else {
                connectNewSsid(context,ssid, password, type);
            }
        }
    }

    private boolean forgetNetWork(WifiConfiguration tempConfig){
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            wifiManager.removeNetwork(tempConfig.networkId);
            wifiManager.saveConfiguration();
            return true;
        }else{
            try {
                Method forget = wifiManager.getClass().getDeclaredMethod("forget", int.class, Class.forName("android.net.wifi.WifiManager$ActionListener"));
                if (forget != null) {
                    forget.setAccessible(true);
                    forget.invoke(wifiManager, tempConfig.networkId, null);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private void connectNewSsid(Context context,String ssid, String password, int type){
        WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
        if (wifiConfig == null) {
            return;
        }
        int netID = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        boolean con = wifiManager.enableNetwork(netID, true);
        connectListener.accept(con);
        LogUtils.file("连接成功");

        /**
        Method connectMethod = connectWifiByReflectMethod(netID);
        if (connectMethod == null) {
            // 通用API
            boolean enabled = wifiManager.enableNetwork(netID, true);
        }
        connectListener.accept(true);
        MyUtil.writeFileToSDCard(context.getResources().getString(R.string.scan_connect_success));
         */
    }

    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();
        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }
        return isHex(wepKey);
    }

    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }
        return true;
    }

    public interface ConnectListener {
        void accept(boolean connect);
    }

    /**
     * 通过反射出不同版本的connect方法来连接Wifi
     * @author jiangping.li
     * @param netId
     * @return
     * @since MT 1.0
     *
     */
    private Method connectWifiByReflectMethod(int netId) {
        Method connectMethod = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // 反射方法： connect(int, listener) , 4.2 <= phone's android version
            for (Method methodSub : wifiManager.getClass().getDeclaredMethods()) {
                if ("connect".equalsIgnoreCase(methodSub.getName())) {
                    Class<?>[] types = methodSub.getParameterTypes();
                    if (types != null && types.length > 0) {
                        if ("int".equalsIgnoreCase(types[0].getName())) {
                            connectMethod = methodSub;
                            break;
                        }
                    }
                }
            }
            if (connectMethod != null) {
                try {
                    connectMethod.invoke(wifiManager, netId, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            // 反射方法: connect(Channel c, int networkId, ActionListener listener)
            // 暂时不处理4.1的情况 , 4.1 == phone's android version
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // 反射方法：connectNetwork(int networkId) ,
            // 4.0 <= phone's android version < 4.1
            for (Method methodSub : wifiManager.getClass()
                    .getDeclaredMethods()) {
                if ("connectNetwork".equalsIgnoreCase(methodSub.getName())) {
                    Class<?>[] types = methodSub.getParameterTypes();
                    if (types != null && types.length > 0) {
                        if ("int".equalsIgnoreCase(types[0].getName())) {
                            connectMethod = methodSub;
                        }
                    }
                }
            }
            if (connectMethod != null) {
                try {
                    connectMethod.invoke(wifiManager, netId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        } else {
            // < android 4.0
            return null;
        }
        return connectMethod;
    }


    public static void connectByConfig(WifiManager manager, WifiConfiguration config) {
        if (manager == null) {
            return;
        }
        try {
            Method connect = manager.getClass().getDeclaredMethod("connect", WifiConfiguration.class, Class.forName("android.net.wifi.WifiManager$ActionListener"));
            if (connect != null) {
                connect.setAccessible(true);
                connect.invoke(manager, config, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 使用 networkId 连接.
    public static void connectByNetworkId(WifiManager manager, int networkId) {
        if (manager == null) {
            return;
        }
        try {
            Method connect = manager.getClass().getDeclaredMethod("connect", int.class, Class.forName("android.net.wifi.WifiManager$ActionListener"));
            if (connect != null) {
                connect.setAccessible(true);
                connect.invoke(manager, networkId, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
