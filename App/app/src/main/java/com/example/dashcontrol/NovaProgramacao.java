package com.example.dashcontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

public class NovaProgramacao extends AppCompatActivity {
    TextView  tv1, tv2, tempProgAgendamento;
    SeekBar seekBar;
    int t1Hour, t1Minute, t2Hour, t2Minute;
    ListView listView;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_programacao);
        tv1 = findViewById(R.id.txtLigaHora);
        tv2  = findViewById(R.id.txtDesligaHora);
        listView = findViewById(R.id.listViewDispositivos);
        adapter  = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, DashWeb.getDispositivos());
        Log.d(" Dash get dispositivos ", Integer.toString(DashWeb.getDispositivos().size()));
        listView.setAdapter(adapter);
        seekBar = findViewById(R.id.seekBarAgendamento);
        tempProgAgendamento = findViewById(R.id.tempAgendada);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tempProgAgendamento.setText(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBar.setProgress(20);


        tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(NovaProgramacao.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        t1Hour = hourOfDay;
                        t1Minute = minute;
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(0,0,0,t1Hour,t1Minute);
                        tv1.setText(DateFormat.format("HH:mm", calendar));
                        Log.d("hora", Integer.toString(t1Hour));
                        Log.d("minuto", Integer.toString(t1Minute));
                    }
                },12,0,true);
                timePickerDialog.updateTime(t1Hour,t1Minute);
                timePickerDialog.show();
            }
        });

        tv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(NovaProgramacao.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        t2Hour = hourOfDay;
                        t2Minute = minute;
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(0,0,0,t2Hour,t2Minute);
                        tv2.setText(DateFormat.format("HH:mm", calendar));
                        Log.d("hora", Integer.toString(t2Hour));
                        Log.d("minuto", Integer.toString(t2Minute));
                    }
                },12,0,true);
                timePickerDialog.updateTime(t2Hour,t2Minute);
                timePickerDialog.show();
            }
        });

    }
}