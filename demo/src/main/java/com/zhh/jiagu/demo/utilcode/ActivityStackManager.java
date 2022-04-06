package com.zhh.jiagu.demo.utilcode;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * Activity堆栈管理
 *
 * @author ZengCS
 * @since 2014年8月16日
 */
public class ActivityStackManager {
    private static final String TAG = "ActivityStackManager";
    private static ActivityStackManager mSingleInstance;
    private Stack<Activity> mActivityStack;

    private ActivityStackManager() {
        mActivityStack = new Stack<Activity>();
    }

    /**
     * 获取单例对象
     */
    public static ActivityStackManager getInstance() {
        if (null == mSingleInstance) {
            mSingleInstance = new ActivityStackManager();
        }
        return mSingleInstance;
    }

    public boolean isEmpty() {
        if (null == mActivityStack || mActivityStack.size() == 0) {
            return true;
        }
        return false;
    }

    public Stack<Activity> getStack() {
        return mActivityStack;
    }

    /**
     * 入栈
     */
    public void addActivity(Activity activity) {
        mActivityStack.push(activity);
    }

    /**
     * 出栈
     */
    public void removeActivity(Activity activity) {
        mActivityStack.remove(activity);
    }

    /**
     * 获取当前的Activity(最后一个入栈的Activity)
     */
    public Activity getCurrentActivity() {
        return mActivityStack.lastElement();
    }

    /**
     * 结束当前Activity（最后一个入栈的Activity）
     */
    public void finishCurrentActivity() {
        Activity activity = mActivityStack.lastElement();
        if (activity != null) {
            activity.finish();
        }
    }

    /**
     * 结束所有Activity
     */
    public void finishAllActivity() {
        Log.d(TAG, "结束所有Activity");
        Activity activity;
        while (!mActivityStack.empty()) {
            activity = mActivityStack.pop();
            if (activity != null)
                activity.finish();
        }
    }

    /**
     * 结束所有Activity
     */
    public void finishAllActivity(Class<? extends Activity> actCls) {
        Log.d(TAG, "结束所有Activity");
        Activity activity;
        while (!mActivityStack.empty()) {
            activity = mActivityStack.pop();
            if (null != activity && !activity.getClass().getName().equals(actCls.getName()))
                activity.finish();
        }
    }

    /**
     * 结束指定的Activity
     */
    public boolean finishActivity(Activity activity) {
        try {
            if (null != activity) {
                if(!activity.isFinishing()){
                    activity.finish();
                }
                mActivityStack.remove(activity);
                return true;
            }
        } catch (Exception e) {
            Log.e("======Exception==", "======" + e.toString());
        }
        return false;
    }

    public boolean isExistMainActivity(Class<? extends Activity> actCls) {
        Activity tempActivity = null;
        Iterator<Activity> itr = mActivityStack.iterator();
        while (itr.hasNext()) {
            tempActivity = itr.next();
            if (null != tempActivity && tempActivity.getClass().getName().equals(actCls.getName()) && !tempActivity.isFinishing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 结束指定的Activity
     */
    public boolean finishActivity(Class<? extends Activity> actCls) {
        Activity activity = findActivityByClass(actCls);
        if (null != activity && !activity.isFinishing()) {
            activity.finish();
            return true;
        }
        return false;
    }

    /**
     * 根据类名查找Activity
     */
    public Activity findActivityByClass(Class<? extends Activity> actCls) {
        Activity tempActivity = null;
        Iterator<Activity> itr = mActivityStack.iterator();
        while (itr.hasNext()) {
            tempActivity = itr.next();
            if (null != tempActivity && tempActivity.getClass().getName().equals(actCls.getName()) && !tempActivity.isFinishing()) {
                break;
            }
            tempActivity = null;
        }
        return tempActivity;
    }

    /**
     * finish指定的Activity之上的所有Activity
     */
    public boolean finishToActivity(Class<? extends Activity> actCls, boolean isIncludeSelf) {
        List<Activity> buf = new ArrayList<Activity>();
        int size = mActivityStack.size();
        Activity activity;
        for (int i = size - 1; i >= 0; i--) {
            activity = mActivityStack.get(i);
            if (activity.getClass().isAssignableFrom(actCls)) {
                for (Activity a : buf) {
                    a.finish();
                }
                return true;
            } else if (i == size - 1 && isIncludeSelf) {
                buf.add(activity);
            } else if (i != size - 1) {
                buf.add(activity);
            }
        }
        return false;
    }

    /**
     * 退出应用程序
     */
    public void exitApp() {
        Log.d(TAG, "退出应用程序");
        finishAllActivity();
//        android.os.Process.killProcess(android.os.Process.myPid());
//        System.gc();
    }


    /**
     * 指定的activity实例是否存活
     *
     * @param actCls
     * @return
     */
    public boolean activityInstanceIsLive(Class<? extends Activity> actCls) {
        if (mActivityStack == null) {
            Log.w("ActivityStackManager","mActivityStack == null when activityInstanceIsLive");
            return false;
        }
        return mActivityStack.contains(findActivityByClass(actCls));
    }
}
