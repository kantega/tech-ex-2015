package no.kantega.techex.android.tools;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
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
import com.kontakt.sdk.core.Proximity;
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

    public static enum BeaconProximityChange {ENTER, EXIT, IGNORE};

    private final String TAG = BeaconMonitorListener.class.getSimpleName();

    private BeaconManager beaconManager = null;

    private boolean started = false;

    private String userId;

    private String updateURL;

    private static UUID regionUUID;

    private static int numberOfRegions;

    private BeaconDataHelper dbHelper;

    private BeaconDevice currentClosestBeacon;

    public void init(Context context) {
        Configuration configuration = Configuration.getInstance();
        beaconManager = BeaconManager.newInstance(context);
        beaconManager.setMonitorPeriod(new MonitorPeriod(configuration.getBeaconMonitoringActive()*1000,
                configuration.getBeaconMonitoringPassive()*1000));
        beaconManager.setForceScanConfiguration(ForceScanConfiguration.DEFAULT);
        beaconManager.registerMonitoringListener(this);
        beaconManager.addFilter(new Filters.CustomFilter() {
            @Override
            public Boolean apply(AdvertisingPackage object) {
                final UUID proximityUUID = object.getProximityUUID();
                if (proximityUUID.equals(regionUUID)) {
                    return (object.getMajor() > 0) && (object.getMajor() <= numberOfRegions);
                } else {
                    return false;
                }
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
    public synchronized void  onBeaconsUpdated(Region region, List<BeaconDevice> list) {
        Log.d(TAG,"UPDATE RECEIVED WITH "+list.size()+" beacons");

        //Only the closest beacon is interesting
        BeaconDevice closestBeacon = getClosestBeacon(list);

        if (currentClosestBeacon == null) {
            //No currently close beacon is saved (user exited the last one)
           if (closestBeacon.getProximity() != Proximity.FAR) {
               //If it's close enough, it's saved as current closest
               currentClosestBeacon = closestBeacon;

               // Entering new closest beacon
               sendUpdate(closestBeacon, BeaconProximityChange.ENTER);
           }
        } else if (closestBeacon.getMajor() == currentClosestBeacon.getMajor()
                && closestBeacon.getMinor() == currentClosestBeacon.getMinor()) {
            //The closest beacon hasn't changed, but maybe it's further away
            if (closestBeacon.getProximity() == Proximity.FAR) {
                sendUpdate(closestBeacon,BeaconProximityChange.EXIT);
                currentClosestBeacon = null;
            }
        } else {
            //Closest beacon has changed

            // Exiting previous closest beacon
            // Check for new data
            for (BeaconDevice bd : list) {
                if (bd.getMajor() == currentClosestBeacon.getMajor()
                        && bd.getMinor() == currentClosestBeacon.getMinor()) {
                    currentClosestBeacon = bd;
                    break;
                }
            }
            sendUpdate(currentClosestBeacon,BeaconProximityChange.EXIT);

            // Entering new closest beacon
            sendUpdate(closestBeacon,BeaconProximityChange.ENTER);

            currentClosestBeacon=closestBeacon;
        }
    }

    /**
     * First time seeing a beacon - always send message
     * @param region
     * @param beaconDevice
     */
    @Override
    public synchronized  void onBeaconAppeared(Region region, BeaconDevice beaconDevice) {
        String msg = "Beacon appeared: "+getBeaconId(beaconDevice) + " - " + beaconDevice.getProximity().name();
        Log.d(TAG,msg);

        if (currentClosestBeacon == null) {
            if (beaconDevice.getProximity() != Proximity.FAR) {
                currentClosestBeacon = beaconDevice;
                sendUpdate(currentClosestBeacon,BeaconProximityChange.ENTER);
            }
        } else if (compareBeacons(beaconDevice,currentClosestBeacon) == -1 ){
            //The new beacon is closer

            //Exit previous closest
            sendUpdate(currentClosestBeacon,BeaconProximityChange.EXIT);

            // Entering new closest beacon
            sendUpdate(beaconDevice,BeaconProximityChange.ENTER);
            currentClosestBeacon = beaconDevice;
        }

    }

    @Override
    public void onRegionEntered(Region region) {
        String msg = "Region entered: "+region.getIdentifier()+ "("+region.getProximity().toString()+"-"+region.getMajor()+"-"+region.getMinor()+")";
        Log.d(TAG,msg);

    }

    @Override
    public void onRegionAbandoned(Region region) {
        String msg = "Region abandoned: "+region.getIdentifier()+ "("+region.getProximity().toString()+"-"+region.getMajor()+"-"+region.getMinor()+")";
        Log.d(TAG,msg);
    }

    //for testing
    private void sendNotification(String title, String msg) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notfication_icon)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setContentText(msg);
        mNotificationManager.notify(1,mBuilder.build());
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

        regionUUID = UUID.fromString(getString(R.string.config_beacon_region_uuid));

        init(this);

        Configuration configuration =  Configuration.getInstance();

        SharedPreferences prefs = getSharedPreferences(configuration.getSharedPreferencesId(),Context.MODE_PRIVATE);
        userId = prefs.getString(configuration.getSpUserIdKey(),null); //User id
        updateURL = configuration.getLocationREST(userId);
        numberOfRegions = prefs.getInt(configuration.getSpRegionNumberKey(),0);

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
//                        Region tmp;
//                        Set<Region> regions = new HashSet<Region>();
//                        for (int i = 1; i<=numberOfRegions;i++) {
//                            tmp = new Region(regionUUID,i,0,"Region "+i);
//                            regions.add(tmp);
//                        }
//                        beaconManager.startMonitoring(regions);
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

    public void sendUpdate(BeaconDevice beaconDevice, BeaconProximityChange change) {
        String proximity = beaconDevice.getProximity().name().toLowerCase();
        new LocationUpdateTask().execute(updateURL,Integer.toString(beaconDevice.getMajor()),Integer.toString(beaconDevice.getMinor()),proximity,change.name().toLowerCase()); //new AsyncTask needed for every request
    }

    private BeaconProximityChange getProximityChange(Proximity current, Proximity old) {
        if (current == old) {
            return BeaconProximityChange.IGNORE;
        }

        boolean isCloseNow = (current == Proximity.NEAR || current == Proximity.IMMEDIATE);
        boolean wasFarBefore = (old == Proximity.FAR);
        if (old == Proximity.FAR && (current == Proximity.NEAR || current == Proximity.IMMEDIATE)) {
            return BeaconProximityChange.ENTER;
        } else {
            return BeaconProximityChange.IGNORE;
        }
    }

    private Proximity getProximity(int ordinal) {
        switch (ordinal) {
            case 0:
                return Proximity.IMMEDIATE;
            case 1:
                return Proximity.NEAR;
            case 2:
                return Proximity.FAR;
            default:
                return Proximity.UNKNOWN;
        }
    }

    private BeaconDevice getClosestBeacon(List<BeaconDevice> list) {
        BeaconDevice closest = list.get(0);
        BeaconDevice tmp;
        for (int i = 1; i < list.size(); i++) {
            tmp = list.get(i);
            if (compareBeacons(tmp,closest) == -1) {
                closest = tmp;
                continue;
            }
        }
        Log.d(TAG,"Closest beacon is "+getBeaconId(closest)+"("+closest.getProximity().name()+" - "+closest.getAccuracy()+")");
        return closest;
    }

    /**
     *
     * @param a
     * @param b
     * @return -1, if a is closer to us then b; 0 if they are equally close; 1 if b is closer to us
     */
    private int compareBeacons(BeaconDevice a, BeaconDevice b) {
        int proximitydiff = a.getProximity().ordinal() - b.getProximity().ordinal();
        if (proximitydiff == 0) {
            //Same proximity category
            return Double.compare(a.getAccuracy(), b.getAccuracy());
        } else if (proximitydiff < 0) {
            //a is closer
            return -1;
        } else {
            // >0 => b is closer
            return 1;
        }
    }
}
