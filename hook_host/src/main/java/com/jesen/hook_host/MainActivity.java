package com.jesen.hook_host;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    
    private Button startDst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startDst = findViewById(R.id.startDst);
        startDst.setOnClickListener(view -> {
            startDstActivity();
        });
    }

    private void startDstActivity() {
        startActivity(new Intent(this, DstNoManifestActivity.class));
        //startActivity(new Intent(this, Test2Activity.class));
    }
}