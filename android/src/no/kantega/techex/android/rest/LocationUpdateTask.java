package no.kantega.techex.android.rest;

import android.util.Log;
import no.kantega.techex.android.tools.Configuration;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Asynch task for sending location updates to the server.
 */
public class LocationUpdateTask extends AbstractRESTTask<Boolean> {

    private final String TAG = LocationUpdateTask.class.getSimpleName();

    /**
     * Creates the Http Post request for sending location data
     * @param params 0 - userId,
     *               1 - beacon id (major),
     *               2 - beacon id (minor),
     *               3 - proximity ("near"|"far"|"immediate"),
     *               4 - change ("enter"|"exit")
     * @return the request
     */
    @Override
    protected HttpRequestBase createHttpRequest(String... params) {
        if (params.length != 5) {
            Log.e(TAG,"Missing parameters, can't create location update");
            return null;
        }

        String userId =params [0];
        String major = params[1];
        String minor = params[2];
        String proximity = params[3];
        String change = params[4];

        try {
            String URL = Configuration.getInstance().getLocationREST(userId);
            HttpRequestBase request = new HttpPost(URL);

            String json = createJSONData(major,minor, proximity,change);
            Log.d(TAG,"Location update: "+json);

            StringEntity se = new StringEntity(json, "UTF-8");
            se.setContentType("application/json");
            se.setContentEncoding("UTF-8");
            ((HttpPost) request).setEntity(se);

            return request;
        }catch (UnsupportedEncodingException e) {
            Log.e(TAG,"Error trying to build location update request",e);
            return null;
        }
    }

    /**
     * Creates boolean response
     * @param responseCode Http response code
     * @param data Http response body
     * @return true if location update was successful
     */
    @Override
    protected Boolean parseResponse(int responseCode, String data) {
        boolean returnVal;
        switch(responseCode) {
            case HttpStatus.SC_OK: //200 -success
                Log.d(TAG,"Status successfully updated");
                returnVal = true;
                break;
            default:
                Log.e(TAG,"Failed to update location status on server");
                returnVal = false;
                break;
        }
        return returnVal;
    }

    /**
     * Creates the json object that's passed on the post call, as defined by the API.
     * @param major beacon major id
     * @param minor beacon minor id
     * @param proximity beacon current proximity
     * @param change beacon proximity change
     * @return JSON string
     */
    private String createJSONData(String major, String minor, String proximity, String change) {
        JSONObject data = new JSONObject();
        try {
            data.put("major", major);
            data.put("minor", minor);
            data.put("proximity", proximity);
            data.put("activity", change);
            return data.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Error trying to create JSON data");
            return null;
        }
    }
}
