package no.kantega.techex.android.rest;

import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Asynch task for sending location updates to the server.
 */
public class LocationUpdateTask extends AbstractRESTTask<Boolean> {

    private final String TAG = LocationUpdateTask.class.getSimpleName();

    /**
     *
     * @param params 0 - URL, 1 - Beacon ID (major:minor), 2 - proximity
     * @return
     */
    @Override
    protected HttpRequestBase createHttpRequest(String... params) {
        if (params.length != 3) {
            Log.e(TAG,"Missing parameters, can't create location update");
            return null;
        }

        String URL =params [0];
        String beaconId = params[1];
        String proximity = params[2];
        try {
            HttpRequestBase request = new HttpPost(URL);

            String json = createJSONData(beaconId, proximity);
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
        return null;
    }

    private String createJSONData(String beaconId, String proximity) {
        JSONObject data = new JSONObject();
        try {
            data.put("beaconId", beaconId);
            data.put("proximity", proximity);
            return data.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Error trying to create JSON data");
            return null;
        }
    }
}
