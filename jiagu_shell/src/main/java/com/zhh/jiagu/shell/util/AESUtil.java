package com.zhh.jiagu.shell.util;

public class AESUtil {


    /**
     * AES加密
     * @param data 待加密数据
     * @return 返回加密后的数据
     */
    public static native byte[] encrypt(byte[] data);

    /**
     * 对加密数据进行AES解密
     * @param data 加密的数据
     * @return 返回解密的数据
     */
    public static native byte[] decrypt(byte[] data);

    public static void loadJiaGuLibrary(){
        // Used to load the 'nativelib' library on application startup.
        System.loadLibrary("sxjiagu");
    }
}