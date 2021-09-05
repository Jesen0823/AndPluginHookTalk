package com.jesen.hook_host;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

/**
 * 跳转目标Activity，没有在Activity中注册
 * 需要用hook手段才能跳转
 * */
public class DstNoManifestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dst_no_manifest);
    }
}