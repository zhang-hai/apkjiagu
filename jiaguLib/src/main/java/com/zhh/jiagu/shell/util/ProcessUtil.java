package com.zhh.jiagu.shell.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessUtil {

    /**
     * 执行命令
     * @param cmd
     * @throws Exception
     */
    public static boolean executeCommand(String cmd) throws Exception{
        System.out.println("开始执行命令===>"+cmd);
        Process process = Runtime.getRuntime().exec("cmd /c "+cmd);
        ProcessUtil.consumeInputStream(process.getInputStream());
        ProcessUtil.consumeInputStream(process.getErrorStream());
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new RuntimeException("执行命令错误===>"+cmd);
        }
        return true;
    }


    /**
     *   消费inputstream，并返回
     */
    public static void consumeInputStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String s ;
//        StringBuilder sb = new StringBuilder();
        while((s=br.readLine())!=null){
            System.out.println(s);
//            sb.append(s);
        }
    }
}
