package com.jesen.hook_no_manifest;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

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