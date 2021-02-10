package com.example.dashcontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.owl93.dpb.CircularProgressView;

public class SetDataEsp extends AppCompatActivity {
    CircularProgressView temp;
    Button test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_data_esp);
        test = findViewById(R.id.btntest);
        temp = findViewById(R.id.progessView);
        temp.setMaxValue(50);
        temp.setProgress(0);
        temp.setTextEnabled(true);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                temp.animateProgressChange(temp.getProgress()+10,1000);
                temp.setText(String.valueOf(temp.getProgress()).concat(" ºC"));


            }
        });


    }
}