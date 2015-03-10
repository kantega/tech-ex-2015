package no.kantega.techex.android.tools;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import no.kantega.techex.android.R;
import no.kantega.techex.android.rest.LocationUpdateTask;
import no.kantega.techex.android.rest.OnTaskComplete;
import no.kantega.techex.android.rest.RegionInfoTask;
import org.altbeacon.beacon.*;

import java.util.*;

/**
 * Beacon monitoring listener with the AltBeacon SDK
 */
public class AltBeaconMonitorListener extends Service implements BeaconConsumer{
    public static enum BeaconProximityChange {ENTER, EXIT, IGNORE};
    public static enum BeaconProximity {UNKNOWN, IMMEDIATE, NEAR, FAR};

    private final static String TAG = AltBeaconMonitorListener.class.getSimpleName();

    private IBinder binder = new LocalBinder();

    private static AltBeaconMonitorListener _self;

    private BeaconManager beaconManager;

    private boolean started = false;

    private String regionUUID;

    private String userId;

    private int numberOfRegions;

    private List<Region> regionsToMonitor;

    private Beacon currentClosestBeacon = null;

    private Collection<Beacon> tmpBeaconData;

    private int tmpBeaconRegions;

    /**
     * Got connected to the beacon manager, can start monitoring
     */
    @Override
    public void onBeaconServiceConnect() {
        Log.i(TAG,"Connect servicxe");
        try {
                for (Region r: regionsToMonitor) {
                    beaconManager.startRangingBeaconsInRegion(r);
                }
        } catch (RemoteException e) {   }

    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        super.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return super.bindService(intent,serviceConnection,i);
    }

