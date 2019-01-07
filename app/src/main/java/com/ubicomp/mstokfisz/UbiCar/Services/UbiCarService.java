package com.ubicomp.mstokfisz.UbiCar.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.ubicomp.mstokfisz.UbiCar.Activities.MainScreen;
import com.ubicomp.mstokfisz.UbiCar.DataClasses.Car;
import com.ubicomp.mstokfisz.UbiCar.DataClasses.Data;
import com.ubicomp.mstokfisz.UbiCar.DataClasses.Passenger;
import com.ubicomp.mstokfisz.UbiCar.DataClasses.Trip;
import com.ubicomp.mstokfisz.UbiCar.Exceptions.CarIncompatibleException;
import com.ubicomp.mstokfisz.UbiCar.OBDCommands.O2LambdaCommand;
import com.ubicomp.mstokfisz.UbiCar.R;
import com.ubicomp.mstokfisz.UbiCar.UbiCar;
import com.ubicomp.mstokfisz.UbiCar.Utils.FuelType;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

import static com.ubicomp.mstokfisz.UbiCar.UbiCar.CHANNEL_ID;

public class UbiCarService extends Service {
    private static UbiCar app = null;
    private static String LOG_TAG = "ObdService";
    private BluetoothSocket socket = null;
    public boolean isConnected = false;
    private MainScreen mainScreen = null;
    private ObdDataGainer obdDataGainer = null;
    private Data data = null;
    private IBinder mBinder = new MyBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        app = (UbiCar) getApplicationContext();
        data = app.dataHandler.getData();
        initializeTestData();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("Calculating fuel consumption..");

        Log.d("OBDService", "Started");

