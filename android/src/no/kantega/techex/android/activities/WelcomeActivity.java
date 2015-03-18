package no.kantega.techex.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import no.kantega.techex.android.R;
import no.kantega.techex.android.tools.Configuration;

/**
 * Welcome screen when app is started when user is already registered
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
            }
        });

        // Redirect automatically after a time as well
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                Intent mainIntent = new Intent(WelcomeActivity.this,QuestListActivity.class);
                startActivity(mainIntent);
                //Quits this activity completely, not needed again
                finish();
            }
        }, configuration.getWelcomeRedirectTime()*1000);

    }
}