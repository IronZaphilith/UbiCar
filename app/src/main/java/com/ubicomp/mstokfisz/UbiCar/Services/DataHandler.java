package com.ubicomp.mstokfisz.UbiCar.Services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.ubicomp.mstokfisz.UbiCar.DataClasses.Data;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataHandler {
    private static final String DATA_FILE = "data";
    private Context context;
    private Gson gson = new Gson();

    public DataHandler(Context context) {
        this.context = context;
    }

    public Data getData() {
        FileInputStream fileInputStream = null;
        Data data = null;

        try {
            if (!isFilePresent()) {
                Log.d("FILE", "No file");
                data = new Data();
            }
            else {
                fileInputStream = context.openFileInput(DATA_FILE);
                int size = fileInputStream.available();
                byte[] buffer = new byte[size];
                fileInputStream.read(buffer);
                fileInputStream.close();
                data = gson.fromJson(new String(buffer, StandardCharsets.UTF_8), Data.class);
            }
        } catch (FileNotFoundException e) {
            Log.e("FILE", "File not found");
        } catch (Exception e) {
            Log.e("FILE", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch(IOException e) {
                Log.e("FILE", "IO error closing");
                e.printStackTrace();
            }
        }
        return data;
    }

    public void saveData(Data data) {
        FileOutputStream fileOutputStream = null;

        try {
            String dataJson = gson.toJson(data);
            fileOutputStream = context.openFileOutput(DATA_FILE, Context.MODE_PRIVATE);
            fileOutputStream.write(dataJson.getBytes());
        } catch (FileNotFoundException e) {
            Log.e("FILE", "File not found");
        } catch (IOException e) {
            Log.e("FILE", "IO Error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("FILE", "Error: " + e.getMessage());
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch(IOException e) {
                Log.e("FILE", "IO error closing");
                e.printStackTrace();
            }
        }

        // Try creating folder
        String myfolder = Environment.getExternalStorageDirectory()+File.separator+"UbiCar";
        File f = new File(myfolder);
        if(!f.exists())
//            try {
//                f.mkdirs();
//            } catch (Exception e) {
//                Log.d("DataHandler", e.getMessage());
//            }
            if (!f.mkdir()) {
                Log.e("DataHandler", "Folder can't be created!");
                //                Toast.makeText(t, "Folder can't be created.", Toast.LENGTH_SHORT).show();
            } else
                Log.d("DataHandler", "Folder created!");
        else
            Log.d("DataHandler", "Folder already exists!");

        // Save file
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.e("DataHandler", "Couldn't save");
        }
        else {
            Log.d("DataHandler", "Saving data");
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File tripFile = new File(myfolder, timeStamp+".txt");
                if (!tripFile.exists()) {
                    FileOutputStream fos = new FileOutputStream(tripFile);
                    fos.write(("Car: " + data.getCurrentTrip().getCar().getName()).getBytes());
                    OutputStreamWriter osw = new OutputStreamWriter(fos);
                    osw.append("\r\n");
                    osw.append("Fuel type: ").append(data.getCurrentTrip().getCar().getFuelType().name()).append("\r\n");
                    osw.append("Engine size: ").append(String.valueOf(data.getCurrentTrip().getCar().getEngineSize())).append(" cm3\r\n");
                    osw.append("Distance: ").append(String.valueOf(data.getCurrentTrip().getDistance())).append(" km\r\n");
                    osw.append("Time: ").append(formatTime(data.getCurrentTrip().getTravelTime())).append("\r\n");
                    osw.append("Average speed: ").append(String.valueOf(data.getCurrentTrip().getAvgSpeed())).append(" km/h\r\n");
                    osw.append("Consumed fuel: ").append(String.valueOf(data.getCurrentTrip().getFuelConsumptionSum())).append(" l\r\n");
                    osw.append("Average fuel consumption: ").append(String.valueOf(data.getCurrentTrip().getAvgfuelConsumption())).append(" l/100km\r\n");
                    osw.flush();
                    osw.close();
                    fos.flush();
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(extStorageState);
    }

    private boolean isFilePresent() {
        String path = context.getFilesDir().getAbsolutePath() + "/" + DATA_FILE;
        File file = new File(path);
        return file.exists();
    }

    private String formatTime(long time) {
        long second = (time / 1000) % 60;
        long minute = (time / (1000 * 60)) % 60;
        long hour = time / (1000 * 60 * 60);
        return String.format(Locale.ENGLISH,"%02d:%02d:%02d", hour, minute, second);
    }
}