        Intent notificationIntent = new Intent(this, MainScreen.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("UbiCar")
                .setContentText(input)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "Destroy");
        obdDataGainer.finish();
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "in onBind");
        mainScreen = app.getActiveMainScreen();
        socket = mainScreen.socket;
        start();
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(LOG_TAG, "in onRebind");
        mainScreen = app.getActiveMainScreen();
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(LOG_TAG, "in onUnbind");
        mainScreen = null;
        return true;
    }

    public void start() {
        this.obdDataGainer = new ObdDataGainer(data.getCurrentTrip());
    }

    private void initializeTestData() {
        data = new Data();
//        data.addCar(new Car("Mitsubishi Outlander", FuelType.DIESEL, 1968));
//        data.addCar(new Car("Peugeot 207", FuelType.PETROL, 1397));
        data.addCar(new Car("Seicento", FuelType.PETROL, 1108));
        data.addPassenger(new Passenger("Tester"));
        data.addTrip(new Trip("Trip 1", data.getCars().get(data.getCars().size()-1), data.getPassengers()));
        app.dataHandler.saveData(data);
    }

    public class ObdDataGainer extends AsyncTask<Void, String, String> {
        private Trip trip;

        private long numberOfCalculations;
        private double distance;
        private int avgSpeed;
        private long speedSum;
        private double fuelConsumptionSum = 0;
//        private long workingTime;
        private long realTime = 0;
        private long previousTime;
        private final FuelType fuelType;


        private long startTime = 0;
        private long startCycleTime = 0;
        private final double dieselDensity = 832;
        private final double petrolDensity = 745; // g/l
        private final double airDieselRatio = 14.5;
        private final double airPetrolRatio = 14.7;

        private long currentTime;
        private double currentMaf = 0;
        private double currentMff;
        private double currentAfr;
        private int currentSpeed = 0;
        private double currentFuelConsumption;
        private double currentLitresPer100Km;

        private final MassAirFlowCommand mafCommand;
        private final SpeedCommand speedCommand;
        private O2LambdaCommand lambdaCommand = null;
        private final RPMCommand rpmCommand;
        private final IntakeManifoldPressureCommand intakeManifoldPressureCommand;
        private final AirIntakeTemperatureCommand airIntakeTemperatureCommand;
        private int compatibilityMode = 1;

        private ObdDataGainer(Trip trip) {
            this.trip = trip;
            this.numberOfCalculations = trip.getNumberOfCalculations();
            this.distance = trip.getDistance();
            this.avgSpeed = trip.getAvgSpeed();
            this.speedSum = trip.getSpeedSum();
            this.fuelConsumptionSum = trip.getFuelConsumptionSum();
//            this.workingTime = trip.getWorkingTime();
            this.previousTime = trip.getTravelTime();

            this.mafCommand = new MassAirFlowCommand();
            this.speedCommand = new SpeedCommand();
            this.airIntakeTemperatureCommand = new AirIntakeTemperatureCommand();
            this.rpmCommand = new RPMCommand();
            this.intakeManifoldPressureCommand = new IntakeManifoldPressureCommand();
            this.fuelType = trip.getCar().getFuelType();
            if (fuelType == FuelType.DIESEL) {
                lambdaCommand = new O2LambdaCommand();
            }
            this.execute();
        }

        public void finish() {
            obdDataGainer.cancel(true);
            trip.setAvgSpeed(avgSpeed);
            trip.setDistance(distance);
            trip.setNumberOfCalculations(numberOfCalculations);
            trip.setSpeedSum(speedSum);
//            trip.setWorkingTime(workingTime);
            trip.setTravelTime(realTime);
            trip.setFuelConsumptionSum(fuelConsumptionSum);
        }

        private void getDataFromDevice() {
            numberOfCalculations = 0;
            startTime = System.currentTimeMillis();
            startCycleTime = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted() && isConnected)
            {
                Log.d("OBD", "Connection: " + isConnected);
                numberOfCalculations++;
                try {
                    speedCommand.run(socket.getInputStream(), socket.getOutputStream());
                    if (compatibilityMode == 1) {
                        mafCommand.run(socket.getInputStream(), socket.getOutputStream());
                        currentMaf = mafCommand.getMAF();
                    }
                    else {
                        /*
                        IMAP = RPM * MAP / IAT
                            RPM
                            MAP - Manifold Absolute Pressure in kPa
                            IAT - Intake Air Temperature in Kelvin
                        MAF = (IMAP/120)*(VE/100)*(ED)*(MM)/(R)
                            R - Specific Gas Constant (8.314472 J/(mol.K)
                            MM - Average molecular mass of air (28.9644 g/mol)
                            VE - volumetric efficiency (most modern cars have a value of 80% to 85% - we use 85% in the app)
                            ED - Engine Displacement in liters
                         */
                        rpmCommand.run(socket.getInputStream(), socket.getOutputStream());
                        intakeManifoldPressureCommand.run(socket.getInputStream(), socket.getOutputStream());
                        airIntakeTemperatureCommand.run(socket.getInputStream(), socket.getOutputStream());
                        currentMaf = ((rpmCommand.getRPM() * intakeManifoldPressureCommand.getMetricUnit() / airIntakeTemperatureCommand.getKelvin()) / 120) * 0.85 * ((double) trip.getCar().getEngineSize() / 1000) * 28.9644 / 8.314472;
                    }
                } catch (Exception e) {
                    mainScreen.startButton.setText("START");
//                    Toast.makeText(getApplicationContext(), "Connection problem!", Toast.LENGTH_SHORT).show();
                    Log.e("OBD", e.getMessage());
                    isConnected = false;
                }
                currentSpeed = speedCommand.getMetricSpeed();
                speedSum += currentSpeed;

                avgSpeed = (int)(speedSum/numberOfCalculations);
                calculateTimeDistance(currentSpeed);
                String formattedRealTime = formatTime(realTime);
                String fuelConsumption = getFormattedFuelConsumption();
                String distance = getFormattedDistance();
                publishProgress(currentMaf+" g/s", speedCommand.getFormattedResult(), distance, formattedRealTime, fuelConsumption, formatConsumedFuel());
//                Log.d("OBD", "MAF: " + currentMaf + " g/s");
//                Log.d("OBD", "Speed: " + speedCommand.getFormattedResult());
                Log.d("OBD", "l/100km: " + fuelConsumption);
//                Log.d("OBD", "Distance: " + distance);
//                Log.d("OBD", "Real time: "+ formattedRealTime);
                Log.d("OBD", "Consumed fuel: " + fuelConsumptionSum +" l");
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getApplicationContext(), "Performing compatibility check..", Toast.LENGTH_SHORT).show();
            try {
                new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());

                new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());

//                new TimeoutCommand(50).run(socket.getInputStream(), socket.getOutputStream());

                new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Connection problem!", Toast.LENGTH_SHORT).show();
                Log.e("OBD", e.getMessage());
                compatibilityMode = 0;
                return;
            }
            // Check compatibility
            try {
//                new TimeoutCommand(50).run(socket.getInputStream(), socket.getOutputStream());
                speedCommand.run(socket.getInputStream(), socket.getOutputStream());
                mafCommand.run(socket.getInputStream(), socket.getOutputStream());

                if (mafCommand.getMAF() + 1.0f < 0.001f) {
                    throw new CarIncompatibleException("MAF command not supported");
                }

                if (fuelType == FuelType.DIESEL) {
                    lambdaCommand.run(socket.getInputStream(), socket.getOutputStream());
                }
            } catch (CarIncompatibleException e) {
                Log.e ("OBD", e.getMessage());
//                Toast.makeText(getApplicationContext(), "Car not compatible!", Toast.LENGTH_LONG).show();
                compatibilityMode = 2;
            } catch (IOException e) {
                Log.e("OBD", e.getMessage());
//                Toast.makeText(getApplicationContext(), "Connection problem encountered!", Toast.LENGTH_LONG).show();
                compatibilityMode = 2;
            } catch (Exception e) {
                Log.e("OBD", e.getMessage());
                compatibilityMode = 2;
            }
