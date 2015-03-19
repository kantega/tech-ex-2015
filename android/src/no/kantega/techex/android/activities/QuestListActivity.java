package no.kantega.techex.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import no.kantega.techex.android.R;
import no.kantega.techex.android.beacons.AltBeaconMonitorListener;
import no.kantega.techex.android.data.Quest;
import no.kantega.techex.android.display.QuestArrayAdapter;
import no.kantega.techex.android.rest.OnTaskComplete;
import no.kantega.techex.android.rest.UserQuestsTask;
import no.kantega.techex.android.tools.Configuration;
import no.kantega.techex.android.tools.ConnectionChecker;
import no.kantega.techex.android.tools.GcmIntentService;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for showing all user quests
 *
 * This activity is the first to bind to the beacon monitoring service. Beacon monitoring is only active while there is
 * a running instance of this activity.
 */
public class QuestListActivity extends Activity {
    private static final String TAG = QuestListActivity.class.getSimpleName();

    /**
     * User id
     */
    private String id;

    /**
     * Adapter for list view display of quests
     */
    private static QuestArrayAdapter questArrayAdapter;

    private static List<Quest> questList;

    private Context context;

    private static ProgressBar spinner;

    private static SharedPreferences prefs;

    /**
     * Will be set to true if class was properly initialized - needed for cleanup
     */
    private boolean wasInitialized;

    /**
     * Set to true if activity receives broadcast on network reconnect - needed for cleanup
     */
    private boolean isRegisteredForReconnect;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.questlist);
        Log.d(TAG,"OnCreate call");

        Configuration configuration = Configuration.getInstance();
        context=this;

        prefs = getSharedPreferences(configuration.getSharedPreferencesId(),Context.MODE_PRIVATE);
        id = prefs.getString(configuration.getSpUserIdKey(),null); //User id

        spinner = (ProgressBar)findViewById(R.id.progressSpinner);
        spinner.setVisibility(View.GONE); //on default it should be gone

        isRegisteredForReconnect=false;

        if (id != null) {
            //Setting up quest list display adapter
            questList = new ArrayList<Quest>();
            questArrayAdapter = new QuestArrayAdapter(this,questList);
            ListView listView = (ListView) findViewById(R.id.lvQuests);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Quest item = (Quest) parent.getItemAtPosition(position);
                    Intent intent = new Intent(QuestListActivity.this,QuestDetailsActivity.class);
                    intent.putExtra("quest",item);
                    QuestListActivity.this.startActivity(intent);
                }
            });
            listView.setAdapter(questArrayAdapter);
            //Fetching list of quests
            updateQuestList();

            //Binding to beacon monitoring (it'll get started if this the first bind)
            Intent intent = new Intent(this, AltBeaconMonitorListener.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

            //Receiver for broadcasts (GCM and connection change notifications)
            IntentFilter filters = new IntentFilter();
            filters.addAction(GcmIntentService.BROADCAST_ACTION);
            LocalBroadcastManager.getInstance(this).registerReceiver(mGCMReceiver,filters);

            wasInitialized=true;
        } else {
            Toast.makeText(context, R.string.questlist_error, Toast.LENGTH_SHORT);
            Log.e(TAG, "User ID not found, can't load quest list.");
            wasInitialized=false;
        }
    }

    /**
     * Fetch the user's quests from the server
     *
     * This function starts the asynchronous task ({@link UserQuestsTask}) to download the quests.
     * When the quests are available {@link #questTaskCompleter} is called.
     */
    private void updateQuestList() {
        if (ConnectionChecker.getInstance().checkConnection(this,"loading quests",true)) {
            spinner.setVisibility(View.VISIBLE);
            UserQuestsTask uqt = new UserQuestsTask();
            uqt.setListener(questTaskCompleter);
            uqt.execute(id);
        } else {
            // No connection available => register to receive change broadcast
            if (!isRegisteredForReconnect) {
                IntentFilter filters = new IntentFilter();
                filters.addAction(ConnectionChecker.RECONNECT_BROADCAST);
                LocalBroadcastManager.getInstance(this).registerReceiver(mConnectionReceiver, filters);
                isRegisteredForReconnect = true;
            }
            Log.d(TAG, "No active network connection, not loading quests.");
        }
    }

    /**
     * This object is called when the REST call is finished fetching the list of quests for the user.
     * @param result List of Quest data
     */
    private OnTaskComplete<List<Quest>> questTaskCompleter = new OnTaskComplete<List<Quest>>() {
        @Override
        public void onTaskComplete(List<Quest> result) {
            if (result != null) {
                Log.d(TAG, "Quest list update");
                questList.clear();
                questList.addAll(result);
                questArrayAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(context, R.string.questlist_noquest_toast, Toast.LENGTH_SHORT);
                Log.e(TAG, "No quests to display.");
            }
            spinner.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy call");
        if (wasInitialized) {
            // Unregister broadcast listeners
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mGCMReceiver);
            if (isRegisteredForReconnect) {
                // Unregister broadcast listeners
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mConnectionReceiver);
            }
            //Unbind from beacon monitoring
            unbindService(mConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Binding to beacon monitoring again in case it fell asleep (can happen if the phone was not used at all for a long time)
        Intent intent = new Intent(this, AltBeaconMonitorListener.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //Make sure quest list is up-to-date
        updateQuestList();
    }

    /**
     * Dummy connection to the beacon service
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    /**
     * Overriding Back button to warn user:
     * if they use this, beacon monitoring will stop.
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG,"Back pressed");

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.questlist_leaving_dialog_title)
                .setMessage(R.string.questlist_leaving_dialog_msg);
        builder.setPositiveButton(R.string.questlist_leaving_dialog_btn_stay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setNegativeButton(R.string.questlist_leaving_dialog_btn_quit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Leaving the app
                doOnBackPressed();
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    private void doOnBackPressed() {
        super.onBackPressed();
    }

    /**
     * Receives broadcasts from other activities -
     * * on new notification (GCM broadcast)
     * refresh view
     */
    private final BroadcastReceiver mGCMReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG,"Refresh broadcast received: "+action);
            //Quest update notification
            updateQuestList(); //Fetching the most recent data -> onTaskComplete will rebuild view
        }
    };

    /**
     * Receives broadcasts from other activities -
     * * on connectivity change
     * refresh view
     */
    private final BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG,"Refresh broadcast received: "+action);
            //Quest update notification
            updateQuestList(); //Fetching the most recent data -> onTaskComplete will rebuild view
            //Unregister connectivity receiver
            LocalBroadcastManager.getInstance(context).unregisterReceiver(mConnectionReceiver);
            isRegisteredForReconnect = false;
        }
    };
}