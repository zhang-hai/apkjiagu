package com.zhh.jiagu.shell;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.zhh.jiagu.shell.dex.LoadDexUtil;
import com.zhh.jiagu.shell.util.AESUtil;
import com.zhh.jiagu.shell.util.LogUtil;

public class StubApplication extends Application {

    private static final String APP_KEY = "APPLICATION_CLASS_NAME";

    private Application app;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        AESUtil.loadJiaGuLibrary();

        //加载dex，并解密出原app的dex文件进行加载
        boolean result = LoadDexUtil.decodeDexAndReplace(this,getAppVersionCode());

        if (result){
            //生成原Application，并手动安装ContentProviders
            app = LoadDexUtil.makeApplication(getSrcApplicationClassName());
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        //create main Apk's Application and replace with it.
        LoadDexUtil.replaceAndRunMainApplication(app);
    }

    private int getAppVersionCode(){
        PackageInfo info = getPackageInfo();
        return info == null ? 0:info.versionCode;
    }

    private PackageInfo getPackageInfo() {
        PackageInfo pi = null;
        try {
            pi = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS);
            return pi;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pi;
    }

    /**
     * 获取原application的类名
     * @return 返回类名
     */
    private String getSrcApplicationClassName(){
        try {
            ApplicationInfo ai = this.getPackageManager()
                    .getApplicationInfo(this.getPackageName(),
                            PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            if (bundle != null && bundle.containsKey(APP_KEY)) {
                return bundle.getString(APP_KEY);//className 是配置在xml文件中的。
            } else {
                LogUtil.info( "have no application class name");
                return "";
            }
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.error("error:" + Log.getStackTraceString(e));
            e.printStackTrace();
        }
        return "";
    }




    //以下是加载资源
    protected AssetManager mAssetManager;//资源管理器
    protected Resources mResources;//资源
    protected Resources.Theme mTheme;//主题
/*

    protected void loadResources(String dexPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, dexPath);
            mAssetManager = assetManager;
        } catch (Exception e) {
            Log.i("inject", "loadResource error:"+Log.getStackTraceString(e));
            e.printStackTrace();
        }
        Resources superRes = super.getResources();
        superRes.getDisplayMetrics();
        superRes.getConfiguration();
        mResources = new Resources(mAssetManager, superRes.getDisplayMetrics(),superRes.getConfiguration());
        mTheme = mResources.newTheme();
        mTheme.setTo(super.getTheme());
    }
*/

    @Override
    public AssetManager getAssets() {
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        return mResources == null ? super.getResources() : mResources;
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }


}
