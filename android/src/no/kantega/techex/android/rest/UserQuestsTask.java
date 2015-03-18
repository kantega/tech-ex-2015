package no.kantega.techex.android.rest;

import android.util.Log;
import no.kantega.techex.android.data.Achievement;
import no.kantega.techex.android.data.Quest;
import no.kantega.techex.android.tools.Configuration;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetching all Quests for the user
 */
public class UserQuestsTask extends AbstractRESTTask<List<Quest>> {

    private final String TAG = UserQuestsTask.class.getSimpleName();

    /**
     * Http get request for fetching user quests
     * @param params 0 - user id
     * @return the request
     */
    @Override
    protected HttpRequestBase createHttpRequest(String... params) {
        String id = params[0];
        String url = Configuration.getInstance().getAllQuestsREST(id);
        HttpRequestBase request = new HttpGet(url);

        //Receiving JSON
        request.setHeader("Accept", "application/json");

        return request;
    }

    @Override
    protected List<Quest> parseResponse(int responseCode, String data) {
        if (responseCode != HttpStatus.SC_OK) {
            Log.e(TAG,"Error trying to load quest data for user.");
            return null;
        }

        //Only handling 200 - OK event, parsing JSON data
        List<Quest> questList = new ArrayList<Quest>();
        try {
            JSONArray questArray = new JSONArray(data);

            Quest tmpQuest = null;
            for (int i = 0; i<questArray.length(); i++) {
                JSONObject questObject = questArray.getJSONObject(i);
                tmpQuest = getQuestFromJSON(questObject);
                if (tmpQuest != null) {
                    questList.add(tmpQuest);
                }
            }
            Log.d(TAG,"Refreshed quest list.");

        } catch (JSONException e) {
            Log.e(TAG,"Error parsing response: "+data);
        }

        return questList;
    }

    /**
     * Parses the Json object into the interal Quest representation
     * @param data Json object received in rest request
     * @return quest data
     */
    private Quest getQuestFromJSON(JSONObject data) {
        Quest q = new Quest();
        try {
            q.setId(data.getString("id"));
            q.setTitle(data.getString("title"));
            q.setVisibility(data.getString("visibility"));
            q.setDescription(data.getString("desc"));

            JSONArray achievementArray = data.getJSONArray("achievements");
            for (int j = 0; j<achievementArray.length(); j++) {
                Achievement tmpAchievement = new Achievement();
                JSONObject achievementObject = achievementArray.getJSONObject(j);
                tmpAchievement.setDescription(achievementObject.getString("desc"));
                tmpAchievement.setTitle(achievementObject.getString("title"));
                tmpAchievement.setId(achievementObject.getString("id"));
                tmpAchievement.setAchieved(achievementObject.getBoolean("achieved"));
                q.addAchievement(tmpAchievement);
            }
        } catch (JSONException e) {
            Log.e(TAG,"Error parsing quest data from: '"+data.toString()+"'");
        }
        return q;
    }
}