    /**
     * This is called when parent activity wants to start it
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        //There should only be one started instance.
        if (!started) {
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                Log.e(TAG, "Service cannot be started without bluetooth enabled."); //This shouldn't happen
                started = false;
            } else {
                //Fetching list of regions
                RegionInfoTask rit = new RegionInfoTask();
                rit.setListener(regionTaskCompleter);
                rit.execute();
                //service will be started after receiving region information
            }
        }
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Load config data
        Configuration configuration =  Configuration.getInstance();
        regionUUID = configuration.getRegionUUID();
        SharedPreferences prefs = getSharedPreferences(configuration.getSharedPreferencesId(),Context.MODE_PRIVATE);
        userId = prefs.getString(configuration.getSpUserIdKey(), null); //User id

        //Config beacon manager
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.setRangeNotifier(rangeNotifier);
        beaconManager.setForegroundScanPeriod(2500);
        beaconManager.setForegroundBetweenScanPeriod(10000);
        beaconManager.setBackgroundScanPeriod(5000); //TODO config
        beaconManager.setBackgroundBetweenScanPeriod(10000);

        tmpBeaconData = new ArrayList<Beacon>();
        tmpBeaconRegions = 0;

        _self = this;
    }

    /**
     * This is called when regions to monitor is fetched from the server and we can start monitoring.
     */
    private OnTaskComplete<Integer> regionTaskCompleter = new OnTaskComplete<Integer>() {
        @Override
        public void onTaskComplete(Integer result) {
            //Save region number
            numberOfRegions = result;

            //Create regions
            regionsToMonitor = new ArrayList<Region>();
            Region r;
            for (int i = 1; i <= numberOfRegions; i++) {
                r = new Region("Region "+i, Identifier.parse(regionUUID),Identifier.fromInt(i),null);
                regionsToMonitor.add(r);
            }

            //Bind to start
            beaconManager.bind(_self);

            Toast toast = Toast.makeText(_self, getString(R.string.beacon_started), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();

            started = true;
        }
    };

    /**
     * This is called when no activity binds to the service anymore.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            for (Region r : regionsToMonitor) {
                beaconManager.stopRangingBeaconsInRegion(r);
            }
        }catch (RemoteException e ){
            Log.e(TAG,"Error trying to stop monitoring",e);
        }
        beaconManager.unbind(this);

        Toast warning = Toast.makeText(this,R.string.beacon_destroy_warning,Toast.LENGTH_SHORT);
        warning.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL,0,0);
        warning.show();

        Log.d(TAG,"Destroyed");
    }

    private RangeNotifier rangeNotifier = new RangeNotifier() {
        @Override
        public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
            Log.d(TAG, "Detected beacons in region " + region.getUniqueId() + " - " + collection.size());
            tmpBeaconData.addAll(collection);
            if (++tmpBeaconRegions == numberOfRegions) {
                updateBeaconState();
            }
        }
    };


    private Beacon getClosestBeacon(Collection<Beacon> beacons) {
        Iterator<Beacon> iterator = beacons.iterator();
        Beacon closest = iterator.next();
        Beacon b;
        while (iterator.hasNext()) {
            b= iterator.next();
            if (b.getDistance() < closest.getDistance()) {
                closest = b;
            }
        }
        return closest;
    }

    /**
     * Compare based on major/minor
     * @param a
     * @param b
     * @return
     */
    private boolean checkIfSameBeacon(Beacon a, Beacon b) {
        return (a.getId2().compareTo(b.getId2())==0) && (a.getId3().compareTo(b.getId3())==0);
    }

    public void sendUpdate(Beacon beacon, BeaconProximityChange change) {
        new LocationUpdateTask().execute(userId,
                beacon.getId2().toString(),
                beacon.getId3().toString(),
                getProximity(beacon).name().toLowerCase(),
                change.name().toLowerCase()); //new AsyncTask needed for every request

    }

    private BeaconProximity getProximity(Beacon beacon) {
        double dist = beacon.getDistance();
        if (dist < 0.0D) {
            return BeaconProximity.UNKNOWN;
        } else if (dist < 0.5D) {
            return BeaconProximity.IMMEDIATE;
        } else if (dist < 3.0D) {
            return BeaconProximity.NEAR;
        } else {
            // >= 3.0D
            return BeaconProximity.FAR;
        }
    }

    /**
     * Should be called after receiving the data from all monitored regions
     */
    private void updateBeaconState(){
        Log.d(TAG, "Updating beacon state. Beacons: "+tmpBeaconData.size());
        if (!tmpBeaconData.isEmpty()) {
            Beacon closest = getClosestBeacon(tmpBeaconData);
            Log.d(TAG, "closest received: " + closest.toString());
            if (currentClosestBeacon != null) {
                Log.d(TAG, "current closest: " + currentClosestBeacon.toString());
                if (!checkIfSameBeacon(closest, currentClosestBeacon)) {
                    //The current closest beacon is not the same as the previously saved

                    //Exit previous closest
                    sendUpdate(currentClosestBeacon, BeaconProximityChange.EXIT);

                    // Entering new closest beacon
                    sendUpdate(closest, BeaconProximityChange.ENTER);
                    currentClosestBeacon = closest;
                } else {
                    // The found closest is the same as the last saved closest. Check if we are further away
                    if (getProximity(closest) == BeaconProximity.FAR) {
                        sendUpdate(closest, BeaconProximityChange.EXIT);
                        currentClosestBeacon = null;
                    }
                }
            } else {
                //No current closest
                BeaconProximity proximity = getProximity(closest);
                if (proximity == BeaconProximity.IMMEDIATE || proximity == BeaconProximity.NEAR) {
                    // Entering new closest beacon
                    sendUpdate(closest, BeaconProximityChange.ENTER);
                    currentClosestBeacon = closest;
                }
            }
            tmpBeaconData.clear();
            tmpBeaconRegions=0;
        } else {
            //Collection is empty. If the last saved current beacon was from this region, we need to exit
            if (currentClosestBeacon != null) {
                Log.d(TAG, "Exiting because no more data for region");
                sendUpdate(currentClosestBeacon, BeaconProximityChange.EXIT);
                currentClosestBeacon = null;
            }
            tmpBeaconRegions=0;
        }
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {

        }
    }
}
