package com.example.dashcontrol;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.json.JSONException;
import org.json.JSONObject;
import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleBiFunction;

import io.reactivex.internal.operators.observable.ObservableRange;

public class NovaProgramacao extends AppCompatActivity {
    TextView  tv1, tv2, tempProgAgendamento;
    SeekBar seekBar;
    Button salvarProgramacao;
    int t1Hour, t1Minute, t2Hour, t2Minute;
    ListView listView;
    CheckBox cSeg, cTer, cQua, cQui, cSex, cSab, cDom, cTodos;
    ArrayAdapter<String> adapter;
    ArrayList<String> ambientesSelecionados;
    ArrayList<Integer> diasSelecionados;
    private FirebaseDatabase database;
    private DatabaseReference ref;
    private List<String> conflito;
    Intent intent;
    SharedPreferences prefs;
    Handler mHandler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_programacao);
        prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.custom_bar_right, null);
        actionBar.setCustomView(view);
        database = FirebaseDatabase.getInstance();

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(NovaProgramacao.this);
                if(message.what == 1){
                    dialog.setTitle("Conflito de Horário");
                    dialog.setMessage(message.obj.toString());
                    dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                } else if(message.what == 2) {
                    dialog.setTitle("Aviso");
                    dialog.setMessage(message.obj.toString());
                    dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                }


                dialog.show();
            }
        };

        RandomStringGenerator generator = new RandomStringGenerator.Builder()
                .withinRange('0', 'Z')
                .filteredBy(LETTERS, DIGITS)
                .build();
        tv1 = findViewById(R.id.txtLigaHora);
        tv2  = findViewById(R.id.txtDesligaHora);
        cSeg = findViewById(R.id.checkBoxSeg);
        cTer = findViewById(R.id.checkBoxTer);
        cQua = findViewById(R.id.checkBoxQua);
        cQui = findViewById(R.id.checkBoxQui);
        cSex = findViewById(R.id.checkBoxSex);
        cSab = findViewById(R.id.checkBoxSab);
        cDom = findViewById(R.id.checkBoxDom);
        cTodos = findViewById(R.id.checkTodos);
        intent =  getIntent();

        listView = findViewById(R.id.listViewDispositivos);
        salvarProgramacao = findViewById(R.id.buttonSalvarProgramacao);
        if (intent.getIntExtra("op", -1) == 1) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add(prefs.getString("deviceNameForAdapter","null").toUpperCase());
            adapter  = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, arrayList);
        }else{
            adapter  = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, DashWeb.getDispositivos());
        }
        //adapter  = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, new String[]{"ambiente 1", "ambiente 2", "ambiente 3", "ambiente 4", "ambiente 5", "ambiente 6", "ambiente 7", "ambiente 8", "ambiente 9", "ambiente 10"});
