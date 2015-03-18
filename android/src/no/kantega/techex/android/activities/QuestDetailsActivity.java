package no.kantega.techex.android.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import no.kantega.techex.android.R;
import no.kantega.techex.android.data.Quest;
import no.kantega.techex.android.display.AchievementArrayAdapter;
import no.kantega.techex.android.tools.GcmIntentService;

/**
 * This activity displays the details of a quest (list of achievements).
 *
 * It needs to receive the Quest object as an Intent extra with the key "quest"
 */
public class QuestDetailsActivity extends Activity {
    private static final String TAG = QuestDetailsActivity.class.getSimpleName();

    private Quest quest;

    /**
     * Adapter for generating list view of achievements
     */
    private AchievementArrayAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quest_details);

        quest = (Quest) getIntent().getExtras().getParcelable("quest");

        if (quest == null) {
            Log.e(TAG,"Failed to pass quest data between activities");
        } else {
            TextView tv = (TextView) findViewById(R.id.qdTitle);
            tv.setText(quest.getTitle());

            //Showing achievements in list
            ListView lv = (ListView) findViewById(R.id.lvAchievements);
            adapter = new AchievementArrayAdapter(this,quest.getAchievements());
            lv.setAdapter(adapter);

            //Register for change broadcast
            LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,new IntentFilter(GcmIntentService.BROADCAST_ACTION));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (quest != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        }
    }

    /**
     * Receiver for badge notification broadcast -> might need to refresh view
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(GcmIntentService.BROADCAST_ACTION)) {
                Log.d(TAG,"GCM Broadcast received");
                String newAchievement = intent.getStringExtra(GcmIntentService.BROADCAST_EXTRA_BADGE_ID);
                //Check & update if achievement is for this quest
                if (quest.updateAchievement(newAchievement)) {
                    //The new achievement as for this quest, update the UI
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };
}