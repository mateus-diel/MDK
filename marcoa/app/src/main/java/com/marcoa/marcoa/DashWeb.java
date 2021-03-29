package com.marcoa.marcoa;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
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

public class DashWeb extends AppCompatActivity {

    private FirebaseDatabase database;
    private DatabaseReference ref, ref1;
    private long offset = 60 * 5;
    public static ArrayList<String> names;
    public static ArrayList<Drawable> draw;
    private static ArrayList<String> dispositivos;
    static GridView gridView;
    Handler mHandler;
    static boolean modoViagemAtivo = false;
    public static GridAdapter adapter;
    SharedPreferences prefs;
    FloatingActionButton prog, sair, contato, modoViagem, personalizarIcones, usuarios;
    FloatingActionMenu floatingMenu;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_web);
        //androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        //actionBar.setDisplayShowCustomEnabled(true);
        //LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //View view = inflater.inflate(R.layout.custom_bar, null);
        //actionBar.setCustomView(view);
        database = FirebaseDatabase.getInstance();
        gridView = findViewById(R.id.grid_view_dash_web);
        sair = findViewById(R.id.floatingSairWeb);
        contato = findViewById(R.id.floatingSupportWeb);
        usuarios = findViewById(R.id.floatingUsuariosWeb);
        modoViagem = findViewById(R.id.floatingModoViagemWeb);
        personalizarIcones = findViewById(R.id.floatingPersonalizarWeb);
        dispositivos = new ArrayList<>();
        names = new ArrayList<>();
        draw = new ArrayList<>();
        prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        prog = findViewById(R.id.floatingProgramaçõesWeb);
        floatingMenu = findViewById(R.id.floatingMenuWeb);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(DashWeb.this);
                if (message.what == 1) {
                    dialog.setTitle("Aviso!");
                    dialog.setMessage(message.obj.toString());
                    dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                }
                dialog.create().show();
            }
        };

        modoViagem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder passResetDialog = new AlertDialog.Builder(DashWeb.this);
                passResetDialog.setTitle("Aviso");
                passResetDialog.setCancelable(false);
                if (modoViagemAtivo) {
                    passResetDialog.setMessage("O modo viagem define a temperatura de todos os seus ambientes para 10ºC.\nEste modo está ativo no momento. Deseja desativar?");
                } else {
                    passResetDialog.setMessage("O modo viagem define a temperatura de todos os seus ambientes para 10ºC.\nEste modo não está ativo no momento. Deseja ativar?");
                }
                passResetDialog.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        database.getReference("cliente/".concat(prefs.getString("chave", "null"))).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    boolean result = false;
                                    for (DataSnapshot device : snapshot.getChildren()) {
                                        if (modoViagemAtivo) {
                                            database.getReference("cliente/".concat(prefs.getString("chave", "null"))).child(device.getKey()).child("R").child("info").child("modoViagem").setValue(false);
                                            result = false;
                                        } else {
                                            database.getReference("cliente/".concat(prefs.getString("chave", "null"))).child(device.getKey()).child("R").child("info").child("modoViagem").setValue(true);
                                            result = true;
                                        }
                                    }
                                    modoViagemAtivo = result;
                                    Message message = mHandler.obtainMessage(1, "Modo viagem definido com sucesso!");
                                    message.sendToTarget();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.d("snapppp", "canceleddd");
                            }
                        });
                    }
                }).setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                passResetDialog.create().show();
            }
        });


        usuarios.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floatingMenu.close(true);
                Intent i = new Intent(getApplicationContext(), Usuarios.class);
                startActivity(i);
            }
        });

        contato.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floatingMenu.close(true);
                Intent i = new Intent(getApplicationContext(), Contato.class);
                startActivity(i);
            }
        });

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
                floatingMenu.close(true);
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
                ArrayList<Drawable> dw = new ArrayList<>(DashWeb.draw);

                Log.d("tamanho dashdraw", Integer.toString(DashWeb.draw.size()));
                DashWeb.draw.clear();
                Log.d("tamanho dw draw", Integer.toString(dw.size()));
                int i =0;
                int cor = ContextCompat.getColor(DashWeb.this,R.color.laranjalogo);
                for(Drawable d : dw){
                    Drawable a = d.getConstantState().newDrawable();
                    i++;
                    Log.d("defini o draw", Integer.toString(i));

                    DrawableCompat.setTint(a, cor);
                    DashWeb.draw.add(a);
                }
                floatingMenu.close(true);
                Intent a = new Intent(getApplicationContext(), PersonalizarIcones.class);
                startActivity(a);
                finish();
            }
        });


        SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Por favor, aguarde...");

        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        Log.d("email", prefs.getString("email", "null"));
        Log.d("uuid", prefs.getString("chave", "null"));
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

        ref = database.getReference("chaves/".concat(prefs.getString("chave", "null")));

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    progressDialog.dismiss();
                    Log.d("snap", snapshot.toString());
                    Log.d("snapshot", snapshot.getValue().toString());

                    if (snapshot.hasChild("ativo")) {
                        if (!Boolean.valueOf(snapshot.child("ativo").getValue().toString().toLowerCase())) {
                            passResetDialog.create().show();
                        } else {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("chave_original", "null");
                            if (snapshot.hasChild("alias")) {
                                Log.d("alias", snapshot.child("alias").getValue().toString());
                                editor.putString("chave_original", prefs.getString("chave", "null"));
                                editor.putString("chave", snapshot.child("alias").getValue().toString());
                                floatingMenu.removeMenuButton(usuarios);
                            }
                            if (snapshot.hasChild("residencial")) {
                                Log.d("residencial", snapshot.child("residencial").getValue().toString());
                                editor.putBoolean("residencial", (boolean)snapshot.child("residencial").getValue());
                                if(!(boolean)snapshot.child("residencial").getValue()){
                                    floatingMenu.removeMenuButton(prog);
                                    floatingMenu.removeMenuButton(modoViagem);
                                }
                            }
                            editor.apply();
                            Log.d("caminhooo", "cliente/".concat(prefs.getString("chave", "null")));
                            ref1 = database.getReference("cliente/".concat(prefs.getString("chave", "null")));
                            ref1.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        for (DataSnapshot device : snapshot.getChildren()) {
                                            Drawable unwrappedDrawable;
                                            Drawable wrappedDrawable;
                                            unwrappedDrawable = AppCompatResources.getDrawable(DashWeb.this, R.drawable.ic_home_iconuserselect);
                                            wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);

                                            if (device.hasChild("W/modoViagem")) {
                                                modoViagemAtivo = (boolean) device.child("W").child("modoViagem").getValue();
                                            }

                                            Log.d(" o caminho do cara e ", prefs.getString(prefs.getString("email", "null").concat(device.getKey().toLowerCase().concat("/IconUser")), "null"));
                                            Log.d("getKey", device.getKey());
                                            if (!prefs.getString(prefs.getString("email", "null").concat(device.getKey().toLowerCase().concat("/IconUser")), "null").equals("null")) {
                                                Log.d(" o cara tem icone ", "null");

                                                Field[] drawablesFields = R.drawable.class.getFields();

                                                for (Field field : drawablesFields) {
                                                    try {
                                                        if (field.getName().contains(prefs.getString(prefs.getString("email", "null").concat(device.getKey().toLowerCase().concat("/IconUser")), "null"))) {
                                                            Log.d("É esse aquii", field.getName());

                                                            unwrappedDrawable = AppCompatResources.getDrawable(DashWeb.this, field.getInt(null));
                                                            wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
                                                        }
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }


                                            if ((Math.abs(Long.valueOf(device.child("W").child("Timestamp").getValue().toString()) - System.currentTimeMillis()) / 1000) < offset) {

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
                                        if (snapshot.exists()) {
                                            Log.d("snapppp", snapshot.toString());
                                        }
                                        adapter = new GridAdapter(DashWeb.this, names, draw);
                                        gridView.setAdapter(adapter);
                                        progressDialog.dismiss();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.d("snapppp", "canceleddd");
                                }
                            });
                        }
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

        floatingMenu.close(true);
        //super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (draw != null) {
            if (draw.size() > 0) {

            }
        }
    }

    public static ArrayList<String> getDispositivos() {
        return dispositivos;
    }
}