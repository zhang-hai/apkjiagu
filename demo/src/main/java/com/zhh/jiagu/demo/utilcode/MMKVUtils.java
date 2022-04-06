package com.zhh.jiagu.demo.utilcode;

import androidx.annotation.NonNull;

import com.tencent.mmkv.MMKV;

/**
 * Created by daiyong on 2019/4/22.
 * E-mail:dyong@sxw.cn
 * add:成都天府软件园E3-3F
 */

public class MMKVUtils {

    public static void saveInt(String tag, int value) {
        MMKV kv = MMKV.defaultMMKV();
        kv.encode(tag, value);
    }

    public static int getInt(String tag, int defaultValue) {
        MMKV kv = MMKV.defaultMMKV();
        return kv.decodeInt(tag, defaultValue);
    }

    public static void saveFloat(String tag, float value) {
        MMKV kv = MMKV.defaultMMKV();
        kv.encode(tag, value);
    }

    public static float getFloat(String tag, float defaultValue) {
        MMKV kv = MMKV.defaultMMKV();
        return kv.decodeFloat(tag, defaultValue);
    }

    public static void saveLong(String tag, long value) {
        MMKV kv = MMKV.defaultMMKV();
        kv.encode(tag, value);
    }

    public static long getLong(String tag, long defaultValue) {
        MMKV kv = MMKV.defaultMMKV();
        return kv.decodeLong(tag, defaultValue);
    }

    public static void saveStr(String tag, String value) {
        MMKV kv = MMKV.defaultMMKV();
        kv.encode(tag, value);
    }

    public static String getStr(String tag) {
        MMKV kv = MMKV.defaultMMKV();
        return kv.decodeString(tag);
    }

    public static void saveBoolean(String tag, boolean value) {
        MMKV kv = MMKV.defaultMMKV();
        kv.encode(tag, value);
    }

    public static boolean getBoolean(String tag) {
        MMKV kv = MMKV.defaultMMKV();
        return kv.decodeBool(tag, false);
    }

    public static boolean getBoolean(String tag, boolean defFlag) {
        MMKV kv = MMKV.defaultMMKV();
        return kv.decodeBool(tag, defFlag);
    }

    //移除指定key的值
    public static void removeKey(@NonNull String key){
        MMKV mmkv = MMKV.defaultMMKV();
        mmkv.removeValueForKey(key);
    }
}
