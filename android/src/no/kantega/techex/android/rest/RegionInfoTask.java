package no.kantega.techex.android.rest;

import android.graphics.Bitmap;
import android.util.Log;
import no.kantega.techex.android.tools.Configuration;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Fetches the regions to monitor through REST query
 */
public class RegionInfoTask extends AbstractRESTTask<Integer> {
    private static final String TAG = RegionInfoTask.class.getSimpleName();

    @Override
    protected HttpRequestBase createHttpRequest(String... params) {
        String url = Configuration.getInstance().getRegionInfoREST();
        HttpRequestBase request = new HttpGet(url);

        //Receiving JSON
        request.setHeader("Accept", "application/json");
        return request;
    }

    @Override
    protected Integer parseResponse(int responseCode, String data) {
        try {
            JSONObject response = new JSONObject(data);
            int regions = response.getInt("numberOfRegions");
            return regions;
        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse JSON answer");
            return 0;
        }
    }
}
