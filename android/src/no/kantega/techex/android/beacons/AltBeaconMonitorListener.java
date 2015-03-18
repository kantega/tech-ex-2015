package no.kantega.techex.android.beacons;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import no.kantega.techex.android.R;
import no.kantega.techex.android.activities.BluetoothDialogActivity;
import no.kantega.techex.android.rest.OnTaskComplete;
import no.kantega.techex.android.rest.RegionInfoTask;
import no.kantega.techex.android.tools.Configuration;
import no.kantega.techex.android.tools.ConnectionChecker;
import org.altbeacon.beacon.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This service is responsible for running the AltBeacon beacon monitoring
 * service and shutting it down.
 * <p/>
 * When service is first started, Bluetooth connection is checked. If user is not connected,
 * a dialog is started to prompt connection.
 * <p/>
 * The service has a broadcast listener to keep up with bluetooth changes, and start/stop
 * monitoring accordingly.
 * <p/>
 * The actual beacon update handling happens in {@link no.kantega.techex.android.beacons.BeaconLocationHandler}
 */
public class AltBeaconMonitorListener extends Service implements BeaconConsumer {
    private final static String TAG = AltBeaconMonitorListener.class.getSimpleName();

    /**
     * Service binder provided for the activities that start this
     */
    private IBinder binder = new LocalBinder();

    private static AltBeaconMonitorListener _self;

    /**
     * AltBeacon beacon manager
     */
    private BeaconManager beaconManager;

    /**
     * Set to true when beacon listening is active
     */
    private boolean started = false;

    /**
     * UUID of beacons (loaded from config)
     */
    private String regionUUID;

    /**
     * Registered userId (required for location updates)
     */
    private String userId;

    /**
     * Number of regions to monitor - needed to generate regions
     * (regions are set to have a major id ranging from 1 to this value)
     */
    private int numberOfRegions;

    /**
     * Region objects for beacon monitoring
     */
    private List<Region> regionsToMonitor;

    /**
     * The object for parsing beacon updates & sending location data to the server
     */
    private BeaconLocationHandler locationHandler;

    /**
     * True if service is registered for bluetooth broadcasts (currently always)
     */
    private boolean isBluetoothReceiverRegistered;

    /**
     * True if service is registered for connection change broadcast
     * (only if connection was missing, gets unregistered when there is connection again)
     */
    private boolean isConnectivityReceiverRegistered;

    /**
     * Called after binding to beacon manager
     * Got connected to the beacon manager, can set up ranging on regions
     */
    @Override
    public void onBeaconServiceConnect() {
        Log.i(TAG, "Connect servicxe");
        try {
            for (Region r : regionsToMonitor) {
                beaconManager.startRangingBeaconsInRegion(r);
            }
        } catch (RemoteException e) {
            Log.e(TAG,"Error trying to set up ranging");
        }
    }

    /**
     * This is called when a parent activity binds to the service
     *
     * The first time someone binds, when the ranging is not started yet:
     * * Checks bluetooth connection
     * * Fetches region number from server (if it hasn't been saved before)
     * * Else (or after regions are fetched) attempts to start beacon monitoring
     *
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        //There should only be one started instance.
        if (!started) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!bluetoothAdapter.isEnabled()) {
                //Prompt to turn on bluetooth
                Intent mIntent = new Intent(this.getApplicationContext(), BluetoothDialogActivity.class);
                mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mIntent);
            } else {
                if (regionsToMonitor.isEmpty()) {
                    //service will be started after receiving region information
                    fetchRegions();
                } else {
                    //number of regions loaded from previous run, start service
                    startRanging();
                }
            }
        }
        //Creating listener on bluetooth state - when the activity for enabling is completed, it calls this
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        isBluetoothReceiverRegistered = true;
        return binder;
    }

    /**
     * Fetches the number of regions to monitor - starts an asynchronous task
     * that will call {@link #regionTaskCompleter} when done.
     *
     * If internet connection is unavailable, registers for broadcasts of connection change.
     */
    private void fetchRegions() {
        //Fetching list of regions
        if (ConnectionChecker.getInstance().checkConnection(this, "", false)) {
            RegionInfoTask rit = new RegionInfoTask();
            rit.setListener(regionTaskCompleter);
            rit.execute();
        } else {
            if (!isConnectivityReceiverRegistered) {
                //Register network change listener
                LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(ConnectionChecker.RECONNECT_BROADCAST));
                isConnectivityReceiverRegistered = true;
            }
            Log.i(TAG, "Can't fetch region list because there is no active connection.");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false; // don't call onRebind
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        //Load config data
        Configuration configuration = Configuration.getInstance();
        regionUUID = configuration.getRegionUUID();
        SharedPreferences prefs = getSharedPreferences(configuration.getSharedPreferencesId(), Context.MODE_PRIVATE);
        userId = prefs.getString(configuration.getSpUserIdKey(), null); //User id
        numberOfRegions = prefs.getInt(configuration.getSpRegionNumberKey(), 0);
        createRegionsToMonitor(); //Handles initialization of regionsToMonitor

        //Config beacon manager
        beaconManager = BeaconManager.getInstanceForApplication(this);
        locationHandler = new BeaconLocationHandler(userId);

        //Beacon layout matches the Kontakt:IO packets
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.setRangeNotifier(locationHandler);
        beaconManager.setForegroundScanPeriod(configuration.getBeaconMonitoringForegroundActive());
        beaconManager.setForegroundBetweenScanPeriod(configuration.getBeaconMonitoringForegroundPassive());
        beaconManager.setBackgroundScanPeriod(configuration.getBeaconMonitoringBackgroundActive());
        beaconManager.setBackgroundBetweenScanPeriod(configuration.getBeaconMonitoringBackgroundPassive());

        _self = this;

        isBluetoothReceiverRegistered = false;
        isConnectivityReceiverRegistered = false;
    }

