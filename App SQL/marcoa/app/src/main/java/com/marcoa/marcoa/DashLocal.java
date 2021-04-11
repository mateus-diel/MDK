package com.marcoa.marcoa;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;


public class DashLocal extends AppCompatActivity {
    private GridView grid;
    private NsdClient nsd;
    private Thread thread;
    private Handler mHandler;
    private Object mPauseLock;
    private boolean mPaused;
    private boolean mFinished;
    private GridAdapterLocal adapterLocal;
    private FloatingActionButton sair, btnNovoESP;
    private FloatingActionMenu menu;
    private ArrayList<String> dispositivos;
    private ArrayList<Drawable> drawables;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash);
        dispositivos = new ArrayList<>();
        drawables = new ArrayList<>();
        Drawable unwrappedDrawable;
        Drawable wrappedDrawable;
        unwrappedDrawable = AppCompatResources.getDrawable(DashLocal.this, R.drawable.ic_burn);
        wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(DashLocal.this, R.color.laranjalogo));
        drawables.add(wrappedDrawable);

        sair = findViewById(R.id.floatingSairLocal);
        btnNovoESP = findViewById(R.id.floatingNovoDispositivo);
        menu = findViewById(R.id.floatingMenuLocal);
        btnNovoESP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashLocal.this, ConfigureEsp.class);
                startActivity(intent);
                menu.close(true);
                finish();
            }
        });


        sair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent a = new Intent(DashLocal.this,Login.class);
                startActivity(a);
                finish();
            }
        });


        grid = findViewById(R.id.gridLayoutforESP);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                if(message.what == 1){
                    try {
                        JSONObject json = new JSONObject(message.obj.toString());
                        Iterator<String> iter = json.keys();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            try {
                                Object value = json.get(key);
                                JSONObject jso = new JSONObject(String.valueOf(value));
                                Log.d("message", jso.toString());
                                Log.d("Size dispo", Integer.toString(dispositivos.size()));


                                if(dispositivos.size()>0){
                                    boolean contain = false;
                                    for (String z : dispositivos){
                                        if(z.contains(jso.get("HostAddress").toString())){
                                            contain = true;
                                            break;
                                        }
                                    }
                                    if(!contain){
                                        dispositivos.add(jso.get("ServiceName").toString()+"*/*"+jso.get("HostAddress").toString());
                                    }
                                }else{
                                    dispositivos.add(jso.get("ServiceName").toString()+"*/*"+jso.get("HostAddress").toString());
                                    Log.d("Nomeee",dispositivos.get(0).substring(0,dispositivos.get(0).indexOf("*/*")));
                                    Log.d("ipppp",dispositivos.get(0).substring(dispositivos.get(0).indexOf("*/*")+3));
                                }

                            } catch (JSONException e) {
                                // Something went wrong!
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                adapterLocal = new GridAdapterLocal(DashLocal.this, dispositivos, drawables);
                grid.setAdapter(adapterLocal);
            }
        };

        mPauseLock = new Object();
        mPaused = false;
        mFinished = false;
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                menu.close(true);

                LoadingDialog dialog = new LoadingDialog(DashLocal.this);
                dialog.startLoadingDialog();
                Intent intent = new Intent(DashLocal.this, SetDataEsp.class);
                intent.putExtra("ip", ((Button)view.findViewById(R.id.btnGridLocal)).getTag().toString());
                intent.putExtra("nome", ((TextView)view.findViewById(R.id.text_view_local)).getText().toString());

                InetAddress in;
                in = null;
                try {
                    in = InetAddress.getByName(((Button)view.findViewById(R.id.btnGridLocal)).getTag().toString());
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                    if (in.isReachable(5000)) {
                        Log.d("oook respondeu","end");
                        startActivity(intent);
                        dialog.dimissDialog();
                        finish();
                    } else {
                        Log.d("nao responde","end");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        thread = new Thread(){
            public void run(){
                nsd = new NsdClient(getApplicationContext());
                nsd. initializeNsd();
                nsd.discoverServices();

                while (!mFinished) {
                    try {
                        Message message = mHandler.obtainMessage(1, nsd.getChosenServiceInfo().toString());
                        message.sendToTarget();
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
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

    @Override
    protected void onResume() {
        super.onResume();
        if(mPauseLock != null){
            synchronized (mPauseLock) {
                mPaused = false;
                mPauseLock.notifyAll();
            }
        }

    }

    @Override
    protected void onPause() {
        synchronized (mPauseLock) {
            mPaused = true;
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        synchronized (mPauseLock) {
            mPaused = true;
        }
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        synchronized (mPauseLock) {
            mPaused = true;
        }
        super.onBackPressed();
        finish();
    }
}

