package no.kantega.techex.android.rest;

import android.util.Log;
import no.kantega.techex.android.rest.wrapper.RegistrationResult;
import no.kantega.techex.android.rest.wrapper.RegistrationResultStatus;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task running in background to handle user registration.
 *
 * Execute params:
 * 0 - nickname
 * 1 - url (including nickname)
 */
public class RegisterTask extends AbstractRESTTask<RegistrationResult> {

    private final String TAG = RegisterTask.class.getSimpleName();

    private String nickName;

    private String gcmId;

    public RegisterTask(OnTaskComplete<RegistrationResult> callback) {
        super(callback);
    }

    @Override
    protected HttpRequestBase createHttpRequest(String... params) {
        try {
            nickName = params[0];
            String url = params[1];
            gcmId = params[2];
            HttpRequestBase request = new HttpPost(url);

            String jsonData = getJsonData();
            Log.d(TAG, "Registering with jsonData: '" + jsonData + "'");
            StringEntity se = new StringEntity(jsonData, "UTF-8");
            se.setContentType("application/json");
            se.setContentEncoding("UTF-8");
            ((HttpPost) request).setEntity(se);

            //Recieving JSON
            request.setHeader("Accept", "application/json");

            return request;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG,"Error trying to build HTTP request for registration",e);
            return null;
        }
    }

    @Override
    protected RegistrationResult parseResponse(int responseCode, String data) {
        RegistrationResult result = new RegistrationResult();
        switch(responseCode) {
            case HttpStatus.SC_OK: // 200
            case HttpStatus.SC_CREATED: //201
                result.setResultStatus(RegistrationResultStatus.SUCCESS);
                try {
                    JSONObject jo = new JSONObject(data);
                    result.setNickname(jo.getString("nick"));
                    result.setId(jo.getString("id"));

                    //Setting preferences
                    JSONObject preferences = jo.getJSONObject("preferences"); //throws exception if doesnt exist
                    Map<String,String> preferenceMap = new HashMap<String,String>();
                    preferenceMap.put("drink",preferences.getString("drink"));
                    preferenceMap.put("eat",preferences.getString("eat"));
                    result.setPreferences(preferenceMap);

                    //Setting quests
                    JSONArray quests = jo.getJSONArray("quests");
                    List<String> questList = new ArrayList<String>();
                    for (int i=0; i<quests.length(); i++ ){
                        questList.add(quests.getString(i));
                    }
                    result.setQuests(questList);
                } catch (JSONException e) {
                    Log.e(TAG,"Server response is not valid JSON: '"+data+"'",e);
                    result.setResultStatus(RegistrationResultStatus.INTERNAL_ERROR);
                }
                break;
            case HttpStatus.SC_CONFLICT: //409
                result.setResultStatus(RegistrationResultStatus.NICK_TAKEN);
                result.setNickname(nickName); //Needs to be set for proper warning message
                Log.i(TAG, "Trying to register already taken username: '" + nickName + "'");
                break;
            default:
                result.setResultStatus(RegistrationResultStatus.INTERNAL_ERROR);
                Log.e(TAG,"Unexpected response code when registering: "+responseCode);
                break;
        }
        return result;
    }

    /**
     * Create JSON message
     * @return
     */
    private String getJsonData() {
        try {
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("nick",nickName);
            JSONObject platform = new JSONObject();
            platform.put("type","android");
            if (gcmId != null) {
                platform.put("deviceToken",gcmId);
            }
            jsonMessage.put("platform",platform);
            //Preferences not used yet
            return jsonMessage.toString();
        } catch (JSONException e) {
            Log.e(TAG,"Error trying to create JSON message for registering",e);
            return null;
        }
    }
}
