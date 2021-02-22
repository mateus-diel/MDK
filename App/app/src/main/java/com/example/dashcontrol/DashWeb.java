package com.example.dashcontrol;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DashWeb extends AppCompatActivity {
    private FirebaseDatabase database;
    private DatabaseReference ref, ref1;
    GridLayout grid;
    private long offset = 60*10;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_web);
        database = FirebaseDatabase.getInstance();

        SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        grid = findViewById(R.id.gridLayoutforWebESP);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Por favor, aguarde...");

        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        Log.d("email",prefs.getString("email","null"));
        Log.d("uuid", prefs.getString("chave","null"));
        AlertDialog.Builder passResetDialog = new AlertDialog.Builder(this);
        passResetDialog.setTitle("Aviso");
        passResetDialog.setCancelable(false);
        passResetDialog.setMessage("Sistema não está registrado, entre em contato para maiores informações!");
        passResetDialog.setPositiveButton("Sair", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(DashWeb.this, Login.class);
                startActivity(intent);
                finish();
            }
        });

        ref = database.getReference("chaves/".concat(prefs.getString("chave","null")).concat("/ativo"));

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    Log.d("snapshot",snapshot.getValue().toString());
                    if(!Boolean.valueOf(snapshot.getValue().toString().toLowerCase())){
                        passResetDialog.create().show();
                    }else{
                        Log.d("caminhooo","cliente/".concat(prefs.getString("chave","null")));
                        ref1 = database.getReference("cliente/".concat(prefs.getString("chave","null")));
                        ref1.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                grid.removeAllViews();
                                Log.d("snappppppppppppp", snapshot.child("marcoa").child("W").child("Timestamp").getValue().toString());
                                for (DataSnapshot device: snapshot.getChildren()) {
                                    Log.d("deviceee", device.child("W").child("Timestamp").getValue().toString());
                                    Log.d("sysstem",String.valueOf(System.currentTimeMillis()));

                                    if ((Math.abs(Long.valueOf(device.child("W").child("Timestamp").getValue().toString())-System.currentTimeMillis())/1000)<offset){
                                        Log.d("Se","tyrue");
                                        Point size = new Point();
                                        Button z = new Button(getApplicationContext());
                                        z.setMinHeight(200);
                                        z.setMinWidth((size.x-50)/3);
                                        z.setText(device.getKey());
                                        z.setBackgroundColor(Color.GREEN);
                                        z.setTag(device.getKey());
                                        z.setOnClickListener(DashWeb.this::onClick);
                                        grid.addView(z);
                                    } else {
                                        Point size = new Point();
                                        Button z = new Button(getApplicationContext());
                                        z.setMinHeight(200);
                                        z.setMinWidth((size.x-50)/3);
                                        z.setText(device.getKey());
                                        z.setBackgroundColor(Color.RED);
                                        z.setTag(device.getKey());
                                        z.setOnClickListener(DashWeb.this::onClick);
                                        grid.addView(z);
                                        Log.d("Se","false");
                                    }
                                }
                                if(snapshot.exists()){
                                    Log.d("snapppp", snapshot.toString());
                                }
                                ref1.removeEventListener(this);
                                progressDialog.dismiss();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.d("snapppp", "canceleddd");
                            }
                        });
                    }

                }
                ref.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }

        });
    }

    private void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), DataEspWeb.class);
        intent.putExtra("deviceName", v.getTag().toString());
        startActivity(intent);
    }
}