package com.jesen.hook_no_manifest;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class HookApplication extends Application {
    private static final String TAG = HookApplication.class.getSimpleName();
    public static final int LAUNCH_ACTIVITY = 100;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hookAmsCheck_Over10();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {

            } else { // 小于N
                hookAmsCheck_lower7();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "hookAmsCheck 失败：" + e.toString());
        }

        try {
            hookLaunchActivity();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "hookLaunchActivity 失败：" + e.toString());
        }
    }

    /**
     * 执行 AMS之前，替换在AndroidManifest里面配置的可用Activity
     */
    private void hookAmsCheck_lower7() throws Exception {
        Class mIActivityManagerClass = Class.forName("android.app.IActivityManager");

        // 执行此方法拿到IActivityManager对象，才能让动态代理里面的 invoke 正常执行
        Class activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");

        final Object iActivityManager = activityManagerNativeClass.getMethod("getDefault").invoke(null);

        InvocationHandler invocationHandler = new InvocationHandler() {
            /**
             * @param method IActivityManager里面的方法
             * @param objects IActivityManager里面的参数
             */
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                if ("startActivity".equals(method.getName())) {
                    Intent intent = new Intent(HookApplication.this, ProxyActivity.class);
                    // 还要把本来的intent暂存下来，后面启动的时候还要用它去启动
                    intent.putExtra("actionIntent", ((Intent) objects[2]));                // 不仅要把代理Intent替换进去，让替身做AMS检查
                    objects[2] = intent;
                }
                Log.d(TAG, "拦截到了IActivityManager里面的方法" + method.getName());
                // 继续往下执行
                return method.invoke(iActivityManager, objects);
            }
        };

        Object iActivityManagerProxy = Proxy.newProxyInstance(
                HookApplication.class.getClassLoader(),
                new Class[]{mIActivityManagerClass},
                invocationHandler
        );

        // 通过 ActivityManagerNative 拿到 gDefault变量
        Field gDefaultField = activityManagerNativeClass.getDeclaredField("gDefault");
        gDefaultField.setAccessible(true);
        Object gDefault = gDefaultField.get(null);

        // 替换IActivityManager对象
        Class singletonClass = Class.forName("android.util.Singleton");
        // 获取 mInstance字段
        Field mInstanceField = singletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);

        mInstanceField.set(gDefault, iActivityManagerProxy);
    }

    private void hookLaunchActivity() throws Exception {
        Field mCallbackFiled = Handler.class.getDeclaredField("mCallback");
        mCallbackFiled.setAccessible(true);

        // 获得ActivityThread对象
        Class activityThreadClass = Class.forName("android.app.ActivityThread");
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        // 通过 ActivityThread 取得 H
        Field mHField = activityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        // 获取真正对象
        Handler mH = (Handler) mHField.get(activityThread);

        // 替换 增加自己的回调逻辑
        mCallbackFiled.set(mH, new MyCallback(mH));
    }

    class MyCallback implements Handler.Callback {

        private Handler mH;

        public MyCallback(Handler mH) {
            this.mH = mH;
        }

        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case LAUNCH_ACTIVITY:
                    // 做我们在自己的业务逻辑（把ProxyActivity 换成  DstNoManifestActivity）
                    Object obj = message.obj;
                    try {
                        // 获取之前Hook携带过来的 DstNoManifestActivity 的本来intent
                        Field intentField = obj.getClass().getDeclaredField("intent");
                        intentField.setAccessible(true);
                        // 获取 intent 对象，才能取出携带过来的 actionIntent
                        Intent intent = (Intent) intentField.get(obj);
                        // actionIntent == DstNoManifestActivity的Intent
                        Intent actionIntent = intent.getParcelableExtra("actionIntent");

                        if (actionIntent != null) {
                            intentField.set(obj, actionIntent); // 把ProxyActivity 换成  TestActivity
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
            mH.handleMessage(message);
            // 让系统继续正常往下执行
            return true;
        }
    }

    /**
     * 10.0以及以上版本
     */
    private void hookAmsCheck_Over10() throws Exception {
        Field iActivityTaskManagerFiled = null;
        Class<?> activityTaskManagerCls = Class.forName("android.app.ActivityTaskManager");
        iActivityTaskManagerFiled = activityTaskManagerCls.getDeclaredField("IActivityTaskManagerSingleton");
        iActivityTaskManagerFiled.setAccessible(true);
        Object singleton = iActivityTaskManagerFiled.get(null);
        Class<?> singletonClass = Class.forName("android.util.Singleton");
        Field mInstanceField = singletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);

        // 通过 ActivityManagerNative 拿到 gDefault变量
        Field gDefaultField = activityTaskManagerCls.getDeclaredField("IActivityTaskManagerSingleton");
        gDefaultField.setAccessible(true);
        Object gDefault = gDefaultField.get(null);

        final Object iActivityTaskManager = activityTaskManagerCls.getMethod("getDefault").invoke(null);
        Object proxy = Proxy.newProxyInstance(HookApplication.class.getClassLoader()
                , new Class[]{Class.forName("android.app.IActivityTaskManager")}
                , new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] objects) throws Throwable {
                        if ("startActivity".equals(method.getName())) {
                            Intent intent = new Intent(HookApplication.this, ProxyActivity.class);
                            // 还要把本来的intent暂存下来，后面启动的时候还要用它去启动
                            intent.putExtra("actionIntent", ((Intent) objects[2]));
                            // 不仅要把代理Intent替换进去，让替身做AMS检查
                            objects[2] = intent;
                        }
                        Log.d(TAG, "拦截到了IActivityManager里面的方法" + method.getName());
                        return method.invoke(iActivityTaskManager, objects);
                    }
                });
        mInstanceField.set(gDefault, proxy);
        Log.d(TAG, "hook activity task manager success ");
    }

}