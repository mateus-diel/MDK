package com.example.dashcontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.owl93.dpb.CircularProgressView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SetDataEsp extends AppCompatActivity {
    CircularProgressView temp;
    Button btnMenos;
    Button btnMais;
    Button btnLigaDesliga;
    TextView txtLocal;
    TextView txtStatus;
    TextView txtTempProg;
    String address;
    Thread thread;
    Handler mHandler;
    private Object mPauseLock;
    private boolean mPaused;
    private boolean mFinished;

    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_data_esp);
        mPauseLock = new Object();
        mPaused = false;
        mFinished = false;
        btnMenos = findViewById(R.id.btnMenos);
        btnMais = findViewById(R.id.btnMais);
        btnLigaDesliga = findViewById(R.id.bntLigaDesliga);
        txtLocal = findViewById(R.id.txtLocal);
        txtStatus = findViewById(R.id.txtLigaDesliga);
        txtTempProg = findViewById(R.id.txtTempProg);

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
                    temp.animateProgressChange(Math.round(json.getDouble("sensor1")),1000);
                    temp.setText(String.valueOf(Math.round(json.getDouble("sensor1"))).concat(" ºC"));
                    txtTempProg.setText(String.valueOf(Math.round(json.getDouble("tempPROG"))));


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

        temp = findViewById(R.id.progessView);
        temp.setMaxValue(50);
        temp.setProgress(0);
        temp.setTextEnabled(true);
        btnMenos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {try {
                JSONObject config = new JSONObject();
                config.put("tempPROG", Float.parseFloat(txtTempProg.getText().toString())-1);

                Log.d("jsonOb",config.toString());
                sendConfigEsp(requestQueue,address,config);

            }catch (Exception e){
                Log.d("json error",e.getMessage());
            }
            }
        });
        btnMais.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONObject config = new JSONObject();
                    config.put("tempPROG", Float.parseFloat(txtTempProg.getText().toString())+1);

                    Log.d("jsonOb",config.toString());
                    sendConfigEsp(requestQueue,address,config);

                }catch (Exception e){
                    Log.d("json error",e.getMessage());
                }

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
                    sendConfigEsp(requestQueue,address,config);

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

        private void sendConfigEsp(RequestQueue q, String url, JSONObject json){
        String address = url.replace("get","post");
        Log.d("esp endereço completo", address);
        //String address = "https://httpbin.org/post";

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                Request.Method.POST,address, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject res = new JSONObject(response.toString());
                            if(res.getString("request").toString().equals("ok")){
                                printToast("Dados definidos. ESP vai reiniciar para aplicar as modificações!", Toast.LENGTH_LONG);
                            }else{
                                printToast("Não foi possível denifir os dados!", Toast.LENGTH_SHORT);
                            }
                        }catch (Exception e){
                            Log.d("error parse json", e.getMessage());
                            printToast("Não foi possível denifir os dados!", Toast.LENGTH_SHORT);

                        }

                        Log.d("onResponse", response.toString());
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
        q.add(jsonObjReq);
    }
    private void printToast(String message, int lenght){
        Toast.makeText(getApplicationContext(),message,lenght).show();
    }
}