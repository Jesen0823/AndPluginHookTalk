package com.jesen.hook_host;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.jesen.hook_host.utils.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class HookApplication extends Application {
    private static final String TAG = HookApplication.class.getSimpleName();

    private Resources resources;
    private AssetManager assetManager;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            pluginDexToHost();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "hookLaunchActivity 失败：" + e.toString());
        }
    }

    private void pluginDexToHost() throws Exception {
        /**
         * 1. 找到宿主的dexElements
         * */
        // 本质就是PathClassLoader
        PathClassLoader pathClassLoader = (PathClassLoader) this.getClassLoader();
        Class mBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        // private final DexPathList pathList;
        Field pathListField = mBaseDexClassLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object mDexPathList = pathListField.get(pathClassLoader);

        Field dexElementsField = mDexPathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        // 拿到 Element[] dexElements
        Object dexElements = dexElementsField.get(mDexPathList);

        /**
         * 2.找到插件的dexElements，要用DexClassLoader
         * */
        File file = FileUtil.getPluginPath(this);
        if (!file.exists()) {
            throw new FileNotFoundException("没有找到插件包!!");
        }
        String pluginPath = file.getAbsolutePath();
        // 缓冲路径 data/data/包名/pluginDir/
        File fileDir = this.getDir("pluginDir", Context.MODE_PRIVATE);
        DexClassLoader dexClassLoader = new
                DexClassLoader(pluginPath, fileDir.getAbsolutePath(), null, getClassLoader());

        Class mBaseDexClassLoaderClassPlugin = Class.forName("dalvik.system.BaseDexClassLoader");
        // private final DexPathList pathList;
        Field pathListFieldPlugin = mBaseDexClassLoaderClassPlugin.getDeclaredField("pathList");
        pathListFieldPlugin.setAccessible(true);
        Object mDexPathListPlugin = pathListFieldPlugin.get(dexClassLoader);

        Field dexElementsFieldPlugin = mDexPathListPlugin.getClass().getDeclaredField("dexElements");
        dexElementsFieldPlugin.setAccessible(true);
        // 本质就是 Element[] dexElements
        Object dexElementsPlugin = dexElementsFieldPlugin.get(mDexPathListPlugin);

        /**
         * 3.创建新的dexElements，将两者融合
         * */
        int hostDexLen = Array.getLength(dexElements);
        int pluginDexLen = Array.getLength(dexElementsPlugin);
        int newDexLen = hostDexLen + pluginDexLen;
        // 创建数组，指定数组类型和长度
        Object newDexElements = Array.newInstance(dexElements.getClass().getComponentType(), newDexLen);

        /*for (int i = 0; i < newDexLen; i++) {
            // 先融合宿主
            if (i < hostDexLen) {
                // 参数一：新要融合的容器 -- newDexElements
                Array.set(newDexElements, i, Array.get(dexElements, i));
            } else { // 再融合插件的
                Array.set(newDexElements, i, Array.get(dexElementsPlugin, i - hostDexLen));
            }
        }*/

        // 将dexElements从下标0开始拷贝到总数组
        System.arraycopy(dexElements, 0, newDexElements, 0, hostDexLen);
        // 将dexElementsPlugin从下标hostDexLen开始拷贝到总数组
        System.arraycopy(dexElementsPlugin, 0, newDexElements, hostDexLen, pluginDexLen);

        /**
         * 4.新的dexElements设置给宿主
         * */
        dexElementsField.set(mDexPathList, newDexElements);
        // 处理加载插件中的布局
        doPluginLayoutLoad();
    }

    /**
     * 加载插件中的布局
     */
    private void doPluginLayoutLoad() throws Exception {
        assetManager = AssetManager.class.newInstance();

        // 把插件路径给assertManager
        File file = FileUtil.getPluginPath(this);
        if (!file.exists()) {
            throw new FileNotFoundException("没有找到插件包!!");
        }
        // 执行此 addAssetPath(String path) 方法，把插件的路径添加进去
        Method method = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
        method.setAccessible(true);
        method.invoke(assetManager, file.getAbsolutePath());

        // 宿主的资源配置信息
        Resources r = getResources();
        // 实例化此方法 final StringBlock[] ensureStringBlocks()
        Method ensureStringBlocksMethod = assetManager.getClass().getDeclaredMethod("ensureStringBlocks");
        ensureStringBlocksMethod.setAccessible(true);
        // 执行了ensureStringBlocks  string.xml  color.xml会被初始化
        ensureStringBlocksMethod.invoke(assetManager);

        // 10.0
        /*Method getApkAssetsMethod = assetManager.getClass().getDeclaredMethod("build");
        getApkAssetsMethod.setAccessible(true);
        getApkAssetsMethod.invoke(assetManager);*/

        // 特殊Resource,专门用来加载插件资源
        resources = new Resources(assetManager, r.getDisplayMetrics(), r.getConfiguration());
    }

    @Override
    public Resources getResources() {
        return resources == null ? super.getResources() : resources;
    }

    @Override
    public AssetManager getAssets() {
        return assetManager == null ? super.getAssets() : assetManager;
    }
}