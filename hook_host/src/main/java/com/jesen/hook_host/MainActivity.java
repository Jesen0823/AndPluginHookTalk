package com.jesen.hook_host;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    
    private Button  startActivity,startPlugin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        startActivity = findViewById(R.id.startActivity);
        startActivity.setOnClickListener(view -> {
            startActivityAnother();
        });

        startPlugin = findViewById(R.id.startPlugin);
        startPlugin.setOnClickListener(view -> startPluginActivity());
    }

    // 启动宿主普通Activity另一种写法
    private void startActivityAnother(){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.jesen.hook_host","com.jesen.hook_host.AnotherActivity"));
        startActivity(intent);
    }

    // 启动插件Activity
    private void startPluginActivity(){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.jesen.hook_plugin","com.jesen.hook_plugin.PluginActivity"));
        startActivity(intent);
    }
}