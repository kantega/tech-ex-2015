package no.kantega.techex.android.tools;

import android.content.Context;
import no.kantega.techex.android.R;

/**
 * Singleton class for easy access of configuration data
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

    private int beaconMonitoringActive;

    private int beaconMonitoringPassive;

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

        beaconMonitoringActive = Integer.valueOf(context.getString(R.string.config_beacon_monitoring_active));
        beaconMonitoringPassive = Integer.valueOf(context.getString(R.string.config_beacon_monitoring_passive));
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

    public String getRegionInfoREST() {
        return REST_baseURL+REST_regionInfo;
    }

    public String getSharedPreferencesId() {
        return sharedPreferencesId;
    }

    public String getGcmProjectId() {
        return gcmProjectId;
    }

    public String getSpUserIdKey() {
        return spUserIdKey;
    }

    public String getSpGcmAuthKey() {
        return spGcmAuthKey;
    }

    public String getSpUserNameKey() {
        return spUserNameKey;
    }

    public int getBeaconMonitoringPassive() {
        return beaconMonitoringPassive;
    }

    public int getBeaconMonitoringActive() {
        return beaconMonitoringActive;
    }

    public String getSpRegionNumberKey() {
        return spRegionNumberKey;
    }
}
