package com.example.dashcontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.owl93.dpb.CircularProgressView;
import com.owl93.dpb.TextFormat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.EventListener;

public class DataEspWeb extends AppCompatActivity {
    CircularProgressView temp;
    Button btnLigaDesliga, btnVerProg, btnManualWeb, btnAutoWeb;
    TextView txtLocal;
    TextView txtStatus, modoWeb;
    TextView txtTempProg;
    private static FirebaseDatabase database;
    private static DatabaseReference ref;
    private static ValueEventListener listener;
    ProgressDialog progressDialog;
    SeekBar seekBar;
    Thread t;
    Intent intent;
    SharedPreferences prefs;
    volatile int lastTempProg;
    volatile boolean lastLigaDesliga, ligaDesliga = false;
    volatile boolean modo, lastModo = false;
    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_esp_web);
        activity = this;

       androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.custom_bar, null);
        actionBar.setCustomView(view);
        seekBar = findViewById(R.id.seekBar);
        btnVerProg = findViewById(R.id.btnVerProg);
        btnAutoWeb = findViewById(R.id.btnAutoWeb);
        btnManualWeb = findViewById(R.id.btnManualWeb);
        modoWeb = findViewById(R.id.txtModoWeb);

        intent = getIntent();





        database = FirebaseDatabase.getInstance();

         prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Por favor, aguarde...");

        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        btnLigaDesliga = findViewById(R.id.bntLigaDesligaWEB);
        txtLocal = findViewById(R.id.txtLocalWEB);
        txtStatus = findViewById(R.id.txtLigaDesligaWEB);
        txtTempProg = findViewById(R.id.txtTempProgWEB);
        temp = findViewById(R.id.progessViewWEB);
        temp.setMaxValue(50);
        temp.setProgress(0);
        temp.setTextEnabled(true);
        temp.setText("...");
        txtLocal.setText(intent.getStringExtra("deviceName").toUpperCase());
        Log.d("nome do dispositivo", intent.getStringExtra("deviceName"));
        listener =  new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot device: snapshot.getChildren()) {
                    Log.d("nome valor",device.getKey().concat("  "+String.valueOf(device.getValue())));

                    if(device.getKey().equals("LINHA_1") && Boolean.valueOf(device.getValue().toString().toLowerCase())){
                        txtStatus.setText("Ligado!");
                        seekBar.setEnabled(true);
                        btnLigaDesliga.setText("Desligar");
                        lastLigaDesliga = true;
                    }else if(device.getKey().equals("LINHA_1") && !Boolean.valueOf(device.getValue().toString().toLowerCase())){
                        txtStatus.setText("Desligado!");
                        btnLigaDesliga.setText("Ligar");
                        seekBar.setEnabled(false);
                        lastLigaDesliga = false;
                    }else if(device.getKey().equals("tempATUAL")){
                        temp.animateProgressChange(Float.valueOf(device.getValue().toString()),1000);
                        temp.setText(String.format("%.1f",Float.valueOf(device.getValue().toString())).replace(",",".").concat(" ºC"));

                    }else if(device.getKey().equals("tempPROG")){
                        txtTempProg.setText(String.valueOf(Math.round(Double.valueOf(device.getValue().toString()))));
                        seekBar.setProgress(Integer.parseInt(device.getValue().toString()));
                        lastTempProg = Integer.parseInt(device.getValue().toString());
                    }else if(device.getKey().equals("auto") && Boolean.valueOf(device.getValue().toString().toLowerCase())){
                        btnManualWeb.setEnabled(true);
                        btnAutoWeb.setEnabled(false);
                        btnLigaDesliga.setEnabled(false);
                        seekBar.setEnabled(false);
                        modoWeb.setText("Automático");
                        lastModo = true;
                    }else if(device.getKey().equals("auto") && !Boolean.valueOf(device.getValue().toString().toLowerCase())){
                        btnManualWeb.setEnabled(false);
                        btnAutoWeb.setEnabled(true);
                        seekBar.setEnabled(true);
                        btnLigaDesliga.setEnabled(true);
                        modoWeb.setText("Manual");
                        lastModo=false;
                    }


                }
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        ref = database.getReference("cliente/".concat(prefs.getString("chave","null")).concat("/").concat(intent.getStringExtra("deviceName").toLowerCase()).concat("/W"));
        Log.d("Caminho do cliente","cliente/".concat(prefs.getString("chave","null")).concat("/").concat(intent.getStringExtra("deviceName")).concat("/W") );


        btnLigaDesliga.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnLigaDesliga.getText().toString().toLowerCase().equals("desligar")){
                    sendDataEsp("LINHA_1",false, null);
                    ligaDesliga = false;
                    lastLigaDesliga = true;

                }else{
                    sendDataEsp("LINHA_1",true, null);
                    lastLigaDesliga = false;
                    ligaDesliga = true;
                }
            }
        });

        btnManualWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastModo = true;
                modo = false;
                sendDataEsp("auto",false, null);
            }
        });

        btnAutoWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastModo = false;
                modo = true;
                sendDataEsp("auto", true, null);

            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtTempProg.setText(Integer.toString(progress));

            };

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                lastTempProg = Integer.parseInt(txtTempProg.getText().toString());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                progressDialog.setMessage("Definindo temperatura, aguarde...");
                sendDataEsp("tempPROG",Float.parseFloat(txtTempProg.getText().toString()), null);
            }
        });

        /*btnMais.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                database.getReference("cliente/".concat(prefs.getString("chave","null")).concat("/").concat(intent.getStringExtra("deviceName")).concat("/R/tempPROG")).setValue(Integer.valueOf(txtTempProg.getText().toString())+1).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

            }
        });

        btnMenos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                database.getReference("cliente/".concat(prefs.getString("chave","null")).concat("/").concat(intent.getStringExtra("deviceName")).concat("/R/tempPROG")).setValue(Integer.valueOf(txtTempProg.getText().toString())-1).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

            }
        });*/

        btnVerProg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i =  new Intent(DataEspWeb.this, VerProgramacoesWeb.class);
                i.putExtra("deviceName", txtLocal.getText().toString());
                startActivity(i);
            }
        });
    }

    private void sendDataEsp(String nodo, Object object, String src) {
        ProgressDialog dial = new ProgressDialog(this);
        dial.setMessage("Por favor aguarde, estamos contatanto o dispositivo...");
        dial.setCanceledOnTouchOutside(false);
        dial.show();

        database.getReference().child("cliente").child(prefs.getString("chave","null")).child(intent.getStringExtra("deviceName")).child("R").child("info").child(nodo).setValue(object).addOnSuccessListener(new OnSuccessListener<Void>() {

            volatile int time = 0;
            volatile int timeout = 150;//15 segundos
            @Override
            public void onSuccess(Void aVoid) {
                    t = new Thread() {
                        public void run() {
                            Log.i("Last temp progh", String.valueOf(lastTempProg));
                            Log.i("temp prog", String.valueOf(txtTempProg.getText().toString()));
                            Log.i("liga desliga", String.valueOf(ligaDesliga));
                            Log.i("last liga desliga", String.valueOf(lastLigaDesliga));
                            Log.i("modo", String.valueOf(modo));
                            Log.i("last modo", String.valueOf(lastModo));
                            while (lastTempProg != Integer.parseInt(txtTempProg.getText().toString()) || lastLigaDesliga != ligaDesliga || lastModo != modo){
                                if(time > timeout){
                                    activity.runOnUiThread(new Runnable()
                                    {
                                        public void run()
                                        {
                                            AlertDialog.Builder dialog = new AlertDialog.Builder(DataEspWeb.this);
                                            dialog.setTitle("Aviso");
                                            dialog.setMessage("Parece que o dispositivo não respondeu a tempo. Verfique a conexão!");
                                            dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                            dialog.create().show();
                                        }
                                    });

                                    break;
                                }
                                try {
                                    Thread.sleep(100);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                time++;
                            }
                            dial.dismiss();
                            this.interrupt();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                //dial.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dial.dismiss();
                message("Houve um erro, não foi possível contatar o dispositivo!");
            }
        });
    }

    private void message(String msg) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(DataEspWeb.this);
        dialog.setTitle("Aviso");
        dialog.setMessage(msg);
        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(listener!=null){
            ref.removeEventListener(listener);
            Log.d("removi o listener", "verdade");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(temp != null){
            temp.invalidate();
            temp.postInvalidate();
            temp.refreshDrawableState();
        }
        if(listener!=null){
            Log.d("adicionei o listener", "verdade");
            ref.addValueEventListener(listener);
        }

    }
}