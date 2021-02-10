package com.example.dashcontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

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

    @SuppressLint("WifiManagerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_esp);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        queue = Volley.newRequestQueue(this);
        config = new JSONObject();


        txtRedeSelecionada = findViewById(R.id.txtRedeSelecionada);
        salvarConfig = findViewById(R.id.btnSalvar);
        salvarConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("cliquei","salva config agora");
                final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
                final DhcpInfo dhcp = manager.getDhcpInfo();
                final String address = Formatter.formatIpAddress(dhcp.gateway);
                Log.d("gatewayip",address);
                try {
                    config.put("ssid", ((TextView)findViewById(R.id.txtRedeSelecionada)).getText());
                    config.put("configNetwork", false);
                    config.put("password", ((EditText)findViewById(R.id.senhaWifi)).getText());
                    config.put("deviceName", ((TextView)findViewById(R.id.nomeDispositivo)).getText());
                    Log.d("jsonOb",config.toString());
                    printToast(config.toString(), Toast.LENGTH_LONG);

                }catch (Exception e){
                    Log.d("json error",e.getMessage());
                }



            }
        });


        wifiList = (ListView)findViewById(R.id.mylistView);
        wifiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("Touchon", ((List<ScanResult>)myWifiList).get(position).SSID);
                txtRedeSelecionada.setText(((List<ScanResult>)myWifiList).get(position).SSID);

            }


        });
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},0 );
        }else{
            scanWifiList();
        }
    }

    private void scanWifiList() {
        boolean ok = wifiManager.startScan();
        myWifiList = wifiManager.getScanResults();
        Log.d("deucerto", Boolean.toString(ok));
        Log.d("Resultssss",Integer.toString(myWifiList.size())) ;
        setAdapter();
    }


    private void setAdapter() {
        listAdapter = new ListAdapter(getApplicationContext(), myWifiList);
        Log.d("Adapterrr",listAdapter.toString());
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
            Log.d("sucessoooo", Boolean.toString(success));
        }
    }

    private void scanSuccess() {
        List<ScanResult> results = wifiManager.getScanResults();

    }

    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        List<ScanResult> results = wifiManager.getScanResults();

    }
    private void sendConfigEsp(RequestQueue q, String url, JSONObject json){

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("name", "Alif");
                params.put("domain", "http://itsalif.info");

                return params;
            }
        };
        q.add(postRequest);
    }
}