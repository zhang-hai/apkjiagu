package com.zhh.jiagu.shell.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileUtils {

    /**
     * 删除指定的文件或目录
     * @param path
     */
    public static void deleteFile(String path){
        File file = new File(path);
        //文件存在时，执行删除操作
        if (file.exists()){
            if (file.isFile()){
                file.delete();
            }else {
                File[] files = file.listFiles();
                for (File child : files){
                    deleteFile(child.getAbsolutePath());
                }
                //删除空目录
                file.delete();
            }
        }
    }

    /**
     * 将指定的内容写入到一个文件中
     * @param content 输出内容
     * @param outFile 输出的文件
     * @return 返回是否写入成功
     */
    public static boolean writeFile(String content,String outFile){
        try {
            FileOutputStream os = new FileOutputStream(outFile);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
            bw.write(content);
            bw.flush();
            bw.close();
            os.close();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public static String getAppApplicationName(File file){
        if (!file.exists()){
            return null;
        }
        try {
            FileInputStream is = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean isFindApplicationTag = false;
            while ((line = reader.readLine()) != null){
                if (line.contains("E: application")){
                    isFindApplicationTag = true;
                }else if (isFindApplicationTag && line.contains("E: ")){
                    //此时说明application标签无android:name属性
                    line = null;
                    break;
                }else if (isFindApplicationTag && line.contains("android:name")){
                    isFindApplicationTag = false;
                    break;
                }
            }
            reader.close();
            is.close();

            //解析line获取application的class name
            if (line != null && line.contains("\"")){
                String substr = line.substring(line.indexOf("\"")+1);
                String clazzName = substr.substring(0,substr.indexOf("\""));
                return clazzName;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
