package com.ubicomp.mstokfisz.UbiCar.Services;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;
import com.github.pires.obd.commands.*;
import com.github.pires.obd.commands.engine.*;
import com.github.pires.obd.commands.protocol.*;
import com.github.pires.obd.enums.ObdProtocols;
import com.ubicomp.mstokfisz.UbiCar.Activities.MainScreen;
import com.ubicomp.mstokfisz.UbiCar.DataClasses.Data;
import com.ubicomp.mstokfisz.UbiCar.DataClasses.Trip;

import java.text.DecimalFormat;
import java.util.Locale;


public class ObdHandler {
    private BluetoothSocket socket;
    public boolean isConnected = false;
    private MainScreen mainScreen;
    public ObdDataGainer obdDataGainer = null;


    public ObdHandler (BluetoothSocket socket, MainScreen mainScreen) {
        this.socket = socket;
        this.mainScreen = mainScreen;
    }

    public void start(Data data) {
        this.obdDataGainer = new ObdDataGainer(data.getCurrentTrip());
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
        private Trip trip;

        private long numberOfCalculations;
        private double distance;
        private int avgSpeed;
        private long speedSum;
        private double avgMaf;
        private long workingTime;
        private long realTime = 0;
        private long previousTime;

        private long startTime = 0;
        private long startCycleTime = 0;
        private long currentTime;
        private final double dieselDensity = 0.83;
        private final double petrolDensity = 0.755;
        private final double airDieselRatio = 14.5;
        private final double airPetrolRatio = 14.7;

        private ObdDataGainer(Trip trip) {
            this.trip = trip;
            this.numberOfCalculations = trip.getNumberOfCalculations();
            this.distance = trip.getDistance();
            this.avgSpeed = trip.getAvgSpeed();
            this.speedSum = trip.getSpeedSum();
            this.avgMaf = trip.getAvgMaf();
            this.workingTime = trip.getWorkingTime();
            this.previousTime = trip.getTravelTime();
            this.execute();
        }

        public void finish() {
            obdDataGainer.cancel(true);
            trip.setAvgMaf(avgMaf);
            trip.setAvgSpeed(avgSpeed);
            trip.setDistance(distance);
            trip.setNumberOfCalculations(numberOfCalculations);
            trip.setSpeedSum(speedSum);
            trip.setWorkingTime(workingTime);
            trip.setTravelTime(realTime);
        }

        private void getDataFromDevice() {
            MassAirFlowCommand mafCommand = new MassAirFlowCommand();
            SpeedCommand speedCommand = new SpeedCommand();
            numberOfCalculations = 0;
            startTime = System.currentTimeMillis();
            startCycleTime = System.currentTimeMillis();
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
                int currentSpeed = speedCommand.getMetricSpeed();

                speedSum += currentSpeed;
                numberOfCalculations++;
                avgSpeed = (int)(speedSum/numberOfCalculations);
                calculateTimeDistance(currentSpeed);
                String formattedRealTime = formatTime(realTime);
                String fuelConsumption = getFormattedFuelConsumption();
                String distance = getFormattedDistance();
                publishProgress(mafCommand.getFormattedResult(), speedCommand.getFormattedResult(), distance, formattedRealTime, fuelConsumption);
                Log.d("OBD", "MAF: " + mafCommand.getFormattedResult());
                Log.d("OBD", "Speed: " + speedCommand.getFormattedResult());
                Log.d("OBD", "l/100km: " + fuelConsumption);
                Log.d("OBD", "Distance: " + distance);
                Log.d("OBD", "Real time: "+ formattedRealTime);
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

        private void calculateTimeDistance(int currentSpeed) {
            currentTime = (System.currentTimeMillis()- startCycleTime);
            startCycleTime = System.currentTimeMillis();
            realTime = (System.currentTimeMillis()- startTime) + previousTime;
            workingTime += currentTime;
            distance += (double)currentSpeed * (currentTime /(1000*3600.0));
        }

        private String formatTime(long time) {
            long second = (time / 1000) % 60;
            long minute = (time / (1000 * 60)) % 60;
            long hour = time / (1000 * 60 * 60);
            return String.format(Locale.ENGLISH,"%02d:%02d:%02d", hour, minute, second);
        }

        private double calculateFuelConsumption() {
            return ((avgMaf * 3600)/airPetrolRatio * petrolDensity * (workingTime /3600000.0)) / (distance * 100.0);
        }

        private String getFormattedFuelConsumption() {
            return new DecimalFormat("#.0#").format(calculateFuelConsumption());
        }

        private String getFormattedDistance() {
            return new DecimalFormat("#.0#").format(distance);
        }
    }
}
