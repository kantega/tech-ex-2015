package no.kantega.techex.android.tools;

import android.content.Context;
import no.kantega.techex.android.R;

/**
 * Singleton class for easy access of configuration data
 *
 * //TODO
 *
 */
public class Configuration {

    //Singleton instance
    private static Configuration instance;

    //API base URL
    private String REST_baseURL;

    //REST method for registering
    private String REST_register;

    //REST method for fetching all quest data
    private String REST_getAllQuests;

    //REST method for fetching all quest data
    private String REST_pushLocation;


    private Configuration() {

    }

    public static Configuration getInstance () {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    //Configuration data is loaded from values/configuration.xml
    public static void init(Context context) {

        //REST_baseURL = context.getString(R.string.config_server_address);
    }

}
