package com.ubicomp.mstokfisz.UbiCar.Services;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;
import com.github.pires.obd.commands.*;
import com.github.pires.obd.commands.engine.*;
import com.github.pires.obd.commands.protocol.*;
import com.github.pires.obd.enums.ObdProtocols;
import com.ubicomp.mstokfisz.UbiCar.Activities.MainScreen;


public class ObdHandler {
    private BluetoothSocket socket;
    public boolean isConnected = false;
    private MainScreen mainScreen = null;
    public ObdDataGainer obdDataGainer = null;


    public ObdHandler (BluetoothSocket socket, MainScreen mainScreen) {
        this.socket = socket;
        this.mainScreen = mainScreen;
        obdDataGainer = new ObdDataGainer();
    }


    public void setupObd() {
        try {
            new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());

            new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());

            new TimeoutCommand(50).run(socket.getInputStream(), socket.getOutputStream());

            new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
        } catch (Exception e) {
            Log.e("OBD", e.getMessage());
        }
        isConnected = true;
    }

    public class ObdDataGainer extends AsyncTask<Void, String, String> {
        private long startTime = 0;
        private long numberOfCalculations;
        private double distance = 0;
        private int avgSpeed = 0;
        private long speedSum = 0;
        private double avgMaf = 0;
        private long time;
        private final double dieselDensity = 0.83;
        private final double petrolDensity = 0.755;
        private final double airDieselRatio = 14.5;
        private final double airPetrolRatio = 14.7;
        public void getDataFromDevice() {
            MassAirFlowCommand mafCommand = new MassAirFlowCommand();
            SpeedCommand speedCommand = new SpeedCommand();
            startTime = System.currentTimeMillis();
            numberOfCalculations = 0;
            while (!Thread.currentThread().isInterrupted() && mainScreen.isStarted)
            {
                try {
                    mafCommand.run(socket.getInputStream(), socket.getOutputStream());
                    speedCommand.run(socket.getInputStream(), socket.getOutputStream());
                } catch (Exception e) {
                    Log.e("OBD", e.getMessage());
                    mainScreen.isStarted = false;
                    isConnected = false;
                }
                if(numberOfCalculations == 0) {
                    avgMaf = mafCommand.getMAF();
                }
                else {
                    avgMaf = (avgMaf + mafCommand.getMAF()) / 2;
                }
                speedSum += speedCommand.getMetricSpeed();
                numberOfCalculations++;
                avgSpeed = (int)(speedSum/numberOfCalculations);
                calculateTimeDistance();
                String formattedTime = formatTime();
                String fuelConsumption = getFormattedFuelConsumption();
                publishProgress(mafCommand.getFormattedResult(), speedCommand.getFormattedResult(), Double.toString(distance), formattedTime, fuelConsumption);
                Log.d("OBD", "MAF: " + mafCommand.getFormattedResult());
                Log.d("OBD", "Speed: " + speedCommand.getFormattedResult());
                Log.d("OBD", "l/100km: " + fuelConsumption);
            }
        }

        @Override
        protected String doInBackground(Void... voids) {
            getDataFromDevice();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... text) {
            mainScreen.mafValue.setText(text[0]);
            mainScreen.speedValue.setText(text[1]);
            mainScreen.distanceValue.setText(text[2]+" km");
            mainScreen.timeValue.setText(text[3]);
            mainScreen.fuelConsumptionValue.setText(text[4]);
        }

        public void finish() {
            obdDataGainer = new ObdDataGainer();
        }

        private void calculateTimeDistance() {
            time = (System.currentTimeMillis()-startTime);
            distance = (double)Math.round((double)avgSpeed * (double)(time/(10*3600)))/100;
            Log.d("OBD","Distance: " + distance);
        }

        private String formatTime() {
            long second = (time / 1000) % 60;
            long minute = (time / (1000 * 60)) % 60;
            long hour = time / (1000 * 60 * 60);

            String formattedTime = String.format("%02d:%02d:%02d", hour, minute, second);
            Log.d("Calc", "Time: "+formattedTime);
            return formattedTime;
        }

        private double calculateFuelConsumption() {
            return ((avgMaf * 3600)/airPetrolRatio * petrolDensity * (time/3600000)) / (distance * 100);
        }

        private String getFormattedFuelConsumption() {
            return String.format("%02d", calculateFuelConsumption());
        }
    }
}
