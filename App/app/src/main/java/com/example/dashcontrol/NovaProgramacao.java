package com.example.dashcontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.internal.operators.observable.ObservableRange;

public class NovaProgramacao extends AppCompatActivity {
    TextView  tv1, tv2, tempProgAgendamento;
    SeekBar seekBar;
    Button salvarProgramacao;
    int t1Hour, t1Minute, t2Hour, t2Minute;
    ListView listView;
    CheckBox cSeg, cTer, cQua, cQui, cSex, cSab, cDom;
    ArrayAdapter<String> adapter;
    ArrayList<String> ambientesSelecionados;
    ArrayList<Integer> diasSelecionados;
    private FirebaseDatabase database;
    private DatabaseReference ref;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_programacao);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.custom_bar_right, null);
        actionBar.setCustomView(view);
        database = FirebaseDatabase.getInstance();
        tv1 = findViewById(R.id.txtLigaHora);
        tv2  = findViewById(R.id.txtDesligaHora);
        cSeg = findViewById(R.id.checkBoxSeg);
        cTer = findViewById(R.id.checkBoxTer);
        cQua = findViewById(R.id.checkBoxQua);
        cQui = findViewById(R.id.checkBoxQui);
        cSex = findViewById(R.id.checkBoxSex);
        cSab = findViewById(R.id.checkBoxSab);
        cDom = findViewById(R.id.checkBoxDom);
        listView = findViewById(R.id.listViewDispositivos);
        salvarProgramacao = findViewById(R.id.buttonSalvarProgramacao);
        adapter  = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, DashWeb.getDispositivos());
        //adapter  = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, new String[]{"ambiente 1", "ambiente 2", "ambiente 3", "ambiente 4", "ambiente 5", "ambiente 6", "ambiente 7", "ambiente 8", "ambiente 9", "ambiente 10"});
//        Log.d(" Dash get dispositivos ", Integer.toString(DashWeb.getDispositivos().size()));
        listView.setAdapter(adapter);
        seekBar = findViewById(R.id.seekBarAgendamento);
        tempProgAgendamento = findViewById(R.id.tempAgendada);

        SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);

        ref = database.getReference("cliente/").child(prefs.getString("chave","null"));
        salvarProgramacao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ambientesSelecionados = new ArrayList<>();
                diasSelecionados = new ArrayList<>();
                for(int i =0; i<listView.getCount(); i++){
                    if(listView.isItemChecked(i)){
                        Log.d(" item na pos ", listView.getItemAtPosition(i).toString());
                        ambientesSelecionados.add(listView.getItemAtPosition(i).toString());
                    }
                }
                if(cSeg.isChecked()){
                    diasSelecionados.add(1);
                }
                if(cTer.isChecked()){
                    diasSelecionados.add(2);
                }
                if(cQua.isChecked()){
                    diasSelecionados.add(3);
                }
                if(cQui.isChecked()){
                    diasSelecionados.add(4);
                }
                if(cSex.isChecked()){
                    diasSelecionados.add(5);
                }
                if(cSab.isChecked()){
                    diasSelecionados.add(6);
                }
                if(cDom.isChecked()){
                    diasSelecionados.add(7);
                }
                Log.d(" leng hora ", Integer.toString(tv1.getText().toString().length()));
                if(diasSelecionados.size()<1){
                    showAlert("Aviso", "É necessário selecionar algum dia da semana!");
                    return;
                }
                if(tv1.getText().toString().length()<1){
                    showAlert("Aviso", "É necessário definir um horário para ligar!");
                    return;
                }
                if(tv2.getText().toString().length()<1){
                    showAlert("Aviso", "É necessário definir um horário para desligar!");
                    return;
                }
                SimpleDateFormat d1 = new SimpleDateFormat("HH:mm");
                try {
                    Date date1 = d1.parse(tv1.getText().toString());
                    Date date2 = d1.parse(tv2.getText().toString());
                    if(date1.getTime()>date2.getTime()){
                        showAlert("Aviso", "O horário de ligar deve ser inferior ao de desligar!");
                        return;
                    }
                } catch (Exception e) {
                }
                if(ambientesSelecionados.size()<1){
                    showAlert("Aviso", "É necessário selecionar algum ambiente!");
                    return;
                }


                JSONObject root;
                JSONObject programacoes = new JSONObject();
                JSONObject rand;
                JSONObject semanas = new JSONObject();
                JSONObject horas = new JSONObject();
                try {
                    horas.put("liga",tv1.getText().toString());
                    horas.put("desliga",tv2.getText().toString());
                    horas.put("tempPROG", tempProgAgendamento.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for(int z = 0; z < diasSelecionados.size(); z++){
                    try {
                        rand = new JSONObject();
                        rand.put(String.valueOf(System.currentTimeMillis()),horas);
                        semanas.put(diasSelecionados.get(z).toString(),rand);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                for(int i = 0; i<ambientesSelecionados.size(); i++){
                    try {
                        root = new JSONObject();
                        root.put("programacoes",semanas);
                        programacoes.put(ambientesSelecionados.get(i).toString(),root);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                Map<String, Object> jsonMap = new Gson().fromJson(programacoes.toString(), new TypeToken<HashMap<String, Object>>() {}.getType());

                Log.d(" json print ", programacoes.toString());

                Log.d(" hora selecionada ini ", tv1.getText().toString());
                Log.d(" hora selecionada out ", tv2.getText().toString());
                Log.d(" temp prog agend ", tempProgAgendamento.getText().toString());
                /*ref.updateChildren(jsonMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(NovaProgramacao.this,"Sucesso",Toast.LENGTH_LONG);
                    }
                });*/



            }
        });

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

    void showAlert(String title, String message){
        AlertDialog.Builder dialog = new AlertDialog.Builder(NovaProgramacao.this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.menu_save, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.btnSaveProgram) {
            // do something here
        }
        return super.onOptionsItemSelected(item);
    }
}