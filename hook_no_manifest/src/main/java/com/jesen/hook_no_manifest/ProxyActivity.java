package com.jesen.hook_no_manifest;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 代理Activity，已经在manifest中注册
 * 用来做替身，替那些没有manifest注册的Activity完成PMS检测
 *
 * */
public class ProxyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proxy);
    }
}