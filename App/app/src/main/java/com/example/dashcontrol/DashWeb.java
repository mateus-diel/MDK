package com.example.dashcontrol;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DashWeb extends AppCompatActivity {

    private FirebaseDatabase database;
    private DatabaseReference ref, ref1;
    private long offset = 60 * 5;
    public static ArrayList<String> names;
    public static  ArrayList <Drawable> draw;
    private static ArrayList<String> dispositivos;
    static GridView gridView;
    GridAdapter adapter;
    SharedPreferences prefs;
    FloatingActionButton prog, sair, contato, modoViagem, personalizarIcones;
    FloatingActionMenu floatingMenu;


    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_web);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.custom_bar, null);
        actionBar.setCustomView(view);
        database = FirebaseDatabase.getInstance();
        gridView = findViewById(R.id.grid_view_dash_web);
        sair = findViewById(R.id.floatingSairWeb);
        contato = findViewById(R.id.floatingSupportWeb);
        modoViagem = findViewById(R.id.floatingModoViagemWeb);
        personalizarIcones = findViewById(R.id.floatingPersonalizarWeb);
        dispositivos = new ArrayList<>();
        names = new ArrayList<>();
        draw = new ArrayList<>();
        prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        prog = findViewById(R.id.floatingProgramaçõesWeb);
        floatingMenu = findViewById(R.id.floatingMenuWeb);

        prog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floatingMenu.close(true);
                Intent i = new Intent(getApplicationContext(), NovaProgramacao.class);
                startActivity(i);
            }
        });

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), DataEspWeb.class);
                intent.putExtra("deviceName", names.get(position).toLowerCase());
                startActivity(intent);
            }
        });

        sair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("autoLogin", false);
                editor.apply();
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        personalizarIcones.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floatingMenu.close(true);
                Intent a = new Intent(getApplicationContext(), PersonalizarIcones.class);
                startActivity(a);
            }
        });



        SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
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
                                for (DataSnapshot device: snapshot.getChildren()) {
                                    Drawable unwrappedDrawable;
                                    Drawable wrappedDrawable;
                                    unwrappedDrawable = AppCompatResources.getDrawable(DashWeb.this, R.drawable.ic_home_iconuserselect);
                                    wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);

                                    Log.d(" o caminho do cara e ", prefs.getString(prefs.getString("email","null").concat(device.getKey().concat("/IconUser")),"null"));

                                    if(!prefs.getString(prefs.getString("email","null").concat(device.getKey().concat("/IconUser")),"null").equals("null")){
                                        Log.d(" o cara tem icone ", "null");

                                        Field[] drawablesFields = com.example.dashcontrol.R.drawable.class.getFields();

                                        for (Field field : drawablesFields) {
                                            try {
                                                Log.i("LOG_TAG", "my drawww." + field.getName());
                                                if (field.getName().contains(prefs.getString(prefs.getString("email","null").concat(device.getKey().concat("/IconUser")),"null"))) {
                                                    Log.d("É esse aquii", field.getName());

                                                     unwrappedDrawable = AppCompatResources.getDrawable(DashWeb.this, field.getInt(null));
                                                     wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }



                                    if ((Math.abs(Long.valueOf(device.child("W").child("Timestamp").getValue().toString())-System.currentTimeMillis())/1000)<offset) {

                                        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(DashWeb.this, R.color.laranjalogo));
                                        names.add(device.getKey().toUpperCase());
                                        draw.add(wrappedDrawable);
                                        dispositivos.add(device.getKey().toUpperCase());

                                    } else {
                                        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(DashWeb.this, R.color.azulonline));
                                        //DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(DashWeb.this, R.color.azulonline));
                                        names.add(device.getKey().toUpperCase());
                                        draw.add(wrappedDrawable);
                                        dispositivos.add(device.getKey().toUpperCase());
                                    }
                                }
                                if(snapshot.exists()){
                                    Log.d("snapppp", snapshot.toString());
                                }
                                ref1.removeEventListener(this);
                                adapter = new GridAdapter(DashWeb.this, names,draw);
                                gridView.setAdapter(adapter);
                                Log.d("tamanghoo",Integer.toString(draw.size()));
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





    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(draw != null){
            if(draw.size()>0){

            }
        }
    }



    public static ArrayList<String> getDispositivos() {
        return dispositivos;
    }
}