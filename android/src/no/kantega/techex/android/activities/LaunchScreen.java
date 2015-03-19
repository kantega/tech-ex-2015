package no.kantega.techex.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import no.kantega.techex.android.R;
import no.kantega.techex.android.tools.Configuration;

/**
 * Main activity
 *
 * This is opened first when application is started. Has no UI but does some initialization:
 * * Initialize Configuration class
 *
 * It automatically redirects to
 * * {@link RegisterActivity} if user is not registered yet
 * * {@link WelcomeActivity} if user is already registered
 */
public class LaunchScreen extends Activity {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final String TAG = LaunchScreen.class.getSimpleName();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"Main activity created");

        Configuration configuration = Configuration.getInstance();
        configuration.init(this); //Very first time initialization

        SharedPreferences prefs = getSharedPreferences(configuration.getSharedPreferencesId(),Context.MODE_PRIVATE);
        String id = prefs.getString(configuration.getSpUserIdKey(),null); //If registered, we received an ID from the server

        // Check if device is capable of required services
        if (checkPlayServices() && checkDeviceCompatibilityForBLE()) {
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
        } else {

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
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
                builder.setMessage(R.string.play_unsupported_msg)
                        .setTitle(R.string.play_unsupported_title);
                builder.setPositiveButton(getString(R.string.play_unsupported_btn),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                finish();
                            }
                        });
                builder.create().show();
            }
            return false;
        }
        return true;
    }

    /**
     * Checks if this device is capable of running Bluetooth Low Energy scanning.
     * If unsupported, a dialog is displayed that on click closes the app.
     * @return false if not compatible
     */
    private boolean checkDeviceCompatibilityForBLE() {
        String message = null;
        if (android.os.Build.VERSION.SDK_INT < 18) {
            message ="This device doesn't have the required minimum SDK (18, Jelly Bean MR3)";
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            message="This device doesn't support Bluetooth LE";
        }

        if (message != null) {
            Log.i(TAG, "This device is not supported: "+message);
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
            builder.setMessage(message)
                    .setTitle(R.string.play_unsupported_title);
            builder.setPositiveButton(getString(R.string.play_unsupported_btn),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            finish();
                        }
                    });
            builder.create().show();
            return false;
        } else {
            return true;
        }
    }




}