//        Log.d(" Dash get dispositivos ", Integer.toString(DashWeb.getDispositivos().size()));
        listView.setAdapter(adapter);
        listView.setItemChecked(0,true);
        seekBar = findViewById(R.id.seekBarAgendamento);
        tempProgAgendamento = findViewById(R.id.tempAgendada);

        SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        cTodos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((CheckBox)v).isChecked()){
                    cSeg.setChecked(true);
                    cTer.setChecked(true);
                    cQua.setChecked(true);
                    cQui.setChecked(true);
                    cSex.setChecked(true);
                    cSab.setChecked(true);
                    cDom.setChecked(true);
                }else{
                    disableAllChecks();
                }
            }
        });

        ref = database.getReference("cliente/").child(prefs.getString("chave","null"));
        salvarProgramacao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ambientesSelecionados = new ArrayList<>();
                diasSelecionados = new ArrayList<>();
                conflito = new ArrayList<>();
                HashMap<String, Object> hrs = new HashMap<>();
                hrs.put("liga", tv1.getText().toString());
                hrs.put("desliga", tv2.getText().toString());
                hrs.put("tempPROG", tempProgAgendamento.getText().toString());

                if (intent.getIntExtra("op", -1) == 1) {
                   /* database.getReference().child("cliente").child(prefs.getString("chave","null")).child(prefs.getString("deviceNameForAdapter","null").toLowerCase()).child("R").child("programacoes").child(intent.getStringExtra("numSemana"))
                            .child(intent.getStringExtra("chaveHora")).updateChildren(hrs).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });*/


                    database.getReference().child("cliente").child(prefs.getString("chave","null")).child(prefs.getString("deviceNameForAdapter","null").toLowerCase()).child("R").child("programacoes").child(intent.getStringExtra("numSemana")).runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            if (mutableData.getValue() != null){
                                Log.d("mutable string", mutableData.toString());
                                Log.d("child count", Long.toString(mutableData.getChildrenCount()));

                                for (MutableData child : mutableData.getChildren()) {
                                    Log.d("Mutable data keey", child.getKey());
                                    Log.d("Mutable data val", child.getValue().toString());
                                    if(!child.getKey().toString().contains(intent.getStringExtra("chaveHora"))){
                                        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                                        try {
                                            Date bancoLiga = df.parse(child.child("liga").getValue().toString());
                                            Date bancoDesliga = df.parse(child.child("desliga").getValue().toString());
                                            Date vaiLigar = df.parse(tv1.getText().toString());
                                            Date vaiDesligar = df.parse(tv2.getText().toString());

                                            if (vaiLigar.after(bancoLiga) && vaiLigar.before(bancoDesliga)) {
                                                Log.d("abortou", "no primeiro");
                                                if(!conflito.contains(child.getKey())){
                                                    conflito.add(child.getKey());
                                                }
                                                //Transaction.abort();
                                                //break;
                                            }
                                            if (vaiDesligar.getTime() - 1 > bancoLiga.getTime() && vaiDesligar.getTime() - 1 < bancoDesliga.getTime()) {

                                                Log.d("abortou", "no segundo");
                                                if(!conflito.contains(child.getKey())){
                                                    conflito.add(child.getKey());
                                                }
                                            }

                                            if (vaiLigar.getTime() < bancoLiga.getTime() && vaiDesligar.getTime() > bancoDesliga.getTime()) {
                                                if(!conflito.contains(child.getKey())){
                                                    conflito.add(child.getKey());
                                                }
                                            }

                                            if (vaiDesligar.after(bancoLiga) && vaiDesligar.before(bancoDesliga)) {
                                                if(!conflito.contains(child.getKey())){
                                                    conflito.add(child.getKey());
                                                }
                                                //Transaction.abort();
                                                // break;
                                            }
                                            Log.d("passou", "no segundo");


                                        } catch (Exception e) {
                                        }

                                    }
                                }
                                Log.d("Tem conflito qtd", Integer.toString(conflito.size()));
                                if(conflito.size() == 0){
                                    mutableData.child(intent.getStringExtra("chaveHora")).setValue(hrs);
                                    Message message = mHandler.obtainMessage(2, "Dados salvos com sucesso!");
                                    message.sendToTarget();

                                }else{
                                    String va = "Não é possível atualizar esse horário pois já existe uma programação definida:\n";
                                    for(String k : conflito){
                                        va+= "Liga: "+mutableData.child(k).child("liga").getValue().toString()+ " - Desliga: "+mutableData.child(k).child("desliga").getValue().toString()+"\n";
                                    }
                                    Message message = mHandler.obtainMessage(1, va);
                                    message.sendToTarget();
                                }

                            }


                          /*  for (int i = 0; i < ambientesSelecionados.size(); i++) {
                                if (mutableData.hasChild(ambientesSelecionados.get(i).toLowerCase())) {
                                    Log.d("tem o ambinete", "selecionado");
                                    for (int z = 0; z < diasSelecionados.size(); z++) {
                                        if (mutableData.child(ambientesSelecionados.get(i).toLowerCase()).child("R").child("programacoes").hasChild(String.valueOf(diasSelecionados.get(z)))) {

                                            for (MutableData child : mutableData.child(ambientesSelecionados.get(i).toLowerCase()).child("R").child("programacoes").child(String.valueOf(diasSelecionados.get(z))).getChildren()) {
                                                Log.d("childdd", child.getKey());
                                                Log.d("liga", child.child("liga").getValue().toString());
                                                Log.d("ddesliga", child.child("desliga").getValue().toString());
                                                Log.d("temp", child.child("tempPROG").getValue().toString());

                                                SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                                                try {
                                                    Date bancoLiga = df.parse(child.child("liga").getValue().toString());
                                                    Date bancoDesliga = df.parse(child.child("desliga").getValue().toString());
                                                    Date vaiLigar = df.parse(tv1.getText().toString());
                                                    Date vaiDesligar = df.parse(tv2.getText().toString());

                                                    if (vaiLigar.after(bancoLiga) && vaiLigar.before(bancoDesliga)) {
                                                        Log.d("abortou", "no primeiro");
                                                        conflito.add(ambientesSelecionados.get(i).concat(String.valueOf(diasSelecionados.get(z))));
                                                        //Transaction.abort();
                                                        //break;
                                                    }
                                                    if (vaiDesligar.getTime() - 1 > bancoLiga.getTime() && vaiDesligar.getTime() - 1 < bancoDesliga.getTime()) {

                                                        Log.d("abortou", "no segundo");
                                                        conflito.add(ambientesSelecionados.get(i).concat(String.valueOf(diasSelecionados.get(z))));
                                                    }

                                                    if (vaiLigar.getTime() < bancoLiga.getTime() && vaiDesligar.getTime() > bancoDesliga.getTime()) {
                                                        conflito.add(ambientesSelecionados.get(i).concat(String.valueOf(diasSelecionados.get(z))));
                                                    }

                                                    if (vaiDesligar.after(bancoLiga) && vaiDesligar.before(bancoDesliga)) {

                                                        //Transaction.abort();
                                                        // break;
                                                    }
                                                    Log.d("passou", "no segundo");


                                                } catch (Exception e) {
                                                }


                                            }

                                        }
                                    }

                                } else {
                                    Log.d("Aqui da pra add", " de boias pq nao tem no banco ainda");
                                }

                            }*/

                            Log.d("conflitooo", conflito.toString());


                            if (mutableData.getChildrenCount() > 0) {
                            /*Log.d("tem o filho","do capiroto");
                            mutableData.child("PC").child("programacoes").child("10").child(UUID.randomUUID().toString()).child("liga").setValue("544562156");*/
                                for (int i = 0; i < ambientesSelecionados.size(); i++) {

                                    for (int z = 0; z < diasSelecionados.size(); z++) {
                                        if (!conflito.contains(ambientesSelecionados.get(i).concat(String.valueOf(diasSelecionados.get(z))))) {
                                            // mutableData.child(ambientesSelecionados.get(i)).child("programacoes").child(diasSelecionados.get(z).toString()).child(UUID.randomUUID().toString()).child("liga").setValue("");

                                            //mutableData.child(ambientesSelecionados.get(i).toLowerCase()).child("R").child("programacoes").child(diasSelecionados.get(z).toString()).child(UUID.randomUUID().toString()).setValue(hrs);
                                            mutableData.child(ambientesSelecionados.get(i).toLowerCase()).child("R").child("programacoes").child(diasSelecionados.get(z).toString().concat("a")).child(generator.generate(10)).setValue(hrs);
                                        }
                                    }

                                }
                            }


                            Log.d("estou retornando", "do capiroto");

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                            Log.d("on complete", currentData.toString());

                        }


                    });




                }else{
                for (int i = 0; i < listView.getCount(); i++) {
                    if (listView.isItemChecked(i)) {
                        Log.d(" item na pos ", listView.getItemAtPosition(i).toString());
                        ambientesSelecionados.add(listView.getItemAtPosition(i).toString());
                    }
                }
                if (cSeg.isChecked()) {
                    diasSelecionados.add(1);
                }
                if (cTer.isChecked()) {
                    diasSelecionados.add(2);
                }
                if (cQua.isChecked()) {
                    diasSelecionados.add(3);
                }
                if (cQui.isChecked()) {
                    diasSelecionados.add(4);
                }
                if (cSex.isChecked()) {
                    diasSelecionados.add(5);
                }
                if (cSab.isChecked()) {
                    diasSelecionados.add(6);
                }
                if (cDom.isChecked()) {
                    diasSelecionados.add(7);
                }
                Log.d(" leng hora ", Integer.toString(tv1.getText().toString().length()));
                if (diasSelecionados.size() < 1) {
                    showAlert("Aviso", "É necessário selecionar algum dia da semana!");
                    return;
                }
                if (tv1.getText().toString().length() < 1) {
                    showAlert("Aviso", "É necessário definir um horário para ligar!");
                    return;
                }
                if (tv2.getText().toString().length() < 1) {
                    showAlert("Aviso", "É necessário definir um horário para desligar!");
                    return;
                }
                SimpleDateFormat d1 = new SimpleDateFormat("HH:mm");
                try {
                    Date date1 = d1.parse(tv1.getText().toString());
                    Date date2 = d1.parse(tv2.getText().toString());
                    if (date1.getTime() > date2.getTime()) {
                        showAlert("Aviso", "O horário de ligar deve ser inferior ao de desligar!");
                        return;
                    }
                } catch (Exception e) {
                }
                if (ambientesSelecionados.size() < 1) {
                    showAlert("Aviso", "É necessário selecionar algum ambiente!");
                    return;
                }




                database.getReference("cliente/").child(prefs.getString("chave", "null")).child("/").runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        Log.d("mutable string", mutableData.toString());
                        Log.d("child count", Long.toString(mutableData.getChildrenCount()));

                        for (int i = 0; i < ambientesSelecionados.size(); i++) {
                            if (mutableData.hasChild(ambientesSelecionados.get(i).toLowerCase())) {
                                Log.d("tem o ambinete", "selecionado");
                                for (int z = 0; z < diasSelecionados.size(); z++) {
                                    if (mutableData.child(ambientesSelecionados.get(i).toLowerCase()).child("R").child("programacoes").hasChild(String.valueOf(diasSelecionados.get(z)))) {

                                        for (MutableData child : mutableData.child(ambientesSelecionados.get(i).toLowerCase()).child("R").child("programacoes").child(String.valueOf(diasSelecionados.get(z))).getChildren()) {
                                            Log.d("childdd", child.getKey());
                                            Log.d("liga", child.child("liga").getValue().toString());
                                            Log.d("ddesliga", child.child("desliga").getValue().toString());
                                            Log.d("temp", child.child("tempPROG").getValue().toString());

                                            SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                                            try {
                                                Date bancoLiga = df.parse(child.child("liga").getValue().toString());
                                                Date bancoDesliga = df.parse(child.child("desliga").getValue().toString());
                                                Date vaiLigar = df.parse(tv1.getText().toString());
                                                Date vaiDesligar = df.parse(tv2.getText().toString());

                                                if (vaiLigar.after(bancoLiga) && vaiLigar.before(bancoDesliga)) {
                                                    Log.d("abortou", "no primeiro");
                                                    conflito.add(ambientesSelecionados.get(i).concat(String.valueOf(diasSelecionados.get(z))));
                                                    //Transaction.abort();
                                                    //break;
                                                }
                                                if (vaiDesligar.getTime() - 1 > bancoLiga.getTime() && vaiDesligar.getTime() - 1 < bancoDesliga.getTime()) {

                                                    Log.d("abortou", "no segundo");
                                                    conflito.add(ambientesSelecionados.get(i).concat(String.valueOf(diasSelecionados.get(z))));
                                                }

                                                if (vaiLigar.getTime() < bancoLiga.getTime() && vaiDesligar.getTime() > bancoDesliga.getTime()) {
                                                    conflito.add(ambientesSelecionados.get(i).concat(String.valueOf(diasSelecionados.get(z))));
                                                }

                                                if (vaiDesligar.after(bancoLiga) && vaiDesligar.before(bancoDesliga)) {
                                                    conflito.add(ambientesSelecionados.get(i).concat(String.valueOf(diasSelecionados.get(z))));
                                                    //Transaction.abort();
                                                    // break;
                                                }
                                                Log.d("passou", "no segundo");


                                            } catch (Exception e) {
                                            }


                                        }

                                    }
                                }

                            } else {
                                Log.d("Aqui da pra add", " de boias pq nao tem no banco ainda");
                            }

                        }

                        Log.d("conflitooo", conflito.toString());


                        if (mutableData.getChildrenCount() > 0) {
                            /*Log.d("tem o filho","do capiroto");
                            mutableData.child("PC").child("programacoes").child("10").child(UUID.randomUUID().toString()).child("liga").setValue("544562156");*/
                            for (int i = 0; i < ambientesSelecionados.size(); i++) {

                                for (int z = 0; z < diasSelecionados.size(); z++) {
                                    if (!conflito.contains(ambientesSelecionados.get(i).concat(String.valueOf(diasSelecionados.get(z))))) {
                                        // mutableData.child(ambientesSelecionados.get(i)).child("programacoes").child(diasSelecionados.get(z).toString()).child(UUID.randomUUID().toString()).child("liga").setValue("");

                                        //mutableData.child(ambientesSelecionados.get(i).toLowerCase()).child("R").child("programacoes").child(diasSelecionados.get(z).toString()).child(UUID.randomUUID().toString()).setValue(hrs);
                                        mutableData.child(ambientesSelecionados.get(i).toLowerCase()).child("R").child("programacoes").child(diasSelecionados.get(z).toString().concat("a")).child(generator.generate(10)).setValue(hrs);
                                    }
                                }

                            }
                        }


                        Log.d("estou retornando", "do capiroto");

                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                        Log.d("on complete", currentData.toString());

                    }


                });


            }
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

        if(intent.getIntExtra("op",-1)==1){
            activateCheck(intent.getStringExtra("numSemana"));

            database.getReference().child("cliente").child(prefs.getString("chave","null")).child(prefs.getString("deviceNameForAdapter","null").toLowerCase()).child("R").child("programacoes").child(String.valueOf(intent.getStringExtra("numSemana")))
                    .child(intent.getStringExtra("chaveHora")).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d("snap deu certoo", "aleluia");
                    if(snapshot.exists()){
                        Log.d("data snapp shot", snapshot.toString());
                        tv1.setText(snapshot.child("liga").getValue().toString());
                        tv2.setText(snapshot.child("desliga").getValue().toString());
                        seekBar.setProgress(Integer.parseInt(snapshot.child("tempPROG").getValue().toString()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }

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

    private  void activateCheck(String num){
        disableAllChecks();
        if(num.contains("1a")){
            cSeg.setChecked(true);
        }else if(num.contains("2a")){
            cTer.setChecked(true);
        }else if(num.contains("3a")){
            cQua.setChecked(true);
        }else if(num.contains("4a")){
            cQui.setChecked(true);
        }else if(num.contains("5a")){
            cSex.setChecked(true);
        }else if(num.contains("6a")){
            cSab.setChecked(true);
        }else if(num.contains("7a")){
            cDom.setChecked(true);
        }

    }

    private void disableAllChecks() {
        cTodos.setEnabled(false);
        cSeg.setEnabled(false);
        cTer.setEnabled(false);
        cQua.setEnabled(false);
        cQui.setEnabled(false);
        cSex.setEnabled(false);
        cSab.setEnabled(false);
        cDom.setEnabled(false);
    }


}