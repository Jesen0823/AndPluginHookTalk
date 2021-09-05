package com.jesen.hook_plugin;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Resources;
/**
 * 覆写方法，ressources和assert都用宿主的，因为插件的dex已经以apk形式给了宿主
 * */
public class BaseActivity extends Activity {

    @Override
    public Resources getResources() {
        // getApplication()是宿主的
        if (getApplication() != null && getApplication().getResources()!=null){
            return getApplication().getResources();
        }
        return super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if (getApplication() != null && getApplication().getAssets()!=null){
            return getApplication().getAssets();
        }
        return super.getAssets();
    }
}
