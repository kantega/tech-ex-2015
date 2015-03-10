package no.kantega.techex.android.activities;

import android.app.Application;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

/**
 * Main application class - created just to enable BLE power saving
 */
public class TechExpApplication extends Application {
    private BackgroundPowerSaver backgroundPowerSaver;

    @Override
    public void onCreate() {
        super.onCreate();
        // Simply constructing this class and holding a reference to it in your custom Application class
        // enables auto battery saving of about 60%
        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }
}
