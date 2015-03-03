package no.kantega.techex.android.tools;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import com.kontakt.sdk.android.configuration.ForceScanConfiguration;
import com.kontakt.sdk.android.configuration.MonitorPeriod;
import com.kontakt.sdk.android.connection.OnServiceBoundListener;
import com.kontakt.sdk.android.device.BeaconDevice;
import com.kontakt.sdk.android.device.Region;
import com.kontakt.sdk.android.factory.AdvertisingPackage;
import com.kontakt.sdk.android.factory.Filters;
import com.kontakt.sdk.android.manager.BeaconManager;
import com.kontakt.sdk.android.manager.BeaconManager.MonitoringListener;
import no.kantega.techex.android.R;
import no.kantega.techex.android.rest.LocationUpdateTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Listening to beacon events
 */
public class BeaconMonitorListener extends Service implements MonitoringListener{

    private final String TAG = BeaconMonitorListener.class.getSimpleName();

    private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 1;

    private BeaconManager beaconManager = null;

    private boolean started = false;

    private String userId;

    private String updateURL;

    private static UUID regionUUID; //TODO there is going to be a set of configuration data

    private BeaconDataHelper dbHelper;

    public void init(Context context) {
            beaconManager = BeaconManager.newInstance(context);
            //beaconManager.setMonitorPeriod(MonitorPeriod.MINIMAL); //5 s active, 5s passive
            beaconManager.setMonitorPeriod(new MonitorPeriod(5*1000,60*1000)); //TODO store in config, active is min 5s
            beaconManager.setForceScanConfiguration(ForceScanConfiguration.DEFAULT);
            beaconManager.registerMonitoringListener(this);
            beaconManager.addFilter(new Filters.CustomFilter() {
            @Override
            public Boolean apply(AdvertisingPackage object) {
                final UUID proximityUUID = object.getProximityUUID();
                //TODO there is going to be a list of regions, we nee to filter based on those
                return proximityUUID.equals(regionUUID);
            }
        });
    }

    @Override
    public void onMonitorStart() {

    }

    @Override
    public void onMonitorStop() {

    }

    /**
     * Beacon status update - only send notification if proximity changed.
     * @param region
     * @param list
     */
    @Override
    public void onBeaconsUpdated(Region region, List<BeaconDevice> list) {
        Log.d(TAG,"UPDATE RECEIVED WITH "+list.size()+" beacons");
        for (BeaconDevice bd: list) {
            int lastDistance = getLastDistance(bd);
            if (lastDistance == bd.getProximity().ordinal()) {
                //Distance from beacon hasn't changed, no update needed
                //Log.d(TAG,"Beacon distance didn't change for "+ getBeaconId(bd)+"("+lastDistance+","+bd.getProximity().name()+")");
            } else {
                //Beacon distance changed, send update
                boolean saved = saveDistance(bd);
                sendUpdate(bd);
                Log.d(TAG,"Beacon status updated, and saved ("+saved+") for "+getBeaconId(bd));
            }
        }
    }

    /**
     * First time seeing a beacon - always send message
     * @param region
     * @param beaconDevice
     */
    @Override
    public void onBeaconAppeared(Region region, BeaconDevice beaconDevice) {
        Log.d(TAG,"ONBA " + region.getIdentifier());
        sendUpdate(beaconDevice);
        saveDistance(beaconDevice);
        String msg = "Beacon appeared: "+getBeaconId(beaconDevice) + " - " + beaconDevice.getProximity().name();
        Log.d(TAG,msg);
    }

    @Override
    public void onRegionEntered(Region region) {

    }

    @Override
    public void onRegionAbandoned(Region region) {

    }

    /**
     * This is called when some activity asks for the service to be started. If it's started with this, the service
     * runs indefinitely (has to be explicitly stopped). This is running on the main thread.
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started");
        super.onStartCommand(intent, flags, startId);

        //There should only be one started instance.
        if (!started) {
            if (!beaconManager.isBluetoothEnabled()) {
                Log.e(TAG, "Service cannot be started without bluetooth enabled."); //This shouldn't happen
                started = false;
            } else if (beaconManager.isConnected()) {
                try {
                    beaconManager.startRanging();
                    started = true;
                } catch (RemoteException e) {
                    Log.e(TAG, "Error trying to start beacon monitoring.", e);
                    started = false;
                }
            } else {
                try {
                    connect();
                    started = true;
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to connect beacon monitoring.", e);
                    started = false;
                }
            }
            if (started) {
                Toast toast = Toast.makeText(this, getString(R.string.beacon_started), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
            }
        }
        return START_NOT_STICKY; //If application is killed, the service is not automatically restarted
    }

    /**
     * Called if activities bind to it.
     * @param intent
     * @return Interface for communicating
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * First time creation of service
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"Created");

        regionUUID = UUID.fromString(getString(R.string.beacon_region_uuid));

        init(this);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.config_sharedpref_id),Context.MODE_PRIVATE);
        userId = prefs.getString("id",null); //User id
        updateURL = String.format(getString(R.string.config_server_address) + getString(R.string.config_rest_locationupdate), userId);

        dbHelper = new BeaconDataHelper(this);
    }

    /**
     * Service is destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroyed");
        Toast warning = Toast.makeText(this,R.string.beacon_destroy_warning,Toast.LENGTH_LONG);
        warning.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL,0,0);
        warning.show();
        beaconManager.stopMonitoring();
        beaconManager.disconnect();
        beaconManager = null;
        started = false;
    }

    private void connect() throws IllegalStateException {
        try {
            beaconManager.connect(new OnServiceBoundListener() {
                @Override
                public void onServiceBound() {
                    try {
                        beaconManager.startMonitoring();
                    } catch (RemoteException e) {
                       Log.e(TAG,"Error trying to start monitoring",e);
                    }
                }
            });
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Created the string id required by the API
     * @param bd
     * @return
     */
    private String getBeaconId(BeaconDevice bd) {
        return bd.getMajor()+":"+bd.getMinor();
    }


    /**
     * Get the last stored distance of the beacon
     * @param bd
     * @return -1 if no previous data, otherwise ordinal value of proximity
     */
    public int getLastDistance(BeaconDevice bd) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteStatement statement = db.compileStatement(dbHelper.SQL_QUERY_DISTANCE);
        statement.bindLong(1,bd.getMajor());
        statement.bindLong(2,bd.getMinor());
        try {
            long dist = statement.simpleQueryForLong();
            return (int) dist;
        } catch (SQLiteDoneException e) {
            //No rows returned - no previous data
            return -1;
        }
    }

    /**
     * UPSERT beacon data
     * @param bd
     * @return true if upsert is successful
     */
    public boolean saveDistance(BeaconDevice bd) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        SQLiteStatement statement = db.compileStatement(dbHelper.SQL_SAVE_DISTANCE);
        statement.bindLong(1,bd.getMajor());
        statement.bindLong(2,bd.getMinor());
        statement.bindLong(3,bd.getProximity().ordinal());
        long res = statement.executeInsert();
        return (res != -1);
    }

    public void sendUpdate(BeaconDevice beaconDevice) {
        String beaconId = getBeaconId(beaconDevice);
        String proximity = beaconDevice.getProximity().name().toLowerCase();
        new LocationUpdateTask().execute(updateURL,beaconId,proximity); //new AsyncTask needed for every request
    }
}
