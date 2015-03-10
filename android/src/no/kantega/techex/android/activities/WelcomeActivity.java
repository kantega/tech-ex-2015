package no.kantega.techex.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import no.kantega.techex.android.R;
import no.kantega.techex.android.tools.Configuration;

/**
 * Created by zsuhor on 24.02.2015.
 */
public class WelcomeActivity extends Activity {
    private final String TAG = WelcomeActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Configuration configuration = Configuration.getInstance();

        SharedPreferences prefs = getSharedPreferences(configuration.getSharedPreferencesId(), Context.MODE_PRIVATE);
        String name = prefs.getString(configuration.getSpUserNameKey(),null); //Registered nickname
        TextView tv = (TextView)findViewById(R.id.welcome_text);
        tv.setText(String.format(getString(R.string.welcome_text),name));

        //Adding click listener to entire application screen
        LinearLayout root = (LinearLayout) findViewById(R.id.mainlayout);
        root.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                Intent i = new Intent(WelcomeActivity.this, QuestListActivity.class);
                WelcomeActivity.this.startActivity(i);
                Log.i(TAG, "Loading quests.");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"Paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"Resumed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"Destroyed");
    }
}