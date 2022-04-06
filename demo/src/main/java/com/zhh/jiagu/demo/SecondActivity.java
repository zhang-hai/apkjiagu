package com.zhh.jiagu.demo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Button button = findViewById(R.id.btn_button);
        button.setOnClickListener(v->{
            Toast.makeText(SecondActivity.this,"点击了按钮",Toast.LENGTH_SHORT).show();
        });
    }
}