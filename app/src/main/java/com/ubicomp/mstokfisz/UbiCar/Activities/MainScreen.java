package com.ubicomp.mstokfisz.UbiCar.Activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.ubicomp.mstokfisz.UbiCar.R;
import com.ubicomp.mstokfisz.UbiCar.Services.UbiCarService;
import com.ubicomp.mstokfisz.UbiCar.Services.UbiCarService.MyBinder;
import com.ubicomp.mstokfisz.UbiCar.UbiCar;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainScreen extends AppCompatActivity {

    private static UbiCar app = null;
    private Button resetButton = null;
    private Button startButton = null;
    private Button connectButton = null;
    public TextView mafValue = null;
    public TextView speedValue = null;
    public TextView fuelConsumptionValue = null;
    public TextView distanceValue = null;
    public TextView timeValue = null;
    private String deviceAddress = null;
    private boolean isStarted = false;
    public BluetoothSocket socket = null;
    private UbiCarService mUbiCarService;
    private boolean mUbiCarServiceBound = false;


    private double AFR = 0.0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = ((UbiCar)getApplicationContext());
        app.setActiveMainScreen(this);

        setContentView(R.layout.relative_main_layout);
        connectButton = findViewById(R.id.connectBtn);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUbiCarService != null && mUbiCarService.isConnected) {
                    Toast.makeText(getApplicationContext(), "Device already connected!",Toast.LENGTH_SHORT).show();
                }
                ArrayList deviceStrs = new ArrayList();
                final ArrayList devices = new ArrayList();

                final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

                if (btAdapter == null) {
                    // Add deviceNotSupportedException
                    Toast.makeText(getApplicationContext(),"Bluetooth Not Supported",Toast.LENGTH_SHORT).show();
                } else if (!btAdapter.isEnabled()) {
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
                }
                else {
                    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            deviceStrs.add(device.getName() + "\n" + device.getAddress());
                            devices.add(device.getAddress());
                        }
                    }

                    // show list
                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainScreen.this);

                    ArrayAdapter adapter = new ArrayAdapter(MainScreen.this, android.R.layout.select_dialog_singlechoice,
                            deviceStrs.toArray(new String[deviceStrs.size()]));

                    alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            deviceAddress = (String) devices.get(position);
                            Log.d("Bluetooth", deviceAddress);
                            bluetoothConnect(btAdapter);
                        }
                    });
                    alertDialog.setTitle("Choose Bluetooth device");
                    alertDialog.show();
                }
            }
        });
        resetButton = findViewById(R.id.resetBtn);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isStarted) {
                    isStarted = false;
                    startButton.setText("Start");
                    Log.d("MainView", "OBD stopped!");
                    if (mUbiCarServiceBound) {
                        unbindService(mServiceConnection);
                        stopService(new Intent(MainScreen.this, UbiCarService.class));
                        mUbiCarServiceBound = false;
                    }
                }
                resetValues();
            }
        });
        startButton = findViewById(R.id.startBtn);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MainView", "Start clicked!");
                if (!isStarted) {
                    final Intent intent = new Intent(MainScreen.this, UbiCarService.class);
                    startService(intent);
                    bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
//                    mUbiCarService.setSocket(socket);
                    Log.d("MainView", "Service bound: "+mUbiCarServiceBound);
                    startButton.setText("Stop");
                    Log.d("MainView", "OBD command sent!");
                    isStarted = true;
                    // Check if it was executed
                }
                else if (mUbiCarServiceBound) {
                    startButton.setText("Start");
                    Log.d("MainView", "OBD stopped!");
                    mUbiCarServiceBound = false;
                    isStarted = false;
                    unbindService(mServiceConnection);
                    stopService(new Intent(MainScreen.this, UbiCarService.class));
                }
            }
        });
        mafValue = findViewById(R.id.mafValue);
        speedValue = findViewById(R.id.speedValue);
        fuelConsumptionValue = findViewById(R.id.fuelConsumptionValue);
        distanceValue = findViewById(R.id.distanceValue);
        timeValue = findViewById(R.id.timeValue);
        resetValues();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        app.setActiveMainScreen(this);
        Intent intent = new Intent(MainScreen.this, UbiCarService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        app.removeActiveMainScreen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w("Main", "Should pause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w("Main", "Should resume");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            Toast.makeText(getApplicationContext(),"Bluetooth Turned ON",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(),"Couldn't turn on Bluetooth",Toast.LENGTH_SHORT).show();
        }
    }

    private void bluetoothConnect(BluetoothAdapter btAdapter) {
        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
        UUID uuid = device.getUuids()[0].getUuid();

        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            socket.connect();
        } catch (Exception e) {
            Log.e("Bluetooth", e.getMessage());
            Toast.makeText(getApplicationContext(), "Couldn't connect to the device!", Toast.LENGTH_LONG).show();
            //Throw exception
        }
        Toast.makeText(getApplicationContext(), device.getName() + " connected!", Toast.LENGTH_LONG).show();
    }

    private void resetValues() {
        speedValue.setText("0 km/h");
        mafValue.setText(Double.toString(AFR) + " g/s");
        fuelConsumptionValue.setText("0.0");
        distanceValue.setText("0 km");
        timeValue.setText("0:00:00");
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mUbiCarServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("ServiceConnection", "Connecting");
            MyBinder myBinder = (MyBinder) service;
            mUbiCarService = myBinder.getService();
            mUbiCarServiceBound = true;
        }
    };
}
