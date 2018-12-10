package com.ubicomp.mstokfisz.UbiCar;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.ubicomp.mstokfisz.UbiCar.Activities.MainScreen;

public class UbiCar extends Application {
    public static final String CHANNEL_ID = "ubiCarServiceChannel";
    private MainScreen activeMainScreen = null;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Example Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public MainScreen getActiveMainScreen() {
        return activeMainScreen;
    }

    public void setActiveMainScreen(MainScreen activeMainScreen) {
        this.activeMainScreen = activeMainScreen;
    }

    public void removeActiveMainScreen() {
        activeMainScreen = null;
    }
}
