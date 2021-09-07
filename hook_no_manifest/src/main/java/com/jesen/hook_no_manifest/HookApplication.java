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
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                hookAmsCheck_9();
                hookLaunchActivity_9();
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                hookAmsCheck_lower7();
                hookLaunchActivity();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                hookAmsCheck_Over10();
            } else { // 小于N

            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "hookAmsCheck 失败：" + e.toString());
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

    private void hookLaunchActivity_9() throws Exception {
        Field mCallbackFiled = Handler.class.getDeclaredField("mCallback");
        mCallbackFiled.setAccessible(true);

        // 获得ActivityThread对象
        Class activityThreadClass = Class.forName("android.app.ActivityThread");

        // 通过 ActivityThread 取得 H
        Field mHField = activityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);

        // 获取真正对象
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
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
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                return handleMessage_9(message);
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                return handleMessage_lower7(message,mH);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                return handleMessage_9(message);
            } else { // 小于N
                return false;
            }
        }
    }

    private boolean handleMessage_lower7(Message message,Handler mH) {
        switch (message.what) {
            case LAUNCH_ACTIVITY:
                // 把ProxyActivity 换成  DstNoManifestActivity
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

    private boolean handleMessage_9(Message msg) {
        Object mClientTransaction = msg.obj;
        /**
         * 拿到 Intent(ProxyActivity)
         *
         * final ClientTransaction transaction = (ClientTransaction) msg.obj;
         * mTransactionExecutor.execute(transaction);
         */

        try {
            Class mLaunchActivityItemClass = Class.forName("android.app.servertransaction.LaunchActivityItem");

            /**
             * LaunchActivityItem
             */
            // private List<ClientTransactionItem> mActivityCallbacks;
            Field mactivityCallbacks = mClientTransaction.getClass().getDeclaredField("mActivityCallbacks");
            mactivityCallbacks.setAccessible(true);
            List mActivityCallbacks = (List) mactivityCallbacks.get(mClientTransaction);
            if (mActivityCallbacks.size() == 0) {
                return false;
            }
            //   不一定LaunchActivityItem    Window....      // 0 Activity的启动 一定第0个
            Object mLaunchActivityItem = mActivityCallbacks.get(0);

            /**
             *  ActivityThread 会添加 ActivityResultItem 我们要区分，
             *  不是ActivityResultItem extends ClientTransactionItem，
             *  必须是LaunchActivityItem extends ClientTransaction
             * */
            if (!mLaunchActivityItemClass.isInstance(mLaunchActivityItem)) {
                return false;
            }

            Field mIntentField = mLaunchActivityItemClass.getDeclaredField("mIntent");
            mIntentField.setAccessible(true);

            /**
             * @2 LaunchActivityItem Intent mIntent   ProxyActivity LoginActivity
             */
            Intent proxyIntent = (Intent) mIntentField.get(mLaunchActivityItem);

            // 目标的Intent
            Intent targetIntent = proxyIntent.getParcelableExtra("actionIntent");
            if (targetIntent != null) {
                mIntentField.setAccessible(true);
                mIntentField.set(mLaunchActivityItem, targetIntent); // 换掉 Intent
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 10.0以及以上版本
     */
    private void hookAmsCheck_Over10() throws Exception {
        Class<?> activityTaskManagerClass = Class.forName("android.app.ActivityTaskManager");
        Field iActivityTaskManagerSingletonFiled = activityTaskManagerClass.getDeclaredField("IActivityTaskManagerSingleton");
        iActivityTaskManagerSingletonFiled.setAccessible(true);
        Object IActivityTaskManagerSingleton = iActivityTaskManagerSingletonFiled.get(null);

        Class IActivityTaskManagerClass = Class.forName("android.app.IActivityTaskManager");

        final Object iActivityTaskManager = activityTaskManagerClass.getMethod("getService").invoke(null);

        Object IActivityTaskManagerProxy = Proxy.newProxyInstance(getClassLoader()
                , new Class[]{IActivityTaskManagerClass}
                , new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] objects) throws Throwable {
                        if ("startActivity".equals(method.getName())) {
                            Intent proxyIntent = new Intent(HookApplication.this, ProxyActivity.class);
                            // 还要把本来的intent暂存下来，后面启动的时候还要用它去启动
                            proxyIntent.putExtra("actionIntent", ((Intent) objects[2]));
                            // 不仅要把代理Intent替换进去，让替身做AMS检查
                            objects[2] = proxyIntent;
                        }
                        Log.d(TAG, "拦截到了IActivityManager里面的方法" + method.getName());
                        return method.invoke(iActivityTaskManager, objects);
                    }
                });

        Class<?> singletonClass = Class.forName("android.util.Singleton");
        Field mInstanceField = singletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        mInstanceField.set(IActivityTaskManagerSingleton, IActivityTaskManagerProxy);
        Log.d(TAG, "hook activity task manager success ");
    }


    /**
     * 9.0版本代理 AMS安检
     */
    private void hookAmsCheck_9() throws Exception {

        Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
        Field iActivityManagerSingletonFiled = activityManagerClass.getDeclaredField("IActivityManagerSingleton");
        iActivityManagerSingletonFiled.setAccessible(true);
        Object IActivityManagerSingleton = iActivityManagerSingletonFiled.get(null);

        Class IActivityManagerClass = Class.forName("android.app.IActivityManager");

        final Object iActivityManager = activityManagerClass.getMethod("getService").invoke(null);

        Object IActivityManagerProxy = Proxy.newProxyInstance(getClassLoader()
                , new Class[]{IActivityManagerClass}
                , new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] objects) throws Throwable {
                        if ("startActivity".equals(method.getName())) {
                            Intent proxyIntent = new Intent(HookApplication.this, ProxyActivity.class);
                            // 还要把本来的intent暂存下来，后面启动的时候还要用它去启动
                            proxyIntent.putExtra("actionIntent", ((Intent) objects[2]));
                            // 不仅要把代理Intent替换进去，让替身做AMS检查
                            objects[2] = proxyIntent;
                        }
                        Log.d(TAG, "拦截到了IActivityManager里面的方法" + method.getName());
                        return method.invoke(iActivityManager, objects);
                    }
                });

        Class<?> singletonClass = Class.forName("android.util.Singleton");
        Field mInstanceField = singletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        mInstanceField.set(IActivityManagerSingleton, IActivityManagerProxy);
        Log.d(TAG, "hook activity task manager success ");
    }

}