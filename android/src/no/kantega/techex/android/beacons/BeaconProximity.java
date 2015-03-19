package no.kantega.techex.android.beacons;

import org.altbeacon.beacon.Beacon;

/**
 * Beacon distance types (in accordance with the Kontakt:IO terminology)
 */
public enum BeaconProximity {
    UNKNOWN(100), IMMEDIATE(0), NEAR(1), FAR(2);

    int strength;

    BeaconProximity(int i) {
        strength = i;
    }

    /**
     * Get the proximity category of the beacon based on the formula used by the Kontakt SDK
     * @param beacon
     * @return proximity category
     */
    public static BeaconProximity getProximity(Beacon beacon) {
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
     *
     * @param a
     * @param b
     * @return negative, if a is closer than b;
     *          0, if a and b are the same proximity from the device;
     *          0<, if a is further away than b
     */
    public static int compareProximity(Beacon a, Beacon b) {
        return getProximity(a).strength - getProximity(b).strength;
    }
};