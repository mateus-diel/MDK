package com.example.dashcontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;

import com.github.druk.rx2dnssd.Rx2Dnssd;
import com.github.druk.rx2dnssd.Rx2DnssdEmbedded;
import com.github.druk.rxdnssd.BonjourService;
import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdEmbedded;

import org.json.JSONObject;

import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/*
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
*/
public class DashLocal extends AppCompatActivity {
    Button novoESP;
    //RxDnssd rxDnssd;
    Rx2Dnssd rxdnssd;
    GridLayout grid;
    NsdClient nsd;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash);
        novoESP = findViewById(R.id.btnNovoESP);
        grid = findViewById(R.id.gridLayoutforESP);
        //rxdnssd = new Rx2DnssdEmbedded(this);
        nsd = new NsdClient(this);
        nsd. initializeNsd();
        nsd.discoverServices();
        if (nsd.getChosenServiceInfo().size()>0){
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

        }





        /*Disposable browseDisposable = rxdnssd.browse("_dimmer._tcp", "local.")
                .compose(rxdnssd.resolve())
                .compose(rxdnssd.queryRecords())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bonjourService -> {
                    Log.d("TAG", bonjourService.toString());
                    //mServiceAdapter.remove(bonjourService);
                    Log.d("bnjourrr",bonjourService.toString());
                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int width = size.x;
                    int height = size.y;
                    Log.d("wid",Integer.toString(size.x));
                    Log.d("heig",Integer.toString(size.y));
                    Log.d("TAG", bonjourService.toString());
                    JSONObject obj = new JSONObject();
                    Button z = new Button(getApplicationContext());
                    z.setMinHeight(200);
                    z.setMinWidth((size.x-50)/3);
                    z.setText(bonjourService.getServiceName());
                    z.setTag(bonjourService.getInet4Address().toString());
                    z.setOnClickListener(DashLocal.this::onClick);
                    grid.addView(z);
                }, throwable -> Log.e("TAG", "error", throwable));


*/

/*

                rxDnssd= new RxDnssdEmbedded(this);
        Subscription subscription = rxDnssd.browse("_dimmer._tcp", "local.")
                .compose(rxDnssd.resolve())
                .compose(rxDnssd.queryRecords())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<BonjourService>() {
                    @Override
                    public void call(BonjourService bonjourService) {
                        Display display = getWindowManager().getDefaultDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        int width = size.x;
                        int height = size.y;
                        Log.d("wid",Integer.toString(size.x));
                        Log.d("heig",Integer.toString(size.y));
                        Log.d("TAG", bonjourService.toString());
                        JSONObject obj = new JSONObject();
                        Button z = new Button(getApplicationContext());
                        z.setMinHeight(200);
                        z.setMinWidth((size.x-50)/3);
                        z.setText(bonjourService.getServiceName());
                        z.setTag(bonjourService.getInet4Address().toString());
                        z.setOnClickListener(DashLocal.this::onClick);
                        grid.addView(z);


                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e("TAG", "error", throwable);
                    }
                });
*/


        novoESP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ConfigureEsp.class);
                startActivity(intent);
            }
        });
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
        Log.d("onclickkk", v.getTag().toString());
        Log.d("onclickkk", String.valueOf(((Button) v).getText()));
        Intent intent = new Intent(getApplicationContext(), SetDataEsp.class);
        intent.putExtra("ip", v.getTag().toString());
        intent.putExtra("nome", String.valueOf(((Button) v).getText()));
        startActivity(intent);

    }
}

