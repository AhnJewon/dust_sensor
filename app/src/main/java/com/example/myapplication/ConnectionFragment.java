package com.example.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.jar.Manifest;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


public class ConnectionFragment extends Fragment {

    String TAG = "MainActivity";
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    TextView textStatus;
    Button btnParied, btnSearch, btnSend;
    ListView listView;
    private Retrofit retrofit;

    BluetoothAdapter  btAdapter;
    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    private final static int REQUEST_ENABLE_BT = 1;
    BluetoothSocket btSocket = null;
    ConnectedThread connectedThread;

    MainActivity mainActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) getActivity();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        btAdapter = mainActivity.blead;
        if(!btAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_connection, container, false);

        textStatus = (TextView) rootView.findViewById(R.id.text_status);
        btnParied = (Button) rootView.findViewById(R.id.btn_paired);
        btnSend = (Button) rootView.findViewById(R.id.btn_send);
        listView = (ListView) rootView.findViewById(R.id.listview);

        btArrayAdapter = new ArrayAdapter<>(mainActivity, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

        listView.setOnItemClickListener(new myOnItemClickListener());

        btnParied.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btArrayAdapter.clear();
                if(deviceAddressArray!=null && !deviceAddressArray.isEmpty()){ deviceAddressArray.clear(); }
                pairedDevices = btAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        btArrayAdapter.add(deviceName);
                        deviceAddressArray.add(deviceHardwareAddress);

                    }
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connectedThread!=null){ connectedThread.write("a"); }
                try {
                    mainActivity.start();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        });

        return rootView;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }

    // Create a BroadcastReceiver for ACTION_FOUND.

    public class myOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Toast.makeText(mainActivity.getApplicationContext(), btArrayAdapter.getItem(position), Toast.LENGTH_SHORT).show();

            textStatus.setText("try...");

            final String name = btArrayAdapter.getItem(position); // get name
            final String address = deviceAddressArray.get(position); // get address
            boolean flag = true;

            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            // create & connect socket
            try {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                textStatus.setText("connection failed!");
                e.printStackTrace();
            }

            try {
                btSocket.connect();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
            }
            // start bluetooth communication
            connectedThread = new ConnectedThread(btSocket);
            textStatus.setText("connected to" + name);
            connectedThread.start();
        }


    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }
    private Boolean sendData(postdata postjson) {

        {
            Gson gson = new GsonBuilder().setLenient().create();

            retrofit = new Retrofit.Builder()
                    .baseUrl("http://203.255.81.72:10021//")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            comm_data service = retrofit.create(comm_data.class);
            Call<String> call = null;
            final String[] callback = new String[1];

            if (mainActivity.dust_sensorMac.contains(postjson.get_mac())) {
                call = service.sensing(postjson.get_sensor(),
                        postjson.get_mode(),
                        postjson.get_mac(),
                        postjson.get_receiver(),
                        postjson.get_time(),
                        postjson.get_otp(),
                        postjson.get_key(),
                        postjson.get_data());

            }

            if (mainActivity.air_sensorMac.contains(postjson.get_mac())) {
                call = service.sensing(postjson.get_sensor(),
                        postjson.get_mode(),
                        postjson.get_mac(),
                        postjson.get_receiver(),
                        postjson.get_time(),
                        postjson.get_otp(),
                        postjson.get_key(),
                        postjson.get_data());

            }
}

