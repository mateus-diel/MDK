package com.example.dashcontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;

import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;


public class DashLocal extends AppCompatActivity {
    Button novoESP;
    GridLayout grid;
    NsdClient nsd;
    Thread thread;
    Handler mHandler;
    private Object mPauseLock;
    private boolean mPaused;
    private boolean mFinished;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash);
        mPauseLock = new Object();
        mPaused = false;
        mFinished = false;
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();

                try {
                    JSONObject json = new JSONObject(bundle.getString("services"));
                    Iterator<String> iter = json.keys();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        try {
                            Object value = json.get(key);
                            JSONObject jso = new JSONObject(String.valueOf(value));

                            Display display = getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);
                            int width = size.x;
                            int height = size.y;
                            ArrayList<View> allButtons;
                            allButtons = ((GridLayout) findViewById(R.id.gridLayoutforESP)).getTouchables();
                            if(allButtons.size()>0){
                                boolean contain = false;
                                for(int i =0; i<allButtons.size(); i++){
                                    Button b = (Button) allButtons.get(i);
                                    if((jso.get("HostAddress").toString().equals(String.valueOf(b.getTag())))){

                                        contain = true;
                                        break;
                                    }
                                }
                                if(!contain){
                                    Button z = new Button(getApplicationContext());
                                    z.setMinHeight(200);
                                    z.setMinWidth((size.x-50)/3);
                                    z.setText(jso.get("ServiceName").toString());
                                    z.setTag(jso.get("HostAddress").toString());
                                    z.setOnClickListener(DashLocal.this::onClick);
                                    grid.addView(z);
                                }

                            }else{
                                Button z = new Button(getApplicationContext());
                                z.setMinHeight(200);
                                z.setMinWidth((size.x-50)/3);
                                z.setText(jso.get("ServiceName").toString());
                                z.setTag(jso.get("HostAddress").toString());
                                z.setOnClickListener(DashLocal.this::onClick);
                                grid.addView(z);
                            }

                        } catch (JSONException e) {
                            // Something went wrong!
                        }
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        novoESP = findViewById(R.id.btnNovoESP);
        grid = findViewById(R.id.gridLayoutforESP);


        thread = new Thread(){
            public void run(){
                nsd = new NsdClient(getApplicationContext());
                nsd. initializeNsd();
                nsd.discoverServices();

                while (!mFinished) {
                    // Do stuff.
                    try {
                        Message msg = mHandler.obtainMessage();
                        Bundle bundle = new Bundle();

                        bundle.putString("services",nsd.getChosenServiceInfo().toString());
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
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





        /*if (nsd.getChosenServiceInfo().size()>0){
            for(int i = 0; i<nsd.getChosenServiceInfo().size(); i++){

                Log.d("TAG", nsd.getChosenServiceInfo().get(i).toString());
                //mServiceAdapter.remove(bonjourService);
                Log.d("bnjourrr",nsd.getChosenServiceInfo().get(i).toString());
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;
                Log.d("wid",Integer.toString(size.x));
                Log.d("heig",Integer.toString(size.y));
                Log.d("TAG", nsd.getChosenServiceInfo().get(i).toString());
                JSONObject obj = new JSONObject();
                Button z = new Button(getApplicationContext());
                z.setMinHeight(200);
                z.setMinWidth((size.x-50)/3);
                z.setText(nsd.getChosenServiceInfo().get(i).getServiceName());
                z.setTag(nsd.getChosenServiceInfo().get(i).getHost());
                Log.d("hooossstttt", nsd.getChosenServiceInfo().get(i).toString());
                z.setOnClickListener(DashLocal.this::onClick);
                grid.addView(z);
            }

        }*/

        novoESP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ConfigureEsp.class);
                startActivity(intent);
            }
        });
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

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                String z;
            case R.id.item2:


        }
        return super.onOptionsItemSelected(item);
    }

    private void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), SetDataEsp.class);
        intent.putExtra("ip", v.getTag().toString());
        intent.putExtra("nome", String.valueOf(((Button) v).getText()));
        startActivity(intent);

    }

}

