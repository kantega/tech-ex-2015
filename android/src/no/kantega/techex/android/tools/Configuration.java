package no.kantega.techex.android.tools;

import android.content.Context;
import no.kantega.techex.android.R;

/**
 * Singleton class for easy access of configuration data that's stored in res/values/configuration.xml
 */
public class Configuration {

    //Singleton instance
    private static Configuration instance;

    /**
     * Server REST API
     */
    //REST API base URL
    private String REST_baseURL;

    //REST method for registering
    private String REST_register;

    //REST method for fetching all quest data
    private String REST_getAllQuests;

    //REST method for fetching all quest data
    private String REST_pushLocation;

    // REST for getting regions to monitor
    private String REST_regionInfo;

    /**
     * Shared preferences
     */
    //Shared preferences ID
    private String sharedPreferencesId;

    //Shared preferences key for user id
    private String spUserIdKey;

    private String spUserNameKey;

    private String spGcmAuthKey;

    private String spRegionNumberKey;

    /**
     * Google Cloud Messaging
     */
    //GCM project id
    private String gcmProjectId;

    /**
     * Beacon monitoring
     */

    private int beaconMonitoringForegroundActive;

    private int beaconMonitoringForegroundPassive;

    private int beaconMonitoringBackgroundActive;

    private int beaconMonitoringBackgroundPassive;

    private String regionUUID;

    /**
     * Other
     */
    private int welcomeRedirectTime;

    //Constructor
    private Configuration() {

    }

    public static Configuration getInstance () {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    //Configuration data is loaded from values/configuration.xml
    public void init(Context context) {
        REST_baseURL = context.getString(R.string.config_server_address);
        REST_register = context.getString(R.string.config_rest_registration);
        REST_getAllQuests = context.getString(R.string.config_rest_allquests);
        REST_pushLocation = context.getString(R.string.config_rest_locationupdate);
        REST_regionInfo = context.getString(R.string.config_rest_regioninfo);

        sharedPreferencesId = context.getString(R.string.config_sharedpref_id);
        spUserIdKey = context.getString(R.string.config_sharedpref_useridkey);
        spUserNameKey = context.getString(R.string.config_sharedpref_usernamekey);
        spGcmAuthKey = context.getString(R.string.config_sharedpref_gcmauthkey);
        spRegionNumberKey = context.getString(R.string.config_sharedpref_region_number);

        gcmProjectId = context.getString(R.string.config_gcm_project_id);

        beaconMonitoringForegroundActive = Integer.valueOf(context.getString(R.string.config_beacon_monitoring_foreground_active));
        beaconMonitoringForegroundPassive = Integer.valueOf(context.getString(R.string.config_beacon_monitoring_foreground_passive));
        beaconMonitoringBackgroundActive = Integer.valueOf(context.getString(R.string.config_beacon_monitoring_background_active));
        beaconMonitoringBackgroundPassive = Integer.valueOf(context.getString(R.string.config_beacon_monitoring_background_passive));
        regionUUID = context.getString(R.string.config_beacon_region_uuid);

        welcomeRedirectTime = Integer.valueOf(context.getString(R.string.config_welcome_redirect_time));
    }

    /**
     * Returns the rest address for getting all user quests
     * @param userId User unique server id
     * @return
     */
    public String getAllQuestsREST(String userId) {
        return String.format(REST_baseURL+REST_getAllQuests,userId);
    }

    /**
     * Returns the REST address for registration
     * @return
     */
    public String getRegistrationREST(){
        return REST_baseURL+REST_register;
    }

    /**
     * Returns the REST address for location update
     * @param userId User unique server id
     * @return
     */
    public String getLocationREST(String userId) {
        return String.format(REST_baseURL+REST_pushLocation,userId);
    }

    /**
     * Returns the REST address for fetching region information
     * @return
     */
    public String getRegionInfoREST() {
        return REST_baseURL+REST_regionInfo;
    }

    /**
     * Get the ID of the shared preferences used by the application
     * @return
     */
    public String getSharedPreferencesId() {
        return sharedPreferencesId;
    }

    /**
     * Returns the project id of our Google Cloud Messaging server
     * @return
     */
    public String getGcmProjectId() {
        return gcmProjectId;
    }

    /**
     * Returns the shared preferences key used for storing the user id
     * @return
     */
    public String getSpUserIdKey() {
        return spUserIdKey;
    }

    /**
     * Returns the shared preferences key used for storing the GCM registration id
     * @return
     */
    public String getSpGcmAuthKey() {
        return spGcmAuthKey;
    }

    /**
     * Returns the shared preferences key used for storing the user name
     * @return
     */
    public String getSpUserNameKey() {
        return spUserNameKey;
    }

    /**
     * Returns the active period length of beacon scanning the the application is in the foreground
     * @return time in milliseconds
     */
    public int getBeaconMonitoringForegroundActive() {
        return beaconMonitoringForegroundActive;
    }

    /**
     * Returns the active passive length of beacon scanning the the application is in the foreground
     * @return time in milliseconds
     */
    public int getBeaconMonitoringForegroundPassive() {
        return beaconMonitoringForegroundPassive;
    }

    /**
     * Returns the active period length of beacon scanning the the application is in the background
     * @return time in milliseconds
     */
    public int getBeaconMonitoringBackgroundActive() {
        return beaconMonitoringBackgroundActive;
    }

    /**
     * Returns the passive period length of beacon scanning the the application is in the background
     * @return time in milliseconds
     */
    public int getBeaconMonitoringBackgroundPassive() {
        return beaconMonitoringBackgroundPassive;
    }

    /**
     * Returns the shared preferences key used for storing the number of monitored regions
     * @return
     */
    public String getSpRegionNumberKey() {
        return spRegionNumberKey;
    }

    /**
     * Returns the UUID of the beacons
     * @return
     */
    public String getRegionUUID() {
        return regionUUID;
    }

    /**
     * Returns how many seconds to wait on welcome screen before automatically redirecting to quest list
     * @return
     */
    public int getWelcomeRedirectTime() {
        return welcomeRedirectTime;
    }
}
