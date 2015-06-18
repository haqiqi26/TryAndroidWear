package com.example.sony_vaio.tryandroidwear;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {
    private static final int REQUEST_ENABLE_BT =1;
    //Button onButton,offButton,listButton,findButton;
    Button findButton;
    BluetoothAdapter myBTAdapter;
    List<BluetoothDevice> pairedDevices;
    List<BluetoothDevice> newDevices;
    ListView listViewPaired,listViewNew;
    ArrayAdapter<String> BTPairedArrayAdapter,BTNewArrayAdapter;
    final int RECIEVE_MESSAGE=1;
    UUID MY_UUID;
    BluetoothSocket bluetoothSocket;
    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findButton= (Button)findViewById(R.id.findNew);
        listViewPaired = (ListView)findViewById(R.id.pairedDevices);
        listViewNew = (ListView)findViewById(R.id.newDevices);
        MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


        myBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBTAdapter == null)
        {

            Log.e("mobileApp","not supported");
        }
        else
        {
            findButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    findBT();
                }
            });
            BTPairedArrayAdapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_expandable_list_item_1);
            BTNewArrayAdapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_expandable_list_item_1);

            TextView textView = new TextView(MainActivity.this);
            textView.setText("Paired Devices");

            listViewPaired.addHeaderView(textView);
            listViewPaired.setAdapter(BTPairedArrayAdapter);

            TextView textView2 = new TextView(MainActivity.this);
            textView2.setText("New Devices");

            listViewNew.addHeaderView(textView2);

            listViewNew.setAdapter(BTNewArrayAdapter);
            /*listViewNew.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    BluetoothDevice device = pairedDevices.get(position);
                    Toast.makeText(MainActivity.this,
                            "Name: " + device.getName() + "\n"
                                    + "Address: " + device.getAddress() + "\n"
                                    + "BondState: " + device.getBondState() + "\n"
                                    + "BluetoothClass: " + device.getBluetoothClass() + "\n"
                                    + "Class: " + device.getClass(),
                            Toast.LENGTH_LONG).show();

                    Log.e("log", "start ThreadConnectBTdevice");
                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
                    myThreadConnectBTdevice.start();

                }
            });*/
            listViewPaired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    BluetoothDevice device = pairedDevices.get(position-1);
                    Toast.makeText(MainActivity.this,
                            "Name: " + device.getName() + "\n"
                                    + "Address: " + device.getAddress() + "\n"
                                    + "BondState: " + device.getBondState() + "\n"
                                    + "BluetoothClass: " + device.getBluetoothClass() + "\n"
                                    + "Class: " + device.getClass(),
                            Toast.LENGTH_LONG).show();

                    Log.e("log","start ThreadConnectBTdevice");
                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
                    myThreadConnectBTdevice.start();
                }
            });
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        turnOnBT();
        findBT();
        listPairedDevices();
    }

    private class ThreadConnectBTdevice extends Thread {
        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;


        public ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.e("log","bluetoothSocket: \n" + bluetoothSocket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                Log.e("log",eMessage);

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(success){
                //connect successful
                final String msgconnected = "connect successful:\n"
                        + "BluetoothSocket: " + bluetoothSocket + "\n"
                        + "BluetoothDevice: " + bluetoothDevice;
                Log.e("log",msgconnected);

                startThreadConnected(bluetoothSocket);
            }else{
                //fail
                Log.e("log","failed to connect");
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
        myThreadConnected.write("hello".getBytes());
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
                    Log.e("log",msgReceived);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    Log.e("log", msgConnectionLost);
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
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


    private void makeDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        Log.e("Log", "Discoverable ");

    }

    public void turnOnBT()
    {
        if(!myBTAdapter.isEnabled())
        {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
            Toast.makeText(MainActivity.this, "Bluetooth turned on", Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(MainActivity.this,"Bluetooth is already on",Toast.LENGTH_LONG).show();
            makeDiscoverable();
        }
    }

    public void listPairedDevices()
    {
        pairedDevices = new ArrayList<>(myBTAdapter.getBondedDevices());
        for(BluetoothDevice device:pairedDevices) {
            BTPairedArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            Log.e("paired","addr: "+device.getAddress()+" name: "+device.getName()+" uuid: "+device.getUuids());
            /*ParcelUuid[] test = device.getUuids();
            for(int i=0;i<test.length;i++)
            {
                Log.e(device.getName(), "uuid: "+test[i].getUuid().toString());
            }*/

        }
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
                int newBTcount = newDevices.size();
                boolean flag = false;
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(newBTcount==0)
                {
                    newDevices.add(device);
                    BTNewArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    BTNewArrayAdapter.notifyDataSetChanged();
                }
                else if(newBTcount>0)
                {
                    for(BluetoothDevice bt:newDevices)
                    {
                        if(!bt.getAddress().equals(device.getAddress()))
                        {
                            flag=true;
                        }
                    }
                    if(flag)
                    {
                        newDevices.add(device);
                        BTNewArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                        BTNewArrayAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    public void findBT()
    {
        if(myBTAdapter.isDiscovering())
            myBTAdapter.cancelDiscovery();
        else
        {
            BTNewArrayAdapter.clear();
            newDevices = new ArrayList<>();
            myBTAdapter.startDiscovery();
            registerReceiver(receiver,new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    }

    public void turnOffBT()
    {
        myBTAdapter.disable();
        Toast.makeText(MainActivity.this,"Bluetooth turned off",Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if(myThreadConnectBTdevice!=null){
            myThreadConnectBTdevice.cancel();
        }
        turnOffBT();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_ENABLE_BT)
        {
            if(myBTAdapter.isEnabled()) {
                //statusBT.setText("Status: Enabled");
                makeDiscoverable();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
