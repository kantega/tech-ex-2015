package no.kantega.techex.android.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import no.kantega.techex.android.R;
import no.kantega.techex.android.data.Quest;
import no.kantega.techex.android.display.AchievementArrayAdapter;

/**
 * Created by zsuhor on 24.02.2015.
 */
public class QuestDetailsActivity extends Activity {
    private static final String TAG = QuestDetailsActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quest_details);

        Quest quest = (Quest) getIntent().getExtras().getParcelable("quest");

        if (quest == null) {
            Log.e(TAG,"Failed to pass quest data between activities");
        } else {
//            TextView tvTitle = (TextView) findViewById(R.id.quest_details_title);
//            tvTitle.setText(quest.getTitle());

            //Showing achievements in list
            ListView lv = (ListView) findViewById(R.id.lvAchievements);
            AchievementArrayAdapter adapter = new AchievementArrayAdapter(this,quest.getAchievements());
            lv.setAdapter(adapter);
        }
    }
}