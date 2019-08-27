package com.test.wifidemo.utils;

import com.blankj.utilcode.util.LogUtils;

/**
 * @author yefl
 * @date 2019/8/26.
 * description：
 */
public class YLog {
    public static void d(String msg){
        LogUtils.d(msg);
        LogUtils.file(msg);
    }
}
