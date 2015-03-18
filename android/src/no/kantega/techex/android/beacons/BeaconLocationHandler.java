package no.kantega.techex.android.beacons;

import android.util.Log;
import no.kantega.techex.android.rest.LocationUpdateTask;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class is directly responsible for handling the beacon updates.
 *
 * The underlying beacon service calls {@link #didRangeBeaconsInRegion(java.util.Collection, org.altbeacon.beacon.Region)} once per
 * active ranging period for every monitored region. Since we want to determine which is the closest beacon at the time, we
 * have to wait for all the region callbacks before calculating the new location with {@link #updateBeaconState()}.
 * If the location (~closest beacon) changed, we send an update to the server. The server only wants to know our current "location",
 * so when it changes we need to send an exit message for the old one.
 */
public class BeaconLocationHandler implements RangeNotifier {
    public static final String TAG = BeaconLocationHandler.class.getSimpleName();

    /**
     * Location change event types
     */
    public static enum BeaconProximityChange {ENTER, EXIT, IGNORE};

    /**
     * Number of regions to aggregate updates for
     */
    private int numberOfRegions;

    /**
     * User id (needed for location update)
     */
    private String userId;

    /**
     * The last saved "location"
     */
    private Beacon currentClosestBeacon = null;

    /**
     * Temporary collection of the latest beacon information.
     */
    private Collection<Beacon> tmpBeaconData;

    /**
     * Flag for seeing how many regions have been updated in the current scanning period.
     */
    private int tmpBeaconRegions;

    /**
     * Set to true if last closest beacon is present in the current scanning
     */
    private boolean tmpHasLastBeacon;

    /**
     * Constructor
     * @param userId user's unique server id
     */
    public BeaconLocationHandler(String userId) {
        this.userId = userId;
        tmpBeaconData = new ArrayList<Beacon>();
        tmpBeaconRegions = 0;
        tmpHasLastBeacon = false;
    }

    /**
     * Set the number of regions to aggregate updates for.
     * Needs to be set before monitoring is started!
     * @param numberOfRegions
     */
    public void setNumberOfRegions(int numberOfRegions) {
        this.numberOfRegions = numberOfRegions;
    }

    /**
     * Callback from beacon monitor service
     * @param collection
     * @param region
     */
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
        Log.d(TAG, "Detected beacons in region " + region.getUniqueId() + " - " + collection.size());
        tmpBeaconData.addAll(collection);
        if (++tmpBeaconRegions == numberOfRegions) {
            updateBeaconState();
        }
    }


    /**
     * Find the beacon from the provided collection that is closest to the device
     * Also sets {@link #tmpHasLastBeacon} if current closest is present in the collection
     * @param beacons collection of beacons
     * @return the beacon with the shortest distance from the device
     */
    private Beacon getClosestBeacon(Collection<Beacon> beacons) {
        Iterator<Beacon> iterator = beacons.iterator();
        Beacon closest = iterator.next(); // start with first
        Log.d(TAG,"First: "+closest.toString()+" :: "+closest.getDistance());

        if (currentClosestBeacon!= null) {
            tmpHasLastBeacon=checkIfSameBeacon(closest,currentClosestBeacon);
        }

        Beacon b;
        while (iterator.hasNext()) {

            b= iterator.next();

            Log.d(TAG,""+b.toString()+" :: "+b.getDistance());

            if (!tmpHasLastBeacon && (currentClosestBeacon != null)) {
                tmpHasLastBeacon = checkIfSameBeacon(b,currentClosestBeacon);
            }

            if (b.getDistance() < closest.getDistance() && (BeaconProximity.compareProximity(b,closest)<0)) {
                closest = b;
            }
        }
        return closest;
    }

    /**
     * Check if two beacon objects are identical based on their ids
     * @param a
     * @param b
     * @return true if the two beacon's major ids are identical and their minor ids are also identical
     */
    private boolean checkIfSameBeacon(Beacon a, Beacon b) {
        return (a.getId2().toInt()==b.getId2().toInt()) && (a.getId3().toInt()==b.getId3().toInt());
    }

    /**
     * Pushes location update to the server using the asynchtask that runs in the background
     * @param beacon current closest beacon
     * @param change change in the beacon's perceived state
     */
    public void sendUpdate(Beacon beacon, BeaconProximityChange change) {
        new LocationUpdateTask().execute(userId,
                beacon.getId2().toString(),
                beacon.getId3().toString(),
                BeaconProximity.getProximity(beacon).name().toLowerCase(),
                change.name().toLowerCase()); //new AsyncTask needed for every request

    }

    /**
     * Should be called after receiving the data from all monitored regions. Finds the current closest beacon,
     * compares it to the previously closest beacon and determines the location change (if any).
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
                    if(BeaconProximity.compareProximity(closest,currentClosestBeacon)<0 || !tmpHasLastBeacon) {
                        //The new closest beacon is in a closer proximity class or last location is missing from the update

                        //Exit previous closest
                        sendUpdate(currentClosestBeacon, BeaconProximityChange.EXIT);

                        // Entering new closest beacon
                        sendUpdate(closest, BeaconProximityChange.ENTER);
                        currentClosestBeacon = closest;
                    }
                } else {
                    // The found closest is the same as the last saved closest. Check if we are further away
                    if (BeaconProximity.getProximity(closest) == BeaconProximity.FAR) {
                        sendUpdate(closest, BeaconProximityChange.EXIT);
                        currentClosestBeacon = null;
                    }
                }
            } else {
                //No current closest
                BeaconProximity proximity = BeaconProximity.getProximity(closest);
                if (proximity == BeaconProximity.IMMEDIATE || proximity == BeaconProximity.NEAR) {
                    // Entering new closest beacon
                    sendUpdate(closest, BeaconProximityChange.ENTER);
                    currentClosestBeacon = closest;
                }
            }
            tmpBeaconData.clear();
            tmpBeaconRegions=0;
            tmpHasLastBeacon=false;
        } else {
            //Collection is empty. If the last saved current beacon was from this region, we need to exit
            if (currentClosestBeacon != null) {
                Log.d(TAG, "Exiting because no more data for region");
                sendUpdate(currentClosestBeacon, BeaconProximityChange.EXIT);
                currentClosestBeacon = null;
            }
            tmpBeaconRegions=0;
            tmpHasLastBeacon=false;
        }
    }
}
