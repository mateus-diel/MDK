package com.marcoa.marcoa;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.owl93.dpb.CircularProgressView;

import org.json.JSONException;
import org.json.JSONObject;

public class DataEspWeb extends AppCompatActivity {
    CircularProgressView temp;
    Button btnLigaDesliga, btnVerProg, btnManualWeb, btnAutoWeb;
    TextView txtLocal;
    TextView txtStatus, modoWeb;
    TextView txtTempProg;
    ProgressDialog dial;
    ProgressDialog progressDialog;
    SeekBar seekBar;
    Thread t, thread;
    Intent intent;
    int timeoutEsp = 20;
    SharedPreferences prefs;
    private Object mPauseLock;
    private boolean mPaused;
    private boolean mFinished;
    RequestQueue requestQueue;

    volatile int lastTempProg, tempRecebido = 0;
    volatile boolean lastLigaDesliga, ligaDesliga = false;
    volatile boolean modo, lastModo = false;
    Activity activity;
    boolean modoAtual = false;
    boolean modoViagem = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_esp_web);
        activity = this;

        //androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        //actionBar.setDisplayShowCustomEnabled(true);
        //LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //View view = inflater.inflate(R.layout.custom_bar, null);
        //actionBar.setCustomView(view);
        seekBar = findViewById(R.id.seekBar);
        btnVerProg = findViewById(R.id.btnVerProg);
        btnAutoWeb = findViewById(R.id.btnAutoWeb);
        btnManualWeb = findViewById(R.id.btnManualWeb);
        modoWeb = findViewById(R.id.txtModoWeb);


        mPauseLock = new Object();
        mPaused = false;
        mFinished = false;

        intent = getIntent();


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
        temp.setMaxValue(40);
        temp.setProgress(0);
        temp.setTextEnabled(true);
        temp.setText("...");
        txtLocal.setText(intent.getStringExtra("deviceName").substring(intent.getStringExtra("deviceName").indexOf("*/*") + 3).toUpperCase());
        Log.d("nome do dispositivo", intent.getStringExtra("deviceName"));
        /*listener =  new ValueEventListener() {
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
                        tempRecebido = Integer.parseInt(device.getValue().toString());
                    }else if(device.getKey().equals("auto") && Boolean.valueOf(device.getValue().toString().toLowerCase())){
                        btnManualWeb.setEnabled(true);
                        btnAutoWeb.setEnabled(false);
                        seekBar.setEnabled(false);
                        modoWeb.setText("Automático");
                        txtStatus.setText("Automático");
                        modoAtual = true;
                        modo = true;
                    }else if(device.getKey().equals("auto") && !Boolean.valueOf(device.getValue().toString().toLowerCase())){
                        btnManualWeb.setEnabled(false);
                        btnAutoWeb.setEnabled(true);
                        seekBar.setEnabled(true);
                        modoWeb.setText("Manual");
                        modoAtual = false;
                        modo=false;
                    }else if(device.getKey().equals("modoViagem")){
                        if(Boolean.valueOf(device.getValue().toString().toLowerCase())){
                            modoViagem = true;
                            modoWeb.setText("Modo Viagem");
                        }else{
                            modoViagem = false;
                        }
                    }


                }
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };*/

        //ref = database.getReference("cliente/".concat(prefs.getString("chave","null")).concat("/").concat(intent.getStringExtra("deviceName").toLowerCase()).concat("/W"));
        Log.d("Caminho do cliente", "cliente/".concat(prefs.getString("chave", "null")).concat("/").concat(intent.getStringExtra("deviceName")).concat("/W"));


        btnLigaDesliga.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (modoAtual) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(DataEspWeb.this);
                    dialog.setTitle("Aviso");
                    dialog.setMessage("O ambiente está trabalhando no modo automático! Se você deseja ajustar manualmente, coloque-o no modo manual!");
                    dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.create().show();
                } else if (modoViagem) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(DataEspWeb.this);
                    dialog.setTitle("Aviso");
                    dialog.setMessage("O ambiente está com o modo viagem ativo. Se você deseja ajustar manualmente, desative o modo viagem!");
                    dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.create().show();
                } else {
                    if (btnLigaDesliga.getText().toString().toLowerCase().equals("desligar")) {
                        sendLigaDesliga(false);
                        ligaDesliga = false;
                        lastLigaDesliga = true;

                    } else {
                        sendLigaDesliga(true);
                        lastLigaDesliga = false;
                        ligaDesliga = true;
                    }
                }
            }
        });

        btnManualWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastModo = false;
                sendModo(false);
            }
        });

        btnAutoWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastModo = true;
                sendModo(true);

            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtTempProg.setText(Integer.toString(progress));

            }

            ;

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                lastTempProg = Integer.parseInt(txtTempProg.getText().toString());
                sendTemp(Float.parseFloat(txtTempProg.getText().toString()));
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
                Intent i = new Intent(DataEspWeb.this, VerProgramacoesWeb.class);
                Log.d("passei na intent", intent.getStringExtra("deviceName"));
                i.putExtra("deviceName", intent.getStringExtra("deviceName"));
                startActivity(i);
            }
        });

        JSONObject params = new JSONObject();
        try {
            params.put("uuid", intent.getStringExtra("deviceName").substring(0, intent.getStringExtra("deviceName").indexOf("*/*")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        requestQueue = Volley.newRequestQueue(getApplicationContext());


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getResources().getString(R.string.server).concat("api/dispositivo/dados/esp"), params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("sucessso", response.toString());
                progressDialog.dismiss();
                try {

                    JSONObject json = new JSONObject(response.toString());
                    Log.d("json array", json.toString());

                    if (json.has("code")) {
                        if (json.getInt("code") == 900) {

                        } else if (json.getInt("code") == 200) {
                            JSONObject dispositivo = json.getJSONObject("0");
                            if (dispositivo.has("temp_atual")) {
                                temp.animateProgressChange((float) dispositivo.getDouble("temp_atual"), 1000);
                                temp.setText(String.format("%.1f", (float) dispositivo.getDouble("temp_atual")).replace(",", ".").concat(" ºC"));
                            }
                            if (dispositivo.has("temp_prog_esp_ler")) {
                                txtTempProg.setText(String.valueOf(Math.round(dispositivo.getDouble("temp_prog_esp_ler"))));
                                seekBar.setProgress((int) Math.round(dispositivo.getDouble("temp_prog_esp_ler")));
                            }
                            if (dispositivo.has("status_esp_ler")) {
                                if (intToBoolean(dispositivo.getInt("status_esp_ler"))) {
                                    txtStatus.setText("Ligado!");
                                    seekBar.setEnabled(true);
                                    btnLigaDesliga.setText("Desligar");
                                } else {
                                    txtStatus.setText("Desligado!");
                                    btnLigaDesliga.setText("Ligar");
                                    seekBar.setEnabled(false);
                                }
                            }
                            if (dispositivo.has("modo_viagem_esp_ler")) {
                                if (intToBoolean(dispositivo.getInt("modo_viagem_esp_ler"))) {
                                    modoViagem = true;
                                    modoWeb.setText("Modo Viagem");
                                } else {
                                    modoViagem = false;
                                }
                            }

                            if (dispositivo.has("auto_esp_ler")) {
                                if (intToBoolean(dispositivo.getInt("auto_esp_ler"))) {
                                    btnManualWeb.setEnabled(true);
                                    btnAutoWeb.setEnabled(false);
                                    seekBar.setEnabled(false);
                                    modoWeb.setText("Automático");
                                    txtStatus.setText("Automático");
                                    modoAtual = true;
                                    modo = true;
                                } else {
                                    btnManualWeb.setEnabled(false);
                                    btnAutoWeb.setEnabled(true);
                                    seekBar.setEnabled(true);
                                    modoWeb.setText("Manual");
                                    modoAtual = false;
                                    modo = false;
                                }
                            }
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                error.printStackTrace();
                Log.d("erroooo", error.getMessage());

            }
        });

        thread = new Thread() {
            public void run() {

                while (!mFinished) {
                    //do stuff
                    requestQueue.add(jsonObjectRequest);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    synchronized (mPauseLock) {
                        while (mPaused) {

                            try {
                                mPauseLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            }
        };

        thread.start();


    }

    private void sendLigaDesliga(boolean state) {
        dial = new ProgressDialog(this);
        dial.setMessage("Por favor aguarde, estamos contatando o dispositivo...");
        dial.setCanceledOnTouchOutside(false);
        dial.show();


        JSONObject params = new JSONObject();
        try {
            params.put("uuid", intent.getStringExtra("deviceName").substring(0, intent.getStringExtra("deviceName").indexOf("*/*")));
            params.put("acao", "liga");
            params.put("valor", booleanToInt(state));
            Log.d("parametros", params.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        comm(params);

        /*

        database.getReference().child("cliente").child(prefs.getString("chave","null")).child(intent.getStringExtra("deviceName")).child("R").child("info").child("LINHA_1").setValue(state).addOnSuccessListener(new OnSuccessListener<Void>() {

            volatile int time = 0;
            volatile int timeout = 150;//15 segundos
            @Override
            public void onSuccess(Void aVoid) {
                t = new Thread() {
                    public void run() {
                        //Log.i("Last temp progh", String.valueOf(lastTempProg));
                        //Log.i("temp prog", String.valueOf(txtTempProg.getText().toString()));
                        Log.i("liga desliga", String.valueOf(ligaDesliga));
                        Log.i("last liga desliga", String.valueOf(lastLigaDesliga));
                        //Log.i("modo", String.valueOf(modo));
                        //Log.i("last modo", String.valueOf(lastModo));
                        //while (lastTempProg != Integer.parseInt(txtTempProg.getText().toString()) || lastLigaDesliga != ligaDesliga || lastModo != modo){
                        while (lastLigaDesliga != ligaDesliga){
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
        });*/
    }

    private void sendModo(boolean state) {
        dial = new ProgressDialog(this);
        dial.setMessage("Por favor aguarde, estamos contatando o dispositivo...");
        dial.setCanceledOnTouchOutside(false);
        dial.show();


        JSONObject params = new JSONObject();
        try {
            params.put("uuid", intent.getStringExtra("deviceName").substring(0, intent.getStringExtra("deviceName").indexOf("*/*")));
            params.put("acao", "modoAutomatico");
            params.put("valor", booleanToInt(state));
        } catch (Exception e) {
            e.printStackTrace();
        }

        comm(params);


        /*dial = new ProgressDialog(this);
        dial.setMessage("Por favor aguarde, estamos contatando o dispositivo...");
        dial.setCanceledOnTouchOutside(false);
        dial.show();

        database.getReference().child("cliente").child(prefs.getString("chave","null")).child(intent.getStringExtra("deviceName")).child("R").child("info").child("auto").setValue(state).addOnSuccessListener(new OnSuccessListener<Void>() {

            volatile int time = 0;
            volatile int timeout = 150;//15 segundos
            @Override
            public void onSuccess(Void aVoid) {
                t = new Thread() {
                    public void run() {
                        //Log.i("Last temp progh", String.valueOf(lastTempProg));
                        //Log.i("temp prog", String.valueOf(txtTempProg.getText().toString()));
                        //Log.i("liga desliga", String.valueOf(ligaDesliga));
                        //Log.i("last liga desliga", String.valueOf(lastLigaDesliga));
                        Log.i("modo", String.valueOf(modo));
                        Log.i("last modo", String.valueOf(lastModo));
                        //while (lastTempProg != Integer.parseInt(txtTempProg.getText().toString()) || lastLigaDesliga != ligaDesliga || lastModo != modo){
                        while (lastModo != modo){
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
        });*/
    }

    private void sendTemp(Float state) {
        dial = new ProgressDialog(this);
        dial.setMessage("Por favor aguarde, estamos contatando o dispositivo...");
        dial.setCanceledOnTouchOutside(false);
        dial.show();


        JSONObject params = new JSONObject();
        try {
            params.put("uuid", intent.getStringExtra("deviceName").substring(0, intent.getStringExtra("deviceName").indexOf("*/*")));
            params.put("acao", "temp");
            params.put("valor", state);
        } catch (Exception e) {
            e.printStackTrace();
        }

        comm(params);
        /*dial = new ProgressDialog(this);
        dial.setMessage("Por favor aguarde, estamos contatando o dispositivo...");
        dial.setCanceledOnTouchOutside(false);
        dial.show();

        database.getReference().child("cliente").child(prefs.getString("chave","null")).child(intent.getStringExtra("deviceName")).child("R").child("info").child("tempPROG").setValue(state).addOnSuccessListener(new OnSuccessListener<Void>() {

            volatile int time = 0;
            volatile int timeout = 150;//15 segundos
            @Override
            public void onSuccess(Void aVoid) {
                t = new Thread() {
                    public void run() {
                        Log.i("Last temp progh", String.valueOf(lastTempProg));
                        Log.i("tempRecebido", String.valueOf(tempRecebido));
                        //Log.i("modo", String.valueOf(modo));
                        //Log.i("last modo", String.valueOf(lastModo));
                        //while ( || lastLigaDesliga != ligaDesliga || lastModo != modo){
                        while (lastTempProg != tempRecebido){
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
        });*/
    }

    private void comm(JSONObject params) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getResources().getString(R.string.server).concat("api/dispositivo/dados/set"), params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dial.dismiss();
                checkInfo(response);
                Log.d("sucessso", response.toString());


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("erroooo", error.getMessage());
                dial.dismiss();

            }
        });

        requestQueue.add(jsonObjectRequest);
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
        synchronized (mPauseLock) {
            mPaused = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
        }
        if (temp != null) {
            temp.invalidate();
            temp.postInvalidate();
            temp.refreshDrawableState();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent a = new Intent(DataEspWeb.this, DashWeb.class);
        startActivity(a);
        finish();
    }

    private boolean intToBoolean(int a) {
        boolean res = false;
        if (a > 0) {
            res = true;
        }
        return res;
    }

    private int booleanToInt(boolean b) {
        int a = 0;
        if (b) {
            a = 1;
        }
        return a;
    }

    private void checkInfo(JSONObject response){
        try{
            JSONObject json = new JSONObject(response.toString());
            Log.d("json array", json.toString());

            if (json.has("code")) {
                if (json.getInt("code") == 900) {

                } else if (json.getInt("code") == 200) {
                    JSONObject dispositivo = json.getJSONObject("0");
                    if (dispositivo.has("temp_atual")) {
                        temp.animateProgressChange((float) dispositivo.getDouble("temp_atual"), 1000);
                        temp.setText(String.format("%.1f", (float) dispositivo.getDouble("temp_atual")).replace(",", ".").concat(" ºC"));
                    }
                    if (dispositivo.has("temp_prog_esp_ler")) {
                        txtTempProg.setText(String.valueOf(Math.round(dispositivo.getDouble("temp_prog_esp_ler"))));
                        seekBar.setProgress((int) Math.round(dispositivo.getDouble("temp_prog_esp_ler")));
                    }
                    if (dispositivo.has("status_esp_ler")) {
                        if (intToBoolean(dispositivo.getInt("status_esp_ler"))) {
                            txtStatus.setText("Ligado!");
                            seekBar.setEnabled(true);
                            btnLigaDesliga.setText("Desligar");
                        } else {
                            txtStatus.setText("Desligado!");
                            btnLigaDesliga.setText("Ligar");
                            seekBar.setEnabled(false);
                        }
                    }
                    if (dispositivo.has("modo_viagem_esp_ler")) {
                        if (intToBoolean(dispositivo.getInt("modo_viagem_esp_ler"))) {
                            modoViagem = true;
                            modoWeb.setText("Modo Viagem");
                        } else {
                            modoViagem = false;
                        }
                    }

                    if (dispositivo.has("auto_esp_ler")) {
                        if (intToBoolean(dispositivo.getInt("auto_esp_ler"))) {
                            btnManualWeb.setEnabled(true);
                            btnAutoWeb.setEnabled(false);
                            seekBar.setEnabled(false);
                            modoWeb.setText("Automático");
                            txtStatus.setText("Automático");
                            modoAtual = true;
                            modo = true;
                        } else {
                            btnManualWeb.setEnabled(false);
                            btnAutoWeb.setEnabled(true);
                            seekBar.setEnabled(true);
                            modoWeb.setText("Manual");
                            modoAtual = false;
                            modo = false;
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}