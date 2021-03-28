package com.marcoa.marcoa;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.owl93.dpb.CircularProgressView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SetDataEsp extends AppCompatActivity {
    CircularProgressView temp;
    Button btnLigaDesliga, manual, automatico;
    TextView txtLocal, txtModo;
    TextView txtStatus;
    TextView txtTempProg;
    String address;
    Thread thread;
    Handler mHandler;
    private Object mPauseLock;
    private boolean mPaused;
    private boolean mFinished;
    private LoadingDialog loadingDialog;
    SeekBar seekBar;


    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_data_esp);
        txtModo= findViewById(R.id.txtModo);
        manual= findViewById(R.id.btnManual);
        automatico= findViewById(R.id.btnAuto);
        loadingDialog = new LoadingDialog(this);
        loadingDialog.startLoadingDialog();
        mPauseLock = new Object();
        mPaused = false;
        mFinished = false;
        btnLigaDesliga = findViewById(R.id.bntLigaDesliga);
        txtLocal = findViewById(R.id.txtLocal);
        txtStatus = findViewById(R.id.txtLigaDesliga);
        txtTempProg = findViewById(R.id.txtTempProgLocal);
        temp = findViewById(R.id.progessView);
        temp.setMaxValue(50);
        temp.setProgress(0);
        temp.setTextEnabled(true);
        temp.setText("...");

        seekBar = findViewById(R.id.seekBarLocal);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtTempProg.setText(Integer.toString(progress));

            };

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try{
                    JSONObject config = new JSONObject();
                    config.put("tempPROG", Float.parseFloat(txtTempProg.getText().toString()));
                    Log.d("jsonOb",config.toString());
                    sendConfigEsp(new Button(SetDataEsp.this), requestQueue,address,config);
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });

        manual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    JSONObject config = new JSONObject();
                    config.put("auto", false);
                    Log.d("jsonOb",config.toString());
                    sendConfigEsp(new Button(SetDataEsp.this), requestQueue,address,config);
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });

        automatico.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    JSONObject config = new JSONObject();
                    config.put("auto", true);
                    Log.d("jsonOb",config.toString());
                    sendConfigEsp(new Button(SetDataEsp.this), requestQueue,address,config);
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });


        Intent intent = getIntent();
        txtLocal.setText(intent.getStringExtra("nome").toUpperCase());
        address = "http://".concat(intent.getStringExtra("ip")).concat("/get");
        Log.d("endereco ip",address);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, address, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject json = new JSONObject(response.toString());
                    Log.d("json array", json.toString());
                    if(json.getBoolean("linha_1")){
                        txtStatus.setText("Ligado!");
                        btnLigaDesliga.setText("Desligar");
                    }else{
                        txtStatus.setText("Desligado!");
                        btnLigaDesliga.setText("Ligar");
                    }
                    if(json.has("auto")){
                        if(json.getBoolean("auto")){
                            automatico.setEnabled(false);
                            manual.setEnabled(true);
                            txtModo.setText("Automático");
                        }else{
                            manual.setEnabled(false);
                            automatico.setEnabled(true);
                            txtModo.setText("Manual");
                        }
                    }

                    temp.animateProgressChange((float)json.getDouble("sensor1"),1000);
                    temp.setText(String.format("%.1f",(float)json.getDouble("sensor1")).replace(",",".").concat(" ºC"));
                    txtTempProg.setText(String.valueOf(Math.round(json.getDouble("tempPROG"))));
                    seekBar.setProgress((int) Math.round(json.getDouble("tempPROG")));
                    loadingDialog.dimissDialog();


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });


        btnLigaDesliga.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONObject config = new JSONObject();
                    if(btnLigaDesliga.getText().toString().equals("Desligar")){
                        config.put("linha_1", false);
                    }else{
                        config.put("linha_1", true);
                    }

                    Log.d("jsonOb",config.toString());
                    sendConfigEsp(btnLigaDesliga, requestQueue,address,config);

                }catch (Exception e){
                    Log.d("json error",e.getMessage());
                }

            }
        });

        thread = new Thread(){
            public void run(){

                while (!mFinished) {
                    // Do stuff.
                    try {
                        Thread.sleep(2500);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    requestQueue.add(jsonObjectRequest);

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

    @Override
    protected void onResume() {
        super.onResume();
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
        }
    }

    @Override
    protected void onPause() {
        synchronized (mPauseLock) {
            mPaused = true;
        }
        super.onPause();
    }

        private void sendConfigEsp(Button b, RequestQueue q, String url, JSONObject json){
        String address = url.replace("get","post");
        Log.d("esp endereço completo", address);
        //String address = "https://httpbin.org/post";

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                Request.Method.POST,address, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        b.setEnabled(true);
                        try {
                            JSONObject json = new JSONObject(response.toString());
                            Log.d("json array", json.toString());
                            if(json.getBoolean("linha_1")){
                                txtStatus.setText("Ligado!");
                                btnLigaDesliga.setText("Desligar");
                            }else{
                                txtStatus.setText("Desligado!");
                                btnLigaDesliga.setText("Ligar");
                            }
                            txtTempProg.setText(String.valueOf(Math.round(json.getDouble("tempPROG"))));


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("onErrorResponse", "Error: " + error.getMessage());
                printToast("Não foi possível comunicar-se com o ESP. Verifique sua conexão!", Toast.LENGTH_LONG);
                Log.d("Erro volley", error.toString());

            }
        }) {

            /**
             * Passing some request headers
             */
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        b.setEnabled(false);
        q.add(jsonObjReq);
    }
    private void printToast(String message, int lenght){
        Toast.makeText(getApplicationContext(),message,lenght).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent a = new Intent(SetDataEsp.this, DashLocal.class);
        startActivity(a);
        finish();
    }
}