package com.zhh.jiagu;

import com.zhh.jiagu.shell.entity.KeyStore;
import com.zhh.jiagu.shell.util.AESUtil;
import com.zhh.jiagu.shell.util.FileUtils;
import com.zhh.jiagu.shell.util.KeyStoreUtil;
import com.zhh.jiagu.shell.util.ProcessUtil;
import com.zhh.jiagu.shell.util.Zip4jUtil;
import com.zhh.jiagu.shell.util.ZipUtil;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.Adler32;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class JiaGuMain {

//    private final static String ROOT = "jiaguLib/";
    private static String ROOT = "";
    private static String OUT_TMP = ROOT+"temp/";

    private static String ORIGIN_APK = "demo/release/demo-release.apk";

    private static String KEYSTORE_CFG = "keystore.cfg";

    /**
     * 是否发布为Jar包，运行的
     */
    private final static boolean isRelease = true;

    static {
        File file = new File(ROOT);
        String strDll = file.getAbsolutePath() + (isRelease ? "" : "/jiaguLib")+"/libs/sx_jiagu.dll";
        System.out.println("根目录=========>" + strDll);
        //load - 支持采用绝对路径的dll库
        //loadLibrary 加载的是jre/bin下的dll库
        System.load(strDll);//这是我即将要重新实现的动态库名字
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (!isRelease){
            ROOT = "jiaguLib/";
            OUT_TMP = ROOT+"temp/";
            JiaGuMain jiagu = new JiaGuMain();
            jiagu.beginJiaGu();
        }else {
            if (args == null || args.length != 2){
                System.out.println("请使用：java -jar jiaguLib.jar [apk文件] [签名配置文件]");
                return;
            }
            String arg = args[0];
            KEYSTORE_CFG = args[1];
            File file = new File(arg);
            if (file.exists() && arg.endsWith(".apk")){
                if (new File(KEYSTORE_CFG).exists()){
                    ORIGIN_APK = arg;
                    JiaGuMain jiagu = new JiaGuMain();
                    jiagu.beginJiaGu();
                }else {
                    System.out.println("签名配置文件不存在!");
                }
            }else {
                System.out.println(arg + " is invalid apk path.");
            }
        }
    }

    /**
     * 将壳dex和待加固的APK进行加密后合成新的dex文件
     *
     * 具体步骤：
     * 步骤一：将加固壳中的aar中的jar转成dex文件
     * 步骤二：将需要加固的APK解压，并将所有dex文件打包成一个zip包，方便后续进行加密处理
     * 步骤三：对步骤二的zip包进行加密，并与壳dex合成新dex文件
     * 步骤四：修改AndroidManifest（Application的android:name属性和新增<meta-data>）
     * 步骤五：将步骤三生成的新dex文件替换apk中的所有dex文件
     * 步骤六：APK对齐处理
     * 步骤七：对生成的APK进行签名
     *
     */
    public void beginJiaGu(){
        try {
            //前奏 - 先将目录删除
            FileUtils.deleteFile(OUT_TMP);

//            步骤一：将加固壳中的aar中的jar转成dex文件
            File shellDexFile = shellAar2Dex();
            //步骤二：将需要加固的APK解压，并将所有dex文件打包成一个zip包，方便后续进行加密处理
            File dexZipFile = apkUnzipAndZipDexFiles();
            if (dexZipFile == null){
                return;
            }
            //步骤三：对步骤二的zip包进行加密，并与壳dex合成新dex文件
            File dexFile = combine2NewDexFile(shellDexFile,dexZipFile);
            //步骤四：修改AndroidManifest（Application的android:name属性和新增<meta-data>）
            String outpath = modifyOriginApkManifest();
//            String outpath = modifyOriginApkManifest2();

            //步骤五：将步骤三生成的新dex文件替换apk中的所有dex文件
            if (dexFile != null && !outpath.isEmpty()){
                boolean ret = replaceDexFiles(outpath,dexFile.getPath());
                //步骤六：APK对齐处理
                if (ret){
//                    String outpath = OUT_TMP + ORIGIN_APK.substring(ORIGIN_APK.lastIndexOf("/")+1);
                    File apk = zipalignApk(new File(outpath));
                    //步骤七：对生成的APK进行签名
                    resignApk(apk);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 步骤一：将加固壳中的aar中的jar转成dex文件
     * @throws Exception 异常
     */
    private File shellAar2Dex() throws Exception{
        logTitle("步骤一：将加固壳中的aar中的jar转成dex文件");
        //步骤一：将加固壳中的aar中的jar转成dex文件
        File aarFile = new File(ROOT+"aar/jiagu_shell-release.aar");
        File aarTemp = new File(OUT_TMP+"shell");
        ZipUtil.unZip(aarFile, aarTemp);
        File classesJar = new File(aarTemp, "classes.jar");
        File classesDex = new File(aarTemp, "classes.dex");
        boolean ret = ProcessUtil.executeCommand(String.format(Locale.CHINESE,"dx --dex --output %s %s",classesDex.getPath(),classesJar.getPath()));
        if (ret){
            System.out.println("已生成======"+classesDex.getPath());
        }
        return classesDex;
    }

    /**
     * 步骤二：将需要加固的APK解压，并将所有dex文件打包成一个zip包，方便后续进行加密处理
     *
     * @throws Exception 异常
     */
    private File apkUnzipAndZipDexFiles(){
        logTitle("步骤二：将需要加固的APK解压，并将所有dex文件打包成一个zip包，方便后续进行加密处理");
        //下面加密码APK中所有的dex文件
        File apkFile = new File(ORIGIN_APK);
        File apkTemp = new File(OUT_TMP+"unzip/");
        try {
            //首先把apk解压出来
            ZipUtil.unZip(apkFile, apkTemp);

            //其次获取解压目录中的dex文件
            File dexFiles[] = apkTemp.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.endsWith(".dex");
                }
            });

            if (dexFiles == null) return null;

            //三：将所有的dex文件压缩为AppDex.zip文件
            File outTmpFile = new File(OUT_TMP);
            File outputFile = new File(outTmpFile,"AppDex.zip");
            //创建目录
            if (!outTmpFile.exists()){
                outTmpFile.mkdirs();
            }
            if (outputFile.exists()){
                outputFile.delete();
            }
            Zip4jUtil.zipFiles(dexFiles,outputFile);
            System.out.println("已生成======"+outputFile.getPath());

            FileUtils.deleteFile(apkTemp.getPath());

            return outputFile;
        }catch (Exception e){
             e.printStackTrace();
        }

        return null;
    }


    /**
     * 步骤三：对步骤二的zip包进行加密，并与壳dex合成新dex文件
     * @param shellDexFile 壳dex文件
     * @param originalDexZipFile 原包中dex压缩包
     */
    private File combine2NewDexFile(File shellDexFile,File originalDexZipFile){
        logTitle("步骤三：对步骤二的zip包进行加密，并与壳dex合成新dex文件");
        try {
            AESUtil aesUtil = new AESUtil();
            byte[] data = readFileBytes(originalDexZipFile);
            System.out.println("加密前数据大小为："+data.length);
            byte[] payloadArray = aesUtil.encrypt(data);//以二进制形式读出zip，并进行加密处理//对源Apk进行加密操作
            byte[] unShellDexArray = readFileBytes(shellDexFile);//以二进制形式读出dex
            int payloadLen = payloadArray.length;
            int unShellDexLen = unShellDexArray.length;
            int totalLen = payloadLen + unShellDexLen +4;//多出4字节是存放长度的。
            byte[] newdex = new byte[totalLen]; // 申请了新的长度
            //添加解壳代码
            System.arraycopy(unShellDexArray, 0, newdex, 0, unShellDexLen);//先拷贝dex内容
            //添加加密后的解壳数据
            System.arraycopy(payloadArray, 0, newdex, unShellDexLen, payloadLen);//再在dex内容后面拷贝apk的内容
            //添加解壳数据长度
            System.arraycopy(intToByte(payloadLen), 0, newdex, totalLen-4, 4);//最后4为长度

            //修改DEX file size文件头
            fixFileSizeHeader(newdex);
            //修改DEX SHA1 文件头
            fixSHA1Header(newdex);
            //修改DEX CheckSum文件头
            fixCheckSumHeader(newdex);

            String str = OUT_TMP + "classes.dex";
            File file = new File(str);
            if (!file.exists()) {
                file.createNewFile();
            }

            //输出成新的dex文件
            FileOutputStream localFileOutputStream = new FileOutputStream(str);
            localFileOutputStream.write(newdex);
            localFileOutputStream.flush();
            localFileOutputStream.close();
            System.out.println("已生成新的Dex文件======"+str);

            //删除dex的zip包
            FileUtils.deleteFile(originalDexZipFile.getAbsolutePath());
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 步骤四：修改AndroidManifest（Application的android:name属性和新增<meta-data>）
     * 反编译：采用apktool+xml解析方式对AndroidManifest.xml进行修改
     * 优点：不用依赖其他jar包，
     * 缺点：非常耗时，在21000ms左右(不包含删除反编译的临时文件的时间)
     */
    private String modifyOriginApkManifest() throws Exception{
        String apkPath = ORIGIN_APK;
        String outputPath = OUT_TMP + "apk/";
        logTitle("步骤四：修改AndroidManifest（Application的android:name属性和新增<meta-data>）");
        String path = "";
        long start = System.currentTimeMillis();
        //1：执行命令进行反编译原apk
        System.out.println("开始反编译原apk ......");
        boolean ret = ProcessUtil.executeCommand("apktool d -o " + outputPath + " " + apkPath);
        if (ret){
            //2.修改AndroidManifest.xml，使用壳的Application替换原Application,并将原Application名称配置在meta-data中
            modifyAndroidManifest(new File(outputPath,"AndroidManifest.xml"));

            //3：重新编译成apk,仍以原来名称命名
            System.out.println("开始回编译apk ......");
            String apk = OUT_TMP + apkPath.substring(apkPath.lastIndexOf("/")+1);
            ret = ProcessUtil.executeCommand(String.format(Locale.CHINESE,"apktool b -o %s %s",apk,outputPath));
            if (ret){
                path = apk;
            }
            System.out.println("=== modifyOriginApkManifest ==== "+(System.currentTimeMillis()-start)+"ms");
        }
        //删除解压的目录
        FileUtils.deleteFile(outputPath);

        return path;
    }

    /**
     * 步骤四：修改AndroidManifest（Application的android:name属性和新增<meta-data>）
     *
     * 非反编译：采用AAPT和AXMLEditor.jar组合方式对AndroidManifest.xml进行修改
     * 优点：高效 耗时 在520ms左右
     * 缺点：多依赖AXMLEditor.jar包（https://github.com/fourbrother/AXMLEditor）
     */
    private String modifyOriginApkManifest2() throws Exception{
        String apkPath = ORIGIN_APK;
        logTitle("步骤四：修改AndroidManifest（Application的android:name属性和新增<meta-data>）");
        String outApk = "";

        long start = System.currentTimeMillis();

        File outAmFile = new File(OUT_TMP,"am.txt");
        //1.执行命令：aapt dump xmltree app_preview3.apk AndroidManifest.xml > out.txt ，得到导出的配置文件
        ProcessUtil.executeCommand("aapt dump xmltree " + apkPath + " AndroidManifest.xml >" + outAmFile.getPath());

        //2.读取输出的文件内容，获取到Application name
        String clazzName = FileUtils.getAppApplicationName(outAmFile);
        System.out.println("application-name ->"+clazzName);
        FileUtils.deleteFile(outAmFile.getPath());

        //3.将AndroidManifest.xml从apk中提取出来
        Zip4jUtil.extractFile(apkPath,"AndroidManifest.xml",OUT_TMP);
        File manifestFile = new File(OUT_TMP,"AndroidManifest.xml");
        if (manifestFile.exists()){
            //4.运行AXMLEditor修改AndroidManifest.xml文件
            String shellClazzName = "com.zhh.jiagu.shell.StubApplication";
            //先删除application 的android:name属性
            String cmd = String.format(Locale.CHINESE,"java -jar %slibs/AXMLEditor.jar -attr -r application package name %s %s",ROOT,manifestFile.getPath(),manifestFile.getPath());
            ProcessUtil.executeCommand(cmd);
            //修改命令：java -jar AXMLEditor.jar -attr -m [标签名] [标签唯一标识] [属性名] [属性值] [输入xml] [输出xml]
            //添加application 的android:name属性
            cmd = String.format(Locale.CHINESE,"java -jar %slibs/AXMLEditor.jar -attr -m application package name %s %s %s",ROOT,shellClazzName,manifestFile.getPath(),manifestFile.getPath());
            ProcessUtil.executeCommand(cmd);
            //先将<meta-data>属性写入到一个xml文件中，然后添加<meta-data>属性
            String metaXml = OUT_TMP+"meta.xml";
            clazzName = clazzName == null || clazzName.length() <= 0 ? "android.app.Application":clazzName;
            FileUtils.writeFile("<meta-data android:name=\"APPLICATION_CLASS_NAME\" android:value=\""+clazzName+"\"/>",metaXml);
            //插入标签：java -jar AXMLEditor.jar -tag -i [需要插入标签内容的xml文件] [输入xml] [输出xml]
            cmd = String.format(Locale.CHINESE,"java -jar %slibs/AXMLEditor.jar -tag -i %s %s %s",ROOT,metaXml,manifestFile.getPath(),manifestFile.getPath());
            ProcessUtil.executeCommand(cmd);
            FileUtils.deleteFile(metaXml);

            //5.将修改完成的AndroidManifest.xml重新添加到apk中
            outApk = OUT_TMP + apkPath.substring(apkPath.lastIndexOf("/")+1);
            Files.copy(new File(ORIGIN_APK).toPath(),new File(outApk).toPath(), StandardCopyOption.REPLACE_EXISTING);
            ProcessUtil.executeCommand("aapt r "+outApk + " AndroidManifest.xml");
            Zip4jUtil.addFile2Zip(outApk,manifestFile.getPath(),"");
            FileUtils.deleteFile(manifestFile.getPath());
            System.out.println("=== modifyOriginApkManifest2 ==== "+(System.currentTimeMillis()-start)+"ms");

        }

        return outApk;
    }



    /**
     * 步骤五：将步骤三生成的新dex文件替换apk中的所有dex文件
     * 替换zip中的所有dex文件
     * @param zipPath zip文件路径
     * @param newDexPath 新的dex文件路径
     */
    private boolean replaceDexFiles(String zipPath,String newDexPath){
        logTitle("步骤五：删除原APK中的DEX文件，并放入可APK的dex文件");
        try {
            Zip4jUtil.deleteDexFromZip(zipPath);
            Zip4jUtil.addFile2Zip(zipPath,newDexPath,"");
            //将加固的so文件添加到apk的lib中
            ZipFile zipFile = new ZipFile(zipPath);
            final boolean[] boolArm = new boolean[4];
            zipFile.getFileHeaders().stream().filter(new Predicate<FileHeader>() {
                @Override
                public boolean test(FileHeader fileHeader) {
                    return fileHeader != null && fileHeader.getFileName().endsWith(".so");
                }
            }).forEach(new Consumer<FileHeader>() {
                @Override
                public void accept(FileHeader fileHeader) {
                    String name = fileHeader.getFileName();
                    if (name.contains("armeabi-v7a")){
                        boolArm[0] = true;
                    }else if (name.contains("arm64-v8a")){
                        boolArm[1] = true;
                    } else if (name.contains("x86")){
                        boolArm[2] = true;
                    }else if (name.contains("x86_64")){
                        boolArm[3] = true;
                    }
                }
            });
            if (!boolArm[0] && !boolArm[1] && !boolArm[2] && !boolArm[3]){
                Zip4jUtil.addFile2Zip(zipPath,OUT_TMP+"shell/jni/armeabi-v7a/libsxjiagu.so","lib/armeabi-v7a/");
            }else {
                if (boolArm[0]){
                    Zip4jUtil.addFile2Zip(zipPath,OUT_TMP+"shell/jni/armeabi-v7a/libsxjiagu.so","lib/armeabi-v7a/");
                }
                if (boolArm[1]){
                    Zip4jUtil.addFile2Zip(zipPath,OUT_TMP+"shell/jni/arm64-v8a/libsxjiagu.so","lib/arm64-v8a/");
                }
                if (boolArm[2]){
                    Zip4jUtil.addFile2Zip(zipPath,OUT_TMP+"shell/jni/x86/libsxjiagu.so","lib/x86/");
                }
                if (boolArm[3]){
                    Zip4jUtil.addFile2Zip(zipPath,OUT_TMP+"shell/jni/x86_64/libsxjiagu.so","lib/x86_64/");
                }
            }

            FileUtils.deleteFile(OUT_TMP+"shell");
            FileUtils.deleteFile(newDexPath);

            System.out.println("已完成替换加密后的Dex文件======");
            return true;
        }catch (ZipException e){
            e.printStackTrace();
        }
        return false;
    }

    private boolean matchCpuArm(Stream<FileHeader> stream,String cpuArm){
        boolean match = stream.anyMatch(new Predicate<FileHeader>() {
            @Override
            public boolean test(FileHeader fileHeader) {
                return fileHeader.getFileName().contains(cpuArm);
            }
        });
        stream.close();
        return match;
    }

    /**
     * 步骤六：将已经签名的APK进行对齐处理
     * @param unAlignedApk 需要对齐的apk文件对象
     * @throws Exception 异常
     */
    private File zipalignApk(File unAlignedApk) throws Exception{
        logTitle("步骤六：重新对APK进行对齐处理.....");
        //步骤四：重新对APK进行对齐处理
        File alignedApk = new File(unAlignedApk.getParent(),unAlignedApk.getName().replace(".apk","_align.apk"));
        boolean ret = ProcessUtil.executeCommand("zipalign -v -p 4 " + unAlignedApk.getPath() + " " + alignedApk.getPath());
        if (ret){
            System.out.println("已完成APK进行对齐处理======");
        }
        //删除未对齐的包
        FileUtils.deleteFile(unAlignedApk.getPath());

        return alignedApk;
    }

    /**
     * 步骤七：对生成的APK进行签名
     *
     * @param unSignedApk 需要签名的文件对象
     * @return 返回签名后的文件对象
     * @throws Exception 异常
     */
    private File resignApk(File unSignedApk) throws Exception{
        logTitle("步骤七：对生成的APK进行签名");
        KeyStore store = KeyStoreUtil.readKeyStoreConfig((isRelease ? "":"jiaguLib/")+KEYSTORE_CFG);
        //步骤五：对APK进行签名
        File signedApk = new File(ROOT+"out",unSignedApk.getName().replace(".apk","_signed.apk"));
        //创建保存加固后apk目录
        if (!signedApk.getParentFile().exists()){
            signedApk.getParentFile().mkdirs();
        }

        String signerCmd = String.format("apksigner sign --ks %s --ks-key-alias %s --min-sdk-version 21 --ks-pass pass:%s --key-pass pass:%s --out %s %s",
                store.storeFile,store.alias,store.storePassword,store.keyPassword,signedApk.getPath(),unSignedApk.getPath());

        boolean ret = ProcessUtil.executeCommand(signerCmd);
        System.out.println("已完成签名======"+signedApk.getPath());
        //删除未对齐的包
        FileUtils.deleteFile(unSignedApk.getPath());
        return signedApk;
    }


    /**
     * 读取原包的AndroidManifest文件并修改
     * @param xmlFile 原AndroidManifest文件对象
     */
    private void modifyAndroidManifest(File xmlFile){
        if (xmlFile == null){
            System.out.println("请设置AndroidManifest.xml文件");
            return;
        }
        if (!xmlFile.exists()){
            System.out.println("指定的AndroidManifest.xml文件不存在");
            return;
        }
        System.out.println("开始修改AndroidManifest.xml......");
        String shellApplicationName = "com.zhh.jiagu.shell.StubApplication";
        String metaDataName = "APPLICATION_CLASS_NAME";
        String attrName = "android:name";

        //采用Dom读取AndroidManifest.xml文件
        try {
            //1.实例化Dom工厂
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            //2.构建一个builder
            DocumentBuilder builder = factory.newDocumentBuilder();
            //3.通过builder解析xml文件
            Document document = builder.parse(xmlFile);
            NodeList nl = document.getElementsByTagName("application");
            if (nl != null){
                Node app = nl.item(0);
                //获取原APK中application
                String applicationName = "android.app.Application";
                NamedNodeMap attrMap = app.getAttributes();
                //有属性时
                Node node = app.getAttributes().getNamedItem(attrName);
                //默认为系统的Application
                if (node != null){
                    applicationName = node.getNodeValue();
                    node.setNodeValue(shellApplicationName);
                }else {//不存在该属性时，则创建一个
                    Attr attr = document.createAttribute(attrName);
                    attr.setValue(shellApplicationName);
                    attrMap.setNamedItem(attr);
                }

                //添加<meta-data>数据，记录原APK的application
                Element metaData = document.createElement("meta-data");
                metaData.setAttribute("android:name",metaDataName);
                metaData.setAttribute("android:value",applicationName);
                app.appendChild(metaData);


                //重新写入文件xml文件
                TransformerFactory outFactory = TransformerFactory.newInstance();
                Transformer transformer = outFactory.newTransformer();
                Source xmlSource = new DOMSource(document);
                Result outResult = new StreamResult(xmlFile);
                transformer.transform(xmlSource,outResult);
                System.out.println("已完成修改AndroidManifest文件======");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //直接返回数据，读者可以添加自己加密方法
    private byte[] encrpt(byte[] srcdata){
        for(int i = 0;i<srcdata.length;i++){
            srcdata[i] = (byte)(0xFF ^ srcdata[i]);
        }
        return srcdata;
    }

    /**
     * 修改dex头，CheckSum 校验码
     * @param dexBytes
     */
    private void fixCheckSumHeader(byte[] dexBytes) {
        Adler32 adler = new Adler32();
        adler.update(dexBytes, 12, dexBytes.length - 12);//从12到文件末尾计算校验码
        long value = adler.getValue();
        int va = (int) value;
        byte[] newcs = intToByte(va);
        //高位在前，低位在前掉个个
        byte[] recs = new byte[4];
        for (int i = 0; i < 4; i++) {
            recs[i] = newcs[newcs.length - 1 - i];
//            System.out.println(Integer.toHexString(newcs[i]));
        }
        System.arraycopy(recs, 0, dexBytes, 8, 4);//效验码赋值（8-11）
//        System.out.println(Long.toHexString(value));
//        System.out.println();
    }


    /**
     * int 转byte[]
     * @param number
     * @return
     */
    public byte[] intToByte(int number) {
        byte[] b = new byte[4];
        for (int i = 3; i >= 0; i--) {
            b[i] = (byte) (number % 256);
            number >>= 8;
        }
        return b;
    }

    /**
     * 修改dex头 sha1值
     * @param dexBytes
     * @throws NoSuchAlgorithmException
     */
    private void fixSHA1Header(byte[] dexBytes)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(dexBytes, 32, dexBytes.length - 32);//从32为到结束计算sha--1
        byte[] newdt = md.digest();
        System.arraycopy(newdt, 0, dexBytes, 12, 20);//修改sha-1值（12-31）
        //输出sha-1值，可有可无
        String hexstr = "";
        for (int i = 0; i < newdt.length; i++) {
            hexstr += Integer.toString((newdt[i] & 0xff) + 0x100, 16)
                    .substring(1);
        }
        //System.out.println(hexstr);
    }

    /**
     * 修改dex头 file_size值
     * @param dexBytes
     */
    private void fixFileSizeHeader(byte[] dexBytes) {
        //新文件长度
        byte[] newfs = intToByte(dexBytes.length);
        System.out.println("fixFileSizeHeader ===== size : " + dexBytes.length);
        byte[] refs = new byte[4];
        //高位在前，低位在前掉个个
        for (int i = 0; i < 4; i++) {
            refs[i] = newfs[newfs.length - 1 - i];
            //System.out.println(Integer.toHexString(newfs[i]));
        }
        System.arraycopy(refs, 0, dexBytes, 32, 4);//修改（32-35）
    }


    /**
     * 以二进制读出文件内容
     * @param file
     * @return
     * @throws IOException
     */
    private byte[] readFileBytes(File file) throws IOException {
        byte[] arrayOfByte = new byte[1024];
        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        while (true) {
            int i = fis.read(arrayOfByte);
            if (i != -1) {
                localByteArrayOutputStream.write(arrayOfByte, 0, i);
            } else {
                return localByteArrayOutputStream.toByteArray();
            }
        }
    }

    private void logTitle(String title){
        System.out.println("==================== "+title+" ====================");
    }
}
