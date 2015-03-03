package no.kantega.techex.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import no.kantega.techex.android.R;
import no.kantega.techex.android.data.Quest;
import no.kantega.techex.android.display.QuestArrayAdapter;
import no.kantega.techex.android.rest.OnTaskComplete;
import no.kantega.techex.android.rest.UserQuestsTask;
import no.kantega.techex.android.tools.BeaconMonitorListener;
import no.kantega.techex.android.tools.GcmIntentService;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for showing all user quests
 *
 * This activity starts the beacon monitoring! Beacon monitoring doesn't start until this activity is created.
 */
public class QuestListActivity extends Activity implements OnTaskComplete<List<Quest>> {
    private final String TAG = QuestListActivity.class.getSimpleName();

    private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 1;

    private String id;

    private QuestArrayAdapter questArrayAdapter;

    private List<Quest> questList;

    private Intent beaconService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.questlist);
        Log.d(TAG,"OnCreate call");

        SharedPreferences prefs = getSharedPreferences(getString(R.string.config_sharedpref_id),Context.MODE_PRIVATE);
        id = prefs.getString("id",null); //User id

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

            //Intent filters for broadcasts
            IntentFilter brIntentFilters = new IntentFilter();
            //GCM notification
            brIntentFilters.addAction(GcmIntentService.BROADCAST_ACTION);

            //Start beacon monitoring
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.bluetooth_unavailable_msg))
                        .setNeutralButton(getString(R.string.bluetooth_unavailable_btn), null)
                        .show();
            } else {
                if (!mBluetoothAdapter.isEnabled()) {
                    final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //TODO custom message
                    startActivityForResult(intent, REQUEST_CODE_ENABLE_BLUETOOTH);
                } else {
                    //Bluetooth is enabled already
                    startBeaconServiceIntent();
                }
                //Creating listener on bluetooth state in case the user
                brIntentFilters.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            }

            //Receiver for broadcasts
            registerReceiver(mReceiver, brIntentFilters);


        } else {
            Log.e(TAG, "User ID not found, can't load quest list.");
        }
    }

    private void updateQuestList() {
        String address = String.format(getString(R.string.config_server_address)+getString(R.string.config_rest_allquests),id);
        Log.d(TAG, "URL: "+address);
        UserQuestsTask uqt = new UserQuestsTask();
        uqt.setListener(this);
        uqt.execute(address);
        //ProgressDialog?
    }

    /**
     * This function is called when the REST called is finished fetching the list of quests for the user.
     * @param result List of Quest data
     */
    @Override
    public void onTaskComplete(List<Quest> result) {
        if (result != null) {
            Log.d(TAG,"Quest list update");
            questList.clear();
            questList.addAll(result);
            questArrayAdapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "No quests to display.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister broadcast listeners
        unregisterReceiver(mReceiver);
        //Stop beacon monitoring
        if (beaconService != null) {
            stopService(beaconService);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Used for getting the result of the bluetooth enabling activity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_CODE_ENABLE_BLUETOOTH) {
            if(resultCode == Activity.RESULT_OK) {
                startBeaconServiceIntent();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.bluetooth_didntturnon_msg))
                        .setTitle(R.string.bluetooth_didntturnon_title);
                builder.setPositiveButton(getString(R.string.bluetooth_unavailable_btn),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.create().show();
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startBeaconServiceIntent() {

            if (beaconService == null) {
                beaconService = new Intent(QuestListActivity.this, BeaconMonitorListener.class);
                startService(beaconService);
            }

    }

    /**
     * Receiver fo handling bluetooth state change
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                //Bluetooth adapter change
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        //
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i(TAG,"Bluetooth is getting turned off, beacon monitoring stopped.");
                        stopService(beaconService);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i(TAG,"Blutetooth is turned on, starting beacon monitoring.");
                        startBeaconServiceIntent();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        //
                        break;
                }
            } else if(action.equals(GcmIntentService.BROADCAST_ACTION)) {
                Log.d(TAG,"GCM Broadcast received");
                //Quest update notification
                updateQuestList(); //Fetching the most recent data -> onTaskComplete will rebuild view
            }
        }
    };
}