    /**
     * Creates the regions to monitor
     * (requires numberOfRegions to be defined)
     *
     * Regions share the same UUID ({@link #regionUUID} and their major IDs go from 1..{@link #numberOfRegions}
     */
    private void createRegionsToMonitor() {
        if (numberOfRegions > 0) {
            regionsToMonitor = new ArrayList<Region>();
            Region r;
            for (int i = 1; i <= numberOfRegions; i++) {
                r = new Region("Region " + i, Identifier.parse(regionUUID), Identifier.fromInt(i), null);
                regionsToMonitor.add(r);
            }
        } else {
            regionsToMonitor = new ArrayList<Region>();
        }
    }

    /**
     * This is called when regions to monitor is fetched from the server and we can start monitoring.
     */
    private OnTaskComplete<Integer> regionTaskCompleter = new OnTaskComplete<Integer>() {
        @Override
        public void onTaskComplete(Integer result) {
            if (result != null) {
                //Save region number
                numberOfRegions = result;
                Configuration configuration = Configuration.getInstance();
                SharedPreferences.Editor editor = getSharedPreferences(configuration.getSharedPreferencesId(), Context.MODE_PRIVATE).edit();
                editor.putInt(configuration.getSpRegionNumberKey(), numberOfRegions);
                editor.commit();

                //Create regions
                createRegionsToMonitor();

                startRanging();
            } else {
                Log.e(TAG, "Invalid region number fetched from server");
            }
        }
    };

    /**
     * This is called when no activity binds to the service anymore.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (started) {
            stopRanging();
        }

        if (isBluetoothReceiverRegistered) {
            unregisterReceiver(mReceiver);
        }

        if (isConnectivityReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        }

        Log.d(TAG, "Destroyed");
    }

    /**
     * Prompts to start beacon ranging by binding to the beacon manager. Actual ranging will
     * start when the manager connects and calls {@link #onBeaconServiceConnect()}
     */
    private void startRanging() {
        //Bind to start
        locationHandler.setNumberOfRegions(numberOfRegions);
        beaconManager.bind(_self);

        Toast toast = Toast.makeText(_self, getString(R.string.beacon_started), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();

        started = true;
    }

    /**
     * Stops ranging by unregistering from regions and unbinding from beaconmanager.
     */
    private void stopRanging() {
        try {
            for (Region r : regionsToMonitor) {
                beaconManager.stopRangingBeaconsInRegion(r);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error trying to stop monitoring", e);
        }
        beaconManager.unbind(this);
        started = false;

        Toast warning = Toast.makeText(this, R.string.beacon_destroy_warning, Toast.LENGTH_SHORT);
        warning.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
        warning.show();
    }


    /**
     * Dummy binder for connecting to activities that bind to this service
     */
    public class LocalBinder extends Binder {
        public LocalBinder() {

        }
    }

    /**
     * Receiver for handling bluetooth and connectivity state change
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                //Bluetooth adapter change
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        //
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i(TAG, "Bluetooth is getting turned off, beacon monitoring stopped.");
                        stopRanging();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i(TAG, "Bluetooth is turned on, starting beacon monitoring.");
                        if (regionsToMonitor.isEmpty()) {
                            fetchRegions();
                        } else {
                            beaconManager.bind(_self); //Prompts monitoring start
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        //
                        break;
                }
            } else if (action.equals(ConnectionChecker.RECONNECT_BROADCAST)) {
                // There is valid connection again -> try to fetch regions and start monitoring
                LocalBroadcastManager.getInstance(context).unregisterReceiver(mReceiver);
                isConnectivityReceiverRegistered = false;

                if (regionsToMonitor.isEmpty()) {
                    fetchRegions();
                }
            }
        }
    };
}
