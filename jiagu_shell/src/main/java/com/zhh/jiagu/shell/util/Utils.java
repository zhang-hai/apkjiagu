package com.zhh.jiagu.shell.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {


    /**
     * 从apk包里面获取dex文件内容（byte）
     * @return
     * @throws IOException
     */
    public static byte[] readDexFileFromApk(String apkPath) throws IOException {
        LogUtil.info("从classes.dex解析出加密的原包的dex数据");
        ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();
        //获取当前zip进行解压
        ZipInputStream zipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(apkPath)));
        while (true) {
            ZipEntry entry = zipInputStream.getNextEntry();
            if (entry == null) {
                zipInputStream.close();
                break;
            }
            if (entry.getName().equals("classes.dex")) {
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = zipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    dexByteArrayOutputStream.write(arrayOfByte, 0, i);
                }
            }
            zipInputStream.closeEntry();
        }
        zipInputStream.close();
        return dexByteArrayOutputStream.toByteArray();
    }

    /**
     * 释放被加壳的AppDex.zip，so文件
     * @param apkdata classes.dex数据
     * @throws IOException 异常
     */
    public static void releaseAppDexFile(byte[] apkdata,String apkFileName) throws Exception {
        int length = apkdata.length;

        //取被加壳apk的长度   这里的长度取值，对应加壳时长度的赋值都可以做些简化
        byte[] dexlen = new byte[4];
        System.arraycopy(apkdata, length - 4, dexlen, 0, 4);
        ByteArrayInputStream bais = new ByteArrayInputStream(dexlen);
        DataInputStream in = new DataInputStream(bais);
        int readInt = in.readInt();
        LogUtil.info("============ 读取原Dex压缩文件大小 ======"+readInt);
        byte[] newdex = new byte[readInt];
        //把被加壳apk内容拷贝到newdex中
        System.arraycopy(apkdata, length - 4 - readInt, newdex, 0, readInt);


        LogUtil.info("============ 开始对加密dex进行解密======" + newdex.length);
        //对zip包进行解密
        newdex = AESUtil.decrypt(newdex);

        LogUtil.info("============ 解密后的大小为======" + newdex.length);

        //写入AppDex.zip文件
        File file = new File(apkFileName);
        try {
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            localFileOutputStream.write(newdex);
            localFileOutputStream.close();
        } catch (IOException localIOException) {
            throw new RuntimeException(localIOException);
        }
        //LogUtil.info("============ 开始对压缩包进行解压得到dex文件======");
        // todo:由于仅加密的是源dex文件，故这里不需要检查so文件

    }
}
