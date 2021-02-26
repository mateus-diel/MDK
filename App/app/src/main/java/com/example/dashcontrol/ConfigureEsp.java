package com.example.dashcontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigureEsp extends AppCompatActivity {
    WifiManager wifiManager;
    WifiReceiver wifiReceiver;
    ListAdapter listAdapter;
    ListView wifiList;
    List myWifiList;
    TextView txtRedeSelecionada;
    Button salvarConfig;
    RequestQueue queue;
    JSONObject config;
    SharedPreferences prefs;
    boolean isAutenticated;
    static ImageView iconSelect;
    static AlertDialog show;
    static String dirDrawable;

    @SuppressLint("WifiManagerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_esp);
        queue = Volley.newRequestQueue(this);
        config = new JSONObject();
        isAutenticated = false;
        iconSelect = findViewById(R.id.iconSelect);
        iconSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder icones = new AlertDialog.Builder(ConfigureEsp.this);
                icones.setTitle("Selecione um icone");
                icones.setCancelable(false);
                GridLayout grid = new GridLayout(ConfigureEsp.this);
                grid.setColumnCount(5);
                Field[] drawablesFields = com.example.dashcontrol.R.drawable.class.getFields();
                ImageView img;

                for (Field field : drawablesFields) {
                    try {
                        Log.i("LOG_TAG", "com.your.project.R.drawable." + field.getName());
                        if(field.getName().contains("iconuserselect")) {
                            img = new ImageView(ConfigureEsp.this);
                            img.setImageResource(field.getInt(null));
                            img.setTag(field.getName());
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, 100);
                            layoutParams.setMargins(10, 10, 10, 10);
                            img.setLayoutParams(layoutParams);
                            img.setOnClickListener(ConfigureEsp::onClicIconSelect);
                            grid.addView(img);
                            //img.requestLayout();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                icones.setView(grid);
                show = icones.show();
            }
        });
         prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
         Log.d("email",prefs.getString("email","null"));
         Log.d("senha",prefs.getString("senha","null"));
         Log.d("chave",prefs.getString("chave","null"));
         if(prefs.getString("email","null").equals("null") || prefs.getString("senha","null").equals("null") || prefs.getString("chave","null").equals("null")){
             AlertDialog.Builder passResetDialog = new AlertDialog.Builder(this);
             passResetDialog.setTitle("Ops!");
             passResetDialog.setMessage("Para poder configurar um novo dispositivo, você precisar ter logado ao menos uma vez utilizando o seu email e senha! Clique em continuar para preencher todos os dados de forma manual, ou volte e faça login.");
             passResetDialog.setCancelable(false);
             passResetDialog.setNegativeButton("Voltar", new DialogInterface.OnClickListener() {
                 @Override
                 public void onClick(DialogInterface dialog, int which) {
                     Intent intent = new Intent(getApplicationContext(), DashLocal.class);
                     startActivity(intent);
                     finish();
                 }
             }).setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                 @Override
                 public void onClick(DialogInterface dialog, int which) {
                     dialog.cancel();
                     EditText resetMail = new EditText(ConfigureEsp.this);
                     resetMail.setHint("Email");
                     resetMail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                     EditText pw = new EditText(ConfigureEsp.this);
                     pw.setHint("Senha");
                     pw.setInputType(InputType.TYPE_TEXT_VARIATION_NORMAL);
                     EditText key = new EditText(ConfigureEsp.this);
                     key.setHint("Chave de ativação");
                     key.setInputType(InputType.TYPE_TEXT_VARIATION_NORMAL);
                     LinearLayout ll=new LinearLayout(ConfigureEsp.this);
                     ll.setOrientation(LinearLayout.VERTICAL);
                     ll.addView(resetMail);
                     ll.addView(pw);
                     ll.addView(key);
                     AlertDialog.Builder dialogg = new AlertDialog.Builder(ConfigureEsp.this);
                     dialogg.setTitle("Validação");
                     dialogg.setMessage("Insira os dados de autenticação ou solicite-nos!");
                     dialogg.setView(ll);
                     dialogg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int which) {

                         }
                     });
                     dialogg.create().show();

                 }
             });
             passResetDialog.create().show();
         }else{
             isAutenticated = true;
         }


        txtRedeSelecionada = findViewById(R.id.txtRedeSelecionada);
        salvarConfig = findViewById(R.id.btnSalvar);
        salvarConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
                final DhcpInfo dhcp = manager.getDhcpInfo();
                final String address = Formatter.formatIpAddress(dhcp.gateway);
                try {
                    config.put("ssid", ((TextView)findViewById(R.id.txtRedeSelecionada)).getText());
                    config.put("configNetwork", false);
                    config.put("password", ((EditText) findViewById(R.id.senhaWifi)).getText());
                    config.put("icon", dirDrawable);
                    config.put("deviceName", ((TextView) findViewById(R.id.nomeDispositivo)).getText());
                    if(isAutenticated){
                        config.put("email", prefs.getString("email","null"));
                        config.put("senha", prefs.getString("senha","null"));
                        config.put("chave", prefs.getString("chave","null"));
                    }
                    queue = Volley.newRequestQueue(getApplicationContext());
                    sendConfigEsp(queue,address,config);

                }catch (Exception e){
                    Log.d("json error",e.getMessage());
                }





            }
        });


        wifiList = (ListView)findViewById(R.id.mylistView);
        wifiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                txtRedeSelecionada.setText(((List<ScanResult>)myWifiList).get(position).SSID);

            }


        });
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);

        } else {
            scanWifiList();
        }


    }

    private static void onClicIconSelect(View view) {
        Field[] drawablesFields = com.example.dashcontrol.R.drawable.class.getFields();

        for (Field field : drawablesFields) {
            try {
                Log.i("LOG_TAG", "com.your.project.R.drawable." + field.getName());
                if (field.getName().contains(view.getTag().toString())) {
                    iconSelect.setImageResource(field.getInt(null));
                    dirDrawable = field.getName();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        show.dismiss();

    }

    private void scanWifiList() {
        boolean ok = wifiManager.startScan();
        myWifiList = wifiManager.getScanResults();
        setAdapter();
    }


    private void setAdapter() {
        listAdapter = new ListAdapter(getApplicationContext(), myWifiList);
        wifiList.setAdapter(listAdapter);
    }

    private void printToast(String message, int lenght){
        Toast.makeText(getApplicationContext(),message,lenght).show();
    }

    class WifiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);
        }
    }

    private void sendInfo(RequestQueue q, String url) {


        //Configura a requisicao
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // mostra a resposta
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error.Response", error.toString());
                    }
                });

// Adiciona a Fila de requisicoes
        q.add(getRequest);
    }

    private void scanSuccess() {
        List<ScanResult> results = wifiManager.getScanResults();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Infelizmente você não concedeu permisso~es para configurar a WiFi. Tente novamente!", Toast.LENGTH_LONG);
        } else {
            scanWifiList();
        }
    }

    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        List<ScanResult> results = wifiManager.getScanResults();

    }

    private void sendConfigEsp(RequestQueue q, String url, JSONObject json) {
        String address = "http://".concat(url).concat("/post");
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
}