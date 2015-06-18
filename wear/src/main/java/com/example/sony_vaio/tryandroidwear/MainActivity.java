package com.example.sony_vaio.tryandroidwear;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class MainActivity extends Activity {

    private TextView textTime,textBatt;
    BluetoothServerSocket btServerSocket;
    BluetoothSocket btSocket;
    BluetoothAdapter btAdapter;
    UUID MY_UUID;
    ThreadConnected myThreadConnected;
    ThreadBeConnected myThreadBeConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                textTime = (TextView) findViewById(R.id.watch_time);
                textBatt = (TextView) findViewById(R.id.watch_battery);
                textTime.setText("Hello");

                btAdapter = BluetoothAdapter.getDefaultAdapter();
                if(btAdapter==null)
                {
                    Log.e("wear","notsupported");
                    finish();
                }
                makeDiscoverable();
                myThreadBeConnected = new ThreadBeConnected();
                myThreadBeConnected.start();
            }
        });
        if(myThreadBeConnected!=null){
            myThreadBeConnected.cancel();
        }
        if(myThreadConnected!=null){
            myThreadConnected.cancel();
        }

    }

    private void makeDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        Log.e("Log", "Discoverable ");

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myThreadBeConnected!=null){
            myThreadBeConnected.cancel();
        }
    }

    private class ThreadBeConnected extends Thread {

        private BluetoothServerSocket bluetoothServerSocket = null;

        public ThreadBeConnected() {
            try {
                bluetoothServerSocket =
                        btAdapter.listenUsingRfcommWithServiceRecord(MY_UUID.toString(), MY_UUID);

                Log.e("log","Waiting\n"
                        + "bluetoothServerSocket :\n"
                        + bluetoothServerSocket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            BluetoothSocket bluetoothSocket = null;

            if(bluetoothServerSocket!=null){
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();

                    BluetoothDevice remoteDevice = bluetoothSocket.getRemoteDevice();

                    final String strConnected = "Connected:\n" +
                            remoteDevice.getName() + "\n" +
                            remoteDevice.getAddress();
                    Log.e("log",strConnected);

                    startThreadConnected(bluetoothSocket);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String eMessage = e.getMessage();
                    Log.e("log",eMessage);


                }
            }else{
                Log.e("log", "bts null");
            }
        }

        public void cancel() {

            Log.e("log","closing socket");


            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = connectedInputStream.read(buffer);

                    String strReceived = new String(buffer, 0, bytes);
                    final String msgReceived = String.valueOf(bytes) +
                            " bytes received:\n"
                            + strReceived;

                    if(strReceived.equals("get"))
                        write("188".getBytes());

                    Log.e("log",msgReceived);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    Log.e("log",msgConnectionLost);
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
                connectedOutputStream.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
}
