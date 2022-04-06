package com.zhh.jiagu.demo.utilcode;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Created by zhanghai on 2021/6/22.
 * function：系统设置工具类
 */
public final class SystemSettingUtils {
    //MIUI标识
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";


    private SystemSettingUtils(){}

    /**
     * 是否是小米的MIUI系统
     * @return
     */
    private static boolean isMIUI() {
        return isPropertiesExist(KEY_MIUI_VERSION_CODE, KEY_MIUI_VERSION_NAME,
                KEY_MIUI_INTERNAL_STORAGE);
    }

    /**
     * 判断是否是MIUI
     */
    private static boolean isPropertiesExist(String... keys) {
        if (keys == null || keys.length == 0) {
            return false;
        }
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
            for (String key : keys){
                if (prop.getProperty(key,null) != null){
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 打开应用程序设置界面
     */
    public static void openAppDetailSettings(@NonNull Context context) {
        if (isMIUI()){
            openMIUISetting(context);
        }else {
            openAppCommonSettings(context);
        }
    }

    /**
     * 系统通用打开权限设置方式
     * @param context
     */
    private static void openAppCommonSettings(@NonNull Context context){
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        context.startActivity(intent);
    }

    /**
     * 打开小米系统设置
     * @param context
     */
    private static void openMIUISetting(@NonNull Context context){
        // 只兼容miui v5/v6 的应用权限设置页面，否则的话跳转应用设置页面（权限设置上一级页面）
        String miuiVersion = getMiuiVersion();
        Intent intent = null;
        if ("V6".equals(miuiVersion) || "V7".equals(miuiVersion)) {
            openMIUISettingsByClassName(context,"com.miui.permcenter.permissions.AppPermissionsEditorActivity");
        } else if ("V8".equals(miuiVersion)) {
            openMIUISettingsByClassName(context,"com.miui.permcenter.permissions.PermissionsEditorActivity");
        } else {
            openAppCommonSettings(context);
        }
    }

    public static String getMiuiVersion() {
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop ro.miui.ui.version.name");
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
        return line;
    }

    /**
     * 指定className，打开MIUI的权限设置
     * @param context
     * @param className
     */
    private static void openMIUISettingsByClassName(@NonNull Context context,String className){
        Intent localIntent = new Intent("miui.intent.action.APP_PERM_EDITOR");
        localIntent.setClassName("com.miui.securitycenter", className);
        localIntent.putExtra("extra_pkgname", context.getPackageName());
        context.startActivity(localIntent);
    }
}
