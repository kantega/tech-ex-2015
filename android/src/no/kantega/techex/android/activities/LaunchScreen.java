package no.kantega.techex.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import no.kantega.techex.android.tools.Configuration;

import java.io.IOException;

/**
 * Main activity
 *
 * This is opened first when application is started. It has no UI, but does some setup functions:
 * * Registers for GCM
 *
 * It automatically redirects to
 * * RegisterActivity if user is not registered yet
 * * QuestListActivity if user is already registered
 */
public class LaunchScreen extends Activity {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static String PROPERTY_REG_ID;

    private final String TAG = LaunchScreen.class.getSimpleName();

    private GoogleCloudMessaging gcm;
    private Context context;
    private SharedPreferences prefs;
    private String gcmProjectId;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"Main activity created");

        Configuration configuration = Configuration.getInstance();
        configuration.init(this); //Very first time initialization

        prefs = getSharedPreferences(configuration.getSharedPreferencesId(),Context.MODE_PRIVATE);
        String id = prefs.getString(configuration.getSpUserIdKey(),null); //If registered, we received an ID from the server
        String registrationId = getRegistrationId();

        PROPERTY_REG_ID = configuration.getSpGcmAuthKey();


       // Check device for Play Services APK.
        if (checkPlayServices()) {
            // GCM is available
            gcm = GoogleCloudMessaging.getInstance(this);
            context = getApplicationContext();
            gcmProjectId = configuration.getGcmProjectId();
            if (registrationId.isEmpty()) {
                //The device hasn't registered for the GCM yet or it needs to be updated
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        if (id == null) {
            //Not registered yet
            Intent i = new Intent(LaunchScreen.this, RegisterActivity.class);
            LaunchScreen.this.startActivity(i);
            Log.d(TAG, "Redirecting to registration activity.");
            finish();
        } else {
            Intent i = new Intent(LaunchScreen.this, WelcomeActivity.class);
            LaunchScreen.this.startActivity(i);
            Log.d(TAG, "Redirecting to welcome activity.");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId() {
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.

        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void,Void,String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    Log.d(TAG,"gcmprojectid "+gcmProjectId);
                    String regid = gcm.register(gcmProjectId);
                    msg = "Device registered, registration ID=" + regid;

                    //Store
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(PROPERTY_REG_ID,regid);
                    editor.commit();
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.i(TAG, msg);
            }
        }.execute(null, null, null);
    }
}
