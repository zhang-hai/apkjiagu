package com.zhh.jiagu.shell.util;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Zip4jUtil {

    /**
     * 通过zip4j对文件进行压缩
     * @param files 压缩的文件列表
     * @param outZip 输出的zip文件
     * @throws ZipException 异常
     */
    public static void zipFiles(File[] files,File outZip) throws ZipException {
        if (files == null || files.length <= 0){
            return;
        }
        // 生成的压缩文件
        ZipFile zipFile = new ZipFile(outZip);
        ZipParameters parameters = new ZipParameters();
        // 压缩方式
        parameters.setCompressionMethod(CompressionMethod.DEFLATE);
        // 压缩级别
        parameters.setCompressionLevel(CompressionLevel.NORMAL);
        // 遍历test文件夹下所有的文件、文件夹
        for (File f : files) {
            if (f.isDirectory()) {
                zipFile.addFolder(f, parameters);
            } else {
                zipFile.addFile(f, parameters);
            }
        }
    }

    /**
     * 采用zip4j解压
     * @param inputFile 输入的zip文件对象
     * @param outFile 解压输出的目录
     */
    private static void unzip(File inputFile,File outFile) {
        try {
            ZipFile zipFile = new ZipFile(inputFile);
            zipFile.extractAll(outFile.getAbsolutePath());
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    /**
     * 提取单个文件到指定的目录中
     * @param zip
     * @param fileName
     * @param outDir
     */
    public static void extractFile(String zip,String fileName,String outDir){
        try {
            ZipFile zipFile = new ZipFile(zip);
            zipFile.extractFile(fileName,outDir);
        }catch (ZipException e){
            e.printStackTrace();
        }
    }


    /**
     * 采用Zip4j进行添加dex,不进行压缩
     * 向zip中追加一个文件
     * @param zip zip包
     * @param filepath 追加的文件
     */
    public static void addFile2Zip(String zip,String filepath,String rootFolder) throws ZipException{
        ZipFile zipFile = new ZipFile(zip);
        ZipParameters parameters = new ZipParameters();
        /*
         * 压缩方式
         * COMP_STORE = 0;（仅打包，不压缩）
         * COMP_DEFLATE = 8;（默认）
         * COMP_AES_ENC = 99; 加密压缩
         */
        parameters.setCompressionMethod(CompressionMethod.DEFLATE);
        /*
         * 压缩级别
         * DEFLATE_LEVEL_FASTEST = 1; (速度最快，压缩比最小)
         * DEFLATE_LEVEL_FAST = 3; (速度快，压缩比小)
         * DEFLATE_LEVEL_NORMAL = 5; (一般)
         * DEFLATE_LEVEL_MAXIMUM = 7;
         * DEFLATE_LEVEL_ULTRA = 9;
         */
        parameters.setCompressionLevel(CompressionLevel.NORMAL);
        // 目标路径
        if (rootFolder == null){
            rootFolder = "";
        }
        parameters.setRootFolderNameInZip(rootFolder);
        zipFile.addFile(filepath, parameters);
    }


    /**
     * 删除dex文件
     * @param zipFilePath zip对象
     * @throws ZipException 异常
     */
    public static void deleteDexFromZip(String zipFilePath) throws ZipException{
        ZipFile zipFile = new ZipFile(zipFilePath);
        List<FileHeader> files = zipFile.getFileHeaders();
        List<String> dexFiles = new ArrayList<>();
        for (FileHeader file : files) {
            if (file.getFileName().endsWith(".dex")) {
                dexFiles.add(file.getFileName());
            }
        }
        zipFile.removeFiles(dexFiles);
    }

}
