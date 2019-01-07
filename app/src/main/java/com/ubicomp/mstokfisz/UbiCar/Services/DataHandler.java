package com.ubicomp.mstokfisz.UbiCar.Services;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.ubicomp.mstokfisz.UbiCar.DataClasses.Data;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.d("DataHandler", "Couldn't save");
        }
        else {
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                FileOutputStream fos = new FileOutputStream("UbiCar"+timeStamp+".txt");
                fos.write(("Distance: "+data.getCurrentTrip().getDistance()+" km").getBytes());
                fos.close();
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

}