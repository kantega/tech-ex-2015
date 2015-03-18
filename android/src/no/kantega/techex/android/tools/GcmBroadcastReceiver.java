package no.kantega.techex.android.tools;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * This class needs to be registered fo receive the Google Cloud Messaging updates.
 * It's a wakeful receiver, which means that the message is received/parsed
 * even if the phone is asleep.
 */
public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {

    private final String TAG = GcmBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received smg");
        // Explicitly specify that GcmIntentService will handle the intent.
        ComponentName comp = new ComponentName(context.getPackageName(),
                GcmIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}
