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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

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
    AlertDialog.Builder sistemabloqueado;
    private RequestQueue requestQueue;

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

        requestQueue = Volley.newRequestQueue(getApplicationContext());

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
                        JSONObject params = new JSONObject();
                        try {
                            params.put("id_usuario", prefs.getString("chave", "null"));
                            params.put("id_cliente", prefs.getString("chave_cliente", "null"));
                            if(modoViagemAtivo){
                                params.put("modo_viagem", 0);
                            }else{
                                params.put("modo_viagem", 1);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        comm(params,"api/dispositivo/modoviagem");
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
        sistemabloqueado = new AlertDialog.Builder(this);
        sistemabloqueado.setTitle("Aviso");
        sistemabloqueado.setCancelable(false);
        sistemabloqueado.setMessage("Sistema não está registrado, entre em contato para maiores informações!");
        sistemabloqueado.setPositiveButton("Sair", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("autoLogin", false);
                editor.apply();
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        ref = database.getReference("chaves/".concat(prefs.getString("chave", "null")));


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
        if(requestQueue != null){
            dispositivos = new ArrayList<>();
            names = new ArrayList<>();
            draw = new ArrayList<>();

            JSONObject params = new JSONObject();
            try {
               params.put("id", prefs.getString("chave", "null"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            comm(params,"api/dispositivo/dados");

           /* ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        progressDialog.dismiss();
                        Log.d("snap", snapshot.toString());
                        Log.d("snapshot", snapshot.getValue().toString());

                        if (snapshot.hasChild("ativo")) {
                            if (!Boolean.valueOf(snapshot.child("ativo").getValue().toString().toLowerCase())) {
                                sistemabloqueado.create().show();
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
                                                unwrappedDrawable = AppCompatResources.getDrawable(DashWeb.this, R.drawable.ic_burn);
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

            });*/

        }
    }

    public static ArrayList<String> getDispositivos() {
        return dispositivos;
    }

    private void comm(JSONObject params, String path){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getResources().getString(R.string.server).concat(path), params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("sucessso", response.toString());
                try {
                    AlertDialog.Builder dial = new AlertDialog.Builder(DashWeb.this);
                    dial.setTitle("Aviso");
                    dial.setCancelable(false);
                    JSONObject json = new JSONObject(response.toString());
                    Log.d("json array", json.toString());

                    if (json.has("code")) {
                        if (json.getInt("code") == 900) {
                            dial.setMessage("Houve uma falha ao entrar, tente novamente mais tarde!\nCódigo: ".concat(Integer.toString(json.getInt("code"))));
                            dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            dial.create().show();
                        } else if (json.getInt("code") == 200) {
                            names.clear();
                            draw.clear();
                            dispositivos.clear();
                            Iterator<String> iter = json.keys();
                            while (iter.hasNext()) {
                                String key = iter.next();
                                try {
                                    JSONObject value = null;
                                    if(!key.equals("code")) {
                                        value = json.getJSONObject(key);
                                        if (value.has("uuid")) {
                                            prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
                                            Log.d("value of json", value.toString());
                                            Log.d("keeey ", key);
                                            Log.d("uuuuidddd ", value.getString("uuid"));
                                            Drawable unwrappedDrawable;
                                            Drawable wrappedDrawable;
                                            unwrappedDrawable = AppCompatResources.getDrawable(DashWeb.this, R.drawable.ic_burn);
                                            wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);


                                            if (value.has("modo_viagem")) {
                                                Log.d("tem o modo", "viagemmm");
                                                Log.d("value int", String.valueOf(value.getInt("modo_viagem")));
                                                Log.d("huehuhe", "viagem");
                                                modoViagemAtivo = intToBoolean(value.getInt("modo_viagem"));
                                                Log.d("tem o modo", "viagem");

                                            }

                                            if (prefs != null) {
                                                Log.d("prefs nao", "esta null");
                                            } else {
                                                Log.d("prefs ", "esta null");
                                            }

                                            Log.d(" o caminho do cara e ", prefs.getString(prefs.getString("email", "null") + (value.getString("uuid") + ("/IconUser")), "null"));
                                            Log.d("getKey", value.getString("uuid"));
                                            if (!prefs.getString(prefs.getString("email", "null").concat(value.getString("uuid").concat("/IconUser")), "null").equals("null")) {
                                                Log.d(" o cara tem icone ", "null");

                                                Field[] drawablesFields = R.drawable.class.getFields();

                                                for (Field field : drawablesFields) {
                                                    try {
                                                        if (field.getName().contains(prefs.getString(prefs.getString("email", "null").concat(key.toLowerCase().concat("/IconUser")), "null"))) {
                                                            Log.d("É esse aquii", field.getName());

                                                            unwrappedDrawable = AppCompatResources.getDrawable(DashWeb.this, field.getInt(null));
                                                            wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
                                                        }
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }

                                            String target = value.getString("ultima_sincronizacao");
                                            DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss", Locale.ENGLISH);
                                            Date result = null;
                                            try {
                                                result = df.parse(target);
                                                Log.d("datee", result.toString());
                                            } catch (ParseException e) {
                                                e.printStackTrace();
                                            }


                                            if ((Math.abs(Long.valueOf(result.getTime()) - System.currentTimeMillis()) / 1000) < offset) {

                                                DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(DashWeb.this, R.color.laranjalogo));
                                                names.add(value.getString("nome").toUpperCase());
                                                draw.add(wrappedDrawable);
                                                dispositivos.add(value.getString("nome").toUpperCase());

                                            } else {
                                                DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(DashWeb.this, R.color.azulonline));
                                                //DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(DashWeb.this, R.color.azulonline));
                                                names.add(value.getString("nome").toUpperCase());
                                                draw.add(wrappedDrawable);
                                                dispositivos.add(value.getString("nome").toUpperCase());
                                            }

                                            adapter = new GridAdapter(DashWeb.this, names, draw);
                                            gridView.setAdapter(adapter);
                                            gridView.invalidate();
                                            Log.d("tam names", Integer.toString(names.size()));
                                            Log.d("tam draw", Integer.toString(draw.size()));
                                        }
                                    }
                                } catch (JSONException e) {
                                    Log.d("errrooo",e.getMessage());
                                }

                            }

                        } else if (json.getInt("code") == 902) {

                        }

                    }

                    //loadingDialog.dimissDialog();


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                progressDialog.dismiss();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("erroooo", error.getMessage());
                progressDialog.dismiss();

            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private boolean intToBoolean(int a){
        boolean b = false;
        if(a == 1){
            b = true;
        }
        return b;
    }
}