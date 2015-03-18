package no.kantega.techex.android.tools;

import android.app.AlertDialog;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import no.kantega.techex.android.R;

/**
 * Singleton util class for
 * * checking internet connection and creating popup if it's not available
 * * listening on connectivity change broadcast when no network is found
 */
public class ConnectionChecker {

    private static final String TAG = ConnectionChecker.class.getSimpleName();

    /**
     * Broadcast action that is sent when the device has an active network connection again
     */
    public static final String RECONNECT_BROADCAST = "no.kantega.techex.android.BROADCAST_NETWORK";

    private static ConnectionChecker instance;

    /**
     * Needs to be set to true when class has an active receiver for connectivity change broadcast
     */
    private boolean isReceivingBroadcast;

    private ConnectionChecker(){
        isReceivingBroadcast = false;
    }

    public static ConnectionChecker getInstance() {
        if (instance == null)
        {
            instance = new ConnectionChecker();
        }
        return instance;
    }

    /**
     * Checks whether internet connection is active. If not, it creates a popup in the given context.
     * @param action action that needs internet connection (for example: "registering")
     * @param showDialog when set to true, a pop-up alert is shown to alert the user to the missing connection (should
     *                   only be used when the method is called with activity context)
     * @return true if internet connection is available
     */
    public boolean checkConnection(Context context, String action, boolean showDialog) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if ( ni == null || !ni.isConnected()) {
            //No connection is available, create alert
            if (showDialog) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getString(R.string.network_alert_title))
                        .setMessage(String.format(context.getString(R.string.network_alert_msg), action));
                builder.setPositiveButton(context.getString(R.string.network_alert_btn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
            }
            if (!isReceivingBroadcast) {
                Log.d(TAG, "Registering to connectivity change broadcast");
                context.registerReceiver(connectionChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                isReceivingBroadcast = true;
            }
            return false;
        } else {
            if(isReceivingBroadcast) {
                Log.d(TAG,"Unregistering from connectivity change broadcast");
                context.unregisterReceiver(connectionChangeReceiver);
                isReceivingBroadcast = false;
            }
            return true;
        }
    }

    /**
     * Broadcast receiver for network connectivity change
     * Should be actively receiving when there is no active network connection, so when it comes back it lets
     * the QLA activity know with a local broadcast.
     */
    private final BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager connectivityManager = (ConnectivityManager)
                        context.getSystemService(Context.CONNECTIVITY_SERVICE );
                NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = activeNetInfo != null && activeNetInfo.isConnectedOrConnecting();

                if (isConnected) {
                    //Send refresh broadcast to QLA
                    Intent localIntent = new Intent(RECONNECT_BROADCAST);
                    // Broadcasts the Intent to receivers in this app.
                    LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

                    //Unregister receiver
                    Log.d(TAG,"Connected again, unregistering from connectivity change broadcast");
                    context.unregisterReceiver(connectionChangeReceiver);
                    isReceivingBroadcast = false;
                } else {
                    // Still no connection, keep receiving
                }

            }
        }
    };
}
