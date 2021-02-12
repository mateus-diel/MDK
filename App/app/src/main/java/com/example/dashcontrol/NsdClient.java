package com.example.dashcontrol;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class NsdClient {
    //estou testando o git
    private Context mContext;

    private NsdManager mNsdManager;
    NsdManager.DiscoveryListener mDiscoveryListener;

    //To find all the available networks SERVICE_TYPE = "_services._dns-sd._udp"
    public static final String SERVICE_TYPE = "_dimmer._tcp.";
    //public static final String SERVICE_TYPE = "_services._dns-sd._udp";
    public static final String TAG = "NsdClient";

    private static ArrayList<NsdServiceInfo> ServicesAvailable = new ArrayList<>();

    public NsdClient(Context context) {
        Log.d("tamanho array", String.valueOf(ServicesAvailable.size()));
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNsd() {
        initializeDiscoveryListener();
    }

    public void initializeDiscoveryListener() {
        Log.d(TAG, "Inicializando ");
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started " + regType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success " + service);

                if (service.getServiceType().equals(SERVICE_TYPE)) {
                    mNsdManager.resolveService(service, new initializeResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                if (ServicesAvailable.equals(service)) {
                    ServicesAvailable = null;
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public class initializeResolveListener implements NsdManager.ResolveListener {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed " + errorCode);
            switch (errorCode) {
                case NsdManager.FAILURE_ALREADY_ACTIVE:
                    Log.e(TAG, "FAILURE ALREADY ACTIVE");
                    mNsdManager.resolveService(serviceInfo, new initializeResolveListener());
                    break;
                case NsdManager.FAILURE_INTERNAL_ERROR:
                    Log.e(TAG, "FAILURE_INTERNAL_ERROR");
                    break;
                case NsdManager.FAILURE_MAX_LIMIT:
                    Log.e(TAG, "FAILURE_MAX_LIMIT");
                    break;
            }
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
            Log.d(TAG, "Achei vc safadim: " + serviceInfo.getServiceType());
            ServicesAvailable.add(serviceInfo);

        }
    }

    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public List<NsdServiceInfo> getChosenServiceInfo() {
        return ServicesAvailable;
    }

    public void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    private void addServiceArray(NsdServiceInfo service) {
        if(ServicesAvailable.size()>0){
            for (int i = 0; i<ServicesAvailable.size();i++){
                if(!(ServicesAvailable.get(i).getServiceName().equals(service.getServiceName()))){
                    ServicesAvailable.add(service);
                }
            }
        }else{
            ServicesAvailable.add(service);
        }
    }
}