//            compatibilityMode = 2;
            if (compatibilityMode == 2) { // Try Extended Compatibility Mode
                try {
//                    new TimeoutCommand(50).run(socket.getInputStream(), socket.getOutputStream());
                    rpmCommand.run(socket.getInputStream(), socket.getOutputStream());
                    intakeManifoldPressureCommand.run(socket.getInputStream(), socket.getOutputStream());
                    airIntakeTemperatureCommand.run(socket.getInputStream(), socket.getOutputStream());
                    Toast.makeText(getApplicationContext(), "Using extended compatibility mode", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e("OBD", e.getMessage());
                    Toast.makeText(getApplicationContext(), "Connection problem encountered!", Toast.LENGTH_LONG).show();
                    compatibilityMode = 0;
                    return;
                } catch (Exception e) {
                    Log.e("OBD", e.getMessage());
                    compatibilityMode = 0;
                    return;
                }
            }

            if (compatibilityMode != 0) {
                Toast.makeText(getApplicationContext(), "Started!", Toast.LENGTH_SHORT).show();
                isConnected = true;
            }
            else {
                Toast.makeText(getApplicationContext(), "Car not compatible!", Toast.LENGTH_LONG).show();
                mainScreen.startButton.setText("START");
            }

        }

        @Override
        protected String doInBackground(Void... voids) {
            if (compatibilityMode == 0) {
                onDestroy();
            } else if (isConnected)
                getDataFromDevice();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... text) {
            if (mainScreen != null) {
                mainScreen.mafValue.setText(text[0]);
                mainScreen.speedValue.setText(text[1]);
                mainScreen.distanceValue.setText(text[2] + " km");
                mainScreen.timeValue.setText(text[3]);
                mainScreen.fuelConsumptionValue.setText(text[4]);
                mainScreen.litersConsumedValue.setText(text[5] + " l");
            }
        }

        private void calculateTimeDistance(int currentSpeed) {
            currentTime = (System.currentTimeMillis()- startCycleTime);
            startCycleTime = System.currentTimeMillis();
            realTime = (System.currentTimeMillis()- startTime) + previousTime;
//            workingTime += currentTime;
            distance += (double)currentSpeed * (currentTime /(1000*3600.0));
        }

        private String formatTime(long time) {
            long second = (time / 1000) % 60;
            long minute = (time / (1000 * 60)) % 60;
            long hour = time / (1000 * 60 * 60);
            return String.format(Locale.ENGLISH,"%02d:%02d:%02d", hour, minute, second);
        }

        private String formatConsumedFuel() {
            return new DecimalFormat("#.00#").format(fuelConsumptionSum);
        }

        private double calculateFuelConsumption() {
            Log.d("OBD", "FuelType: "+fuelType);
            if (fuelType == FuelType.PETROL) {
                Log.d("OBD", "Current time: " + (double)currentTime/1000);
                currentFuelConsumption = ((currentMaf / airPetrolRatio) / petrolDensity) * ((double)currentTime / 1000);
                fuelConsumptionSum += currentFuelConsumption;
                Log.d("OBD", "Fuel: "+currentFuelConsumption);
            }
            else if (fuelType == FuelType.DIESEL) {
                currentMff = calculateMff();
                currentFuelConsumption = currentMff * dieselDensity * currentTime / 1000;
                fuelConsumptionSum += currentFuelConsumption;
//                currentFuelConsumption = ((currentMaf / airDieselRatio) / dieselDensity) * currentTime / 1000;
//                fuelConsumptionSum += currentFuelConsumption;
//                return fuelConsumptionSum * 100.0 / distance;
            }
            if (distance < 0.01) // don't divide by 0
                return 0;
            return fuelConsumptionSum * 100.0 / distance;
        }

        private double calculateMff() {
            try {
                lambdaCommand.run(socket.getInputStream(), socket.getOutputStream());
            } catch (Exception e) {
                Log.e("OBD", e.getMessage());
                isConnected = false;
            }
            currentAfr = lambdaCommand.getEquivalenceRatio();
            Log.d("OBD", "AFR: "+ currentAfr);
            if (currentAfr > 1.97) {
                currentAfr = 0.23478 / ( 0.218911 - 0.18415 * lambdaCommand.getLambdaVoltage()); //regression function for Passat B6 2.0 TDI
            }
            Log.d("OBD", "AFR regression: "+currentAfr);
            return (currentMaf/currentAfr) / airDieselRatio;
        }

        private String getFormattedFuelConsumption() {
            currentLitresPer100Km = calculateFuelConsumption();
            if (currentLitresPer100Km < 0.01)
                return "--.--";
            return new DecimalFormat("#.0#").format(currentLitresPer100Km);
        }

        private String getFormattedDistance() {
            return new DecimalFormat("#.0#").format(distance);
        }
    }

    public class MyBinder extends Binder {
        public UbiCarService getService() {
            return UbiCarService.this;
        }
    }

}
