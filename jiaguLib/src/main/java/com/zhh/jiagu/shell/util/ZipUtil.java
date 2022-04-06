/*
 *  四川生学教育科技有限公司
 *  Copyright (c) 2015-2025 Founder Ltd. All Rights Reserved.
 *
 *  This software is the confidential and proprietary information of
 *  Founder. You shall not disclose such Confidential Information
 *  and shall use it only in accordance with the terms of the agreements
 *  you entered into with Founder.
 */

package com.zhh.jiagu.shell.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    /**
     * zip文件解压
     *
     * @param apkFile   待解压文件夹/文件
     * @param destDir 解压路径
     */
    public static void unZip(File apkFile,File destDir) throws Exception{
        // 判断源文件是否存在
        if (!apkFile.exists()) {
            throw new Exception(apkFile.getPath() + "所指文件不存在");
        }
        //开始解压
        //构建解压输入流
        ZipInputStream zIn = new ZipInputStream(new FileInputStream(apkFile));
        ZipEntry entry = null;
        File file = null;
        while ((entry = zIn.getNextEntry()) != null) {
            if (!entry.isDirectory() && !entry.getName().equals("")) {
                file = new File(destDir, entry.getName());
                if (!file.exists()) {
                    file.getParentFile().mkdirs();//创建此文件的上级目录
                }
                FileOutputStream fos = new FileOutputStream(file);
                int len = -1;
                byte[] buf = new byte[1024];
                while ((len = zIn.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }
                // 关流顺序，先打开的后关闭
                fos.flush();
                fos.close();
            }else {
                file = new File(destDir, entry.getName());
                //是文件夹的时候创建目录
                if (!file.exists()){
                    file.mkdirs();
                }
            }
            zIn.closeEntry();
        }
        zIn.close();
    }



}
