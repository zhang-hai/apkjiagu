package com.zhh.jiagu.shell.dex;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.zhh.jiagu.shell.util.LogUtil;
import com.zhh.jiagu.shell.util.RefInvoke;
import com.zhh.jiagu.shell.util.Utils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import dalvik.system.DexClassLoader;

/**
 * 方式一：壳程序直接对APK进行加密方式
 *
 * 这里对APK解密及加载
 */
public class LoadDexUtil {


    /**
     * 解析apk，得到加密的AppDex.zip文件，并进行解密
     * @param context 壳的context
     * @param appVersionCode
     */
    public static boolean decodeDexAndReplace(Application context, int appVersionCode){
        try {
            //创建两个文件夹payload_odex，payload_lib 私有的，可写的文件目录
            File odex = context.getDir("payload_odex", Application.MODE_PRIVATE);
//            File libs = context.getDir("payload_lib", Application.MODE_PRIVATE);
            String odexPath = odex.getAbsolutePath();
            //按版本号来标记zip
            String dexFilePath = String.format(Locale.CHINESE,"%s/AppDex_%d.zip",odexPath,appVersionCode);

            LogUtil.info("decodeDexAndReplace =============================开始");

            File dexFile = new File(dexFilePath);
            LogUtil.info("apk size ===== "+dexFile.length());
//            if (dexFile.exists()){
//                dexFile.delete();
//            }
            //第一次加载APP
            if (!dexFile.exists()) {
                //先清空odexPath目录中文件,防止数据越来越多
                File[] children = odex.listFiles();
                if (children != null && children.length > 0){
                    for (File child : children){
                        child.delete();
                    }
                }
                LogUtil.info( " ===== App is first loading.");
                long start = System.currentTimeMillis();
                dexFile.createNewFile();  //在payload_odex文件夹内，创建payload.apk

                String apkPath = context.getApplicationInfo().sourceDir;
                // 读取程序classes.dex文件
                byte[] dexdata = Utils.readDexFileFromApk(apkPath);

                //从classes.dex中再取出AppDex.zip解密后存放到/AppDex.zip，及其so文件放到payload_lib下
                Utils.releaseAppDexFile(dexdata,dexFilePath);

                LogUtil.info("解压和解密耗时 ===== "+(System.currentTimeMillis() - start) + "  === " + dexFile.exists());
            }
            // 配置动态加载环境
            //获取主线程对象
            Object currentActivityThread = getCurrentActivityThread();
            String packageName = context.getPackageName();//当前apk的包名
            LogUtil.info("packageName ===== "+packageName);
            //下面两句不是太理解
            ArrayMap mPackages = (ArrayMap) RefInvoke.getFieldOjbect(
                    "android.app.ActivityThread", currentActivityThread, "mPackages");
            LogUtil.info("反射得到的mPackages ===== "+mPackages);
            WeakReference wr = (WeakReference) mPackages.get(packageName);
            ClassLoader mClassLoader = (ClassLoader) RefInvoke.getFieldOjbect("android.app.LoadedApk", wr.get(), "mClassLoader");
            //创建被加壳apk的DexClassLoader对象  加载apk内的类和本地代码（c/c++代码）
            DexClassLoader dLoader = new DexClassLoader(dexFilePath, odexPath, context.getApplicationInfo().nativeLibraryDir, mClassLoader);
            LogUtil.info("反射得到的dLoader ===== "+dLoader);
            //base.getClassLoader(); 是不是就等同于 (ClassLoader) RefInvoke.getFieldOjbect()? 有空验证下//?
            //把当前进程的DexClassLoader 设置成了被加壳apk的DexClassLoader  ----有点c++中进程环境的意思~~
            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader", wr.get(), dLoader);

            LogUtil.info("decodeDexAndReplace ============================= 结束");
            return true;
        } catch (Exception e) {
            LogUtil.error( "error ===== "+Log.getStackTraceString(e));
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 构造原Application对象
     * @param srcApplicationClassName 原Application类名
     * @return 返回原application对象
     */
    public static Application makeApplication(String srcApplicationClassName){
        LogUtil.info( "makeApplication ============== " + srcApplicationClassName);
        if (TextUtils.isEmpty(srcApplicationClassName)){
            LogUtil.error("请配置原APK的Application ===== ");
            return null;
        }

        //调用静态方法android.app.ActivityThread.currentActivityThread获取当前activity所在的线程对象
        Object currentActivityThread = getCurrentActivityThread();
        LogUtil.info("currentActivityThread ============ "+currentActivityThread);
        //获取当前currentActivityThread的mBoundApplication属性对象，
        //该对象是一个AppBindData类对象，该类是ActivityThread的一个内部类
        Object mBoundApplication = getBoundApplication(currentActivityThread);
        LogUtil.info("mBoundApplication ============ "+mBoundApplication);
        //读取mBoundApplication中的info信息，info是LoadedApk对象
        Object loadedApkInfo = getLoadApkInfoObj(mBoundApplication);
        LogUtil.info("loadedApkInfo ============ "+loadedApkInfo);

        //先从LoadedApk中反射出mApplicationInfo变量，并设置其className为原Application的className
        //todo:注意：这里一定要设置，否则makeApplication还是壳Application对象，造成一直在attach中死循环
        ApplicationInfo mApplicationInfo = (ApplicationInfo) RefInvoke.getFieldOjbect(
                "android.app.LoadedApk", loadedApkInfo, "mApplicationInfo");
        mApplicationInfo.className = srcApplicationClassName;
        //执行 makeApplication（false,null）
        Application app = (Application) RefInvoke.invokeMethod("android.app.LoadedApk", "makeApplication", loadedApkInfo, new Class[] { boolean.class, Instrumentation.class }, new Object[] { false, null });

        LogUtil.info("makeApplication ============ app : "+app);

        //由于源码ActivityThread中handleBindApplication方法绑定Application后会调用installContentProviders，
        //此时传入的context仍为壳Application，故此处进手动安装ContentProviders，调用完成后，清空原providers
        installContentProviders(app,currentActivityThread,mBoundApplication);

        return app;
    }


    /**
     * 手动安装ContentProviders
     * @param app 原Application对象
     * @param currentActivityThread 当前ActivityThread对象
     * @param boundApplication 当前AppBindData对象
     */
    private static void installContentProviders(Application app,Object currentActivityThread,Object boundApplication){
        if (app == null) return;
        LogUtil.info("执行installContentProviders =================");
        List providers = (List) RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData",
                boundApplication, "providers");
        LogUtil.info( "反射拿到providers = " + providers);
        if (providers != null) {
            RefInvoke.invokeMethod("android.app.ActivityThread","installContentProviders",currentActivityThread,new Class[]{Context.class,List.class},new Object[]{app,providers});
            providers.clear();
        }
    }


    /**
     * Application替换并运行
     * @param app 原application对象
     */
    public static void replaceAndRunMainApplication(Application app){
        if (app == null){
            return;
        }

        LogUtil.info( "onCreate ===== 开始替换=====");
        // 如果源应用配置有Appliction对象，则替换为源应用Applicaiton，以便不影响源程序逻辑。
        final String appClassName = app.getClass().getName();

        //调用静态方法android.app.ActivityThread.currentActivityThread获取当前activity所在的线程对象
        Object currentActivityThread = getCurrentActivityThread();
        //获取当前currentActivityThread的mBoundApplication属性对象，
        //该对象是一个AppBindData类对象，该类是ActivityThread的一个内部类
        Object mBoundApplication = getBoundApplication(currentActivityThread);
        //读取mBoundApplication中的info信息，info是LoadedApk对象
        Object loadedApkInfo = getLoadApkInfoObj(mBoundApplication);
        //检测loadApkInfo是否为空
        if (loadedApkInfo == null){
            LogUtil.error( "loadedApkInfo ===== is null !!!!");
        }else {
            LogUtil.info( "loadedApkInfo ===== "+loadedApkInfo);
        }

        //把当前进程的mApplication 设置成了原application,
        RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication", loadedApkInfo, app);
        Object oldApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mInitialApplication");
        LogUtil.info( "oldApplication ===== "+oldApplication);
        ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke.getFieldOjbect(
                "android.app.ActivityThread", currentActivityThread, "mAllApplications");
        //将壳oldApplication从ActivityThread#mAllApplications列表中移除
        mAllApplications.remove(oldApplication);

        //将原Application赋值给mInitialApplication
        RefInvoke.setFieldOjbect("android.app.ActivityThread", "mInitialApplication", currentActivityThread, app);


//        ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo) RefInvoke.getFieldOjbect(
//                "android.app.LoadedApk", loadedApkInfo, "mApplicationInfo");
        ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) RefInvoke.getFieldOjbect(
                "android.app.ActivityThread$AppBindData", mBoundApplication, "appInfo");
//        appinfo_In_LoadedApk.className = appClassName;
        appinfo_In_AppBindData.className = appClassName;


        ArrayMap mProviderMap = (ArrayMap) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mProviderMap");
        Iterator it = mProviderMap.values().iterator();
        while (it.hasNext()) {
            Object providerClientRecord = it.next();
            Object localProvider = RefInvoke.getFieldOjbect("android.app.ActivityThread$ProviderClientRecord", providerClientRecord, "mLocalProvider");
            RefInvoke.setFieldOjbect("android.content.ContentProvider", "mContext", localProvider, app);
        }

        LogUtil.info( "app ===== "+app + "=====开始执行原Application");
        app.onCreate();
    }

    /**
     * 调用静态方法android.app.ActivityThread.currentActivityThread获取当前activity所在的线程对象
     * @return 当前ActivityThread对象
     */
    private static Object getCurrentActivityThread(){
        return RefInvoke.invokeStaticMethod("android.app.ActivityThread",
                "currentActivityThread", new Class[] {}, new Object[] {});
    }

    /**
     * 获取当前currentActivityThread的mBoundApplication属性对象，
     * 该对象是一个AppBindData类对象，该类是ActivityThread的一个内部类
     * @param currentActivityThread 当前ActivityThread对象
     * @return 返回AppBindData对象
     */
    private static Object getBoundApplication(Object currentActivityThread){
        if (currentActivityThread == null)
            return null;
        return RefInvoke.getFieldOjbect("android.app.ActivityThread",
                currentActivityThread, "mBoundApplication");
    }

    /**
     * 读取mBoundApplication中的info信息，info是LoadedApk对象
     * @param boundApplication AppBindData对象
     * @return LoadedApkInfo对象
     */
    private static Object getLoadApkInfoObj(Object boundApplication){
        return RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData",
                boundApplication, "info");
    }
}
