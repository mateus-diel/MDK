package com.example.dashcontrol;

import android.app.ProgressDialog;
import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.EventListener;

public class DataEspWeb extends AppCompatActivity {
    CircularProgressView temp;
    Button btnMenos;
    Button btnMais;
    Button btnLigaDesliga;
    TextView txtLocal;
    TextView txtStatus;
    TextView txtTempProg;
    private FirebaseDatabase database;
    private DatabaseReference ref;
    private ValueEventListener listener;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_esp_web);

        database = FirebaseDatabase.getInstance();

        SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Por favor, aguarde...");

        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        btnMenos = findViewById(R.id.btnMenosWEB);
        btnMais = findViewById(R.id.btnMaisWEB);
        btnLigaDesliga = findViewById(R.id.bntLigaDesligaWEB);
        txtLocal = findViewById(R.id.txtLocalWEB);
        txtStatus = findViewById(R.id.txtLigaDesligaWEB);
        txtTempProg = findViewById(R.id.txtTempProgWEB);
        temp = findViewById(R.id.progessViewWEB);
        temp.setMaxValue(50);
        temp.setProgress(0);
        temp.setTextEnabled(true);
        temp.setText("...");
        Intent intent = getIntent();
        txtLocal.setText(intent.getStringExtra("deviceName").toUpperCase());
        Log.d("nome do dispositivo", intent.getStringExtra("deviceName"));
        listener =  new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot device: snapshot.getChildren()) {
                    Log.d("nome",device.getKey());
                    Log.d("valor",String.valueOf(device.getValue()));

                    if(device.getKey().equals("LINHA_1") && Boolean.valueOf(device.getValue().toString().toLowerCase())){
                        txtStatus.setText("Ligado!");
                        btnLigaDesliga.setText("Desligar");
                    }else if(device.getKey().equals("LINHA_1") && !Boolean.valueOf(device.getValue().toString().toLowerCase())){
                        txtStatus.setText("Desligado!");
                        btnLigaDesliga.setText("Ligar");
                    }else if(device.getKey().equals("tempATUAL")){
                        temp.animateProgressChange(Float.valueOf(device.getValue().toString()),1000);
                        temp.setText(String.format("%.1f",Float.valueOf(device.getValue().toString())).replace(",",".").concat(" ºC"));
                    }else if(device.getKey().equals("tempPROG")){
                        txtTempProg.setText(String.valueOf(Math.round(Double.valueOf(device.getValue().toString()))));
                    }


                }
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        ref = database.getReference("cliente/".concat(prefs.getString("chave","null")).concat("/").concat(intent.getStringExtra("deviceName")).concat("/W"));
        Log.d("Caminho do cliente","cliente/".concat(prefs.getString("chave","null")).concat("/").concat(intent.getStringExtra("deviceName")).concat("/W") );
        ref.addValueEventListener(listener);

        btnLigaDesliga.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnLigaDesliga.getText().toString().toLowerCase().equals("desligar")){
                    database.getReference("cliente/".concat(prefs.getString("chave","null")).concat("/").concat(intent.getStringExtra("deviceName")).concat("/R/LINHA_1")).setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });

                }else{
                    database.getReference("cliente/".concat(prefs.getString("chave","null")).concat("/").concat(intent.getStringExtra("deviceName")).concat("/R/LINHA_1")).setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });
                }
            }
        });

        btnMais.setOnClickListener(new View.OnClickListener() {
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
        });
    }

    @Override
    protected void onPause() {
        if(listener!=null){
            ref.removeEventListener(listener);
        }
        super.onPause();
    }
}