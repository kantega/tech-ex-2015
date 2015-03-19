package no.kantega.techex.android.tools;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import no.kantega.techex.android.R;
import no.kantega.techex.android.activities.QuestListActivity;

/**
 * Background service for handling the GCM broadcast received when the user
 * gets a badge. The service creates a notification in the status bar and sends
 * local broadcast to the activities to trigger UI update.
 */
public class GcmIntentService extends IntentService {

    private static final String TAG = GcmIntentService.class.getSimpleName();

    /**
     * Local broadcast intent id which is sent out by the service
     */
    public static final String BROADCAST_ACTION = "no.kantega.techex.android.BROADCAST_GCM";

    /**
     * When broadcasting to activities, the broadcast intent includes the id of the newly received
     * badge - it can be accessed with this key
     */
    public static final String BROADCAST_EXTRA_BADGE_ID = "badge";

   private static int notificationId = 1;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    /**
     * This method is called when a new GCM is received.
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            //New achievement
            String id = extras.getString("id");

            //Achievement received!
            String msg = extras.getString("message");

            //Intent to advertise data update (can have extras if needed)
            Intent localIntent = new Intent(BROADCAST_ACTION);
            localIntent.putExtra(BROADCAST_EXTRA_BADGE_ID,id);
            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

            sendNotification(msg);
        } else {
            //Other message types not used
            Log.w(TAG,"Message type '"+messageType+"' is not processed.");
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    /**
     * Creates and sends a status bar notification.
     * Clicking the notification attempts to open the already running version of {@link QuestListActivity}
     * @param msg Text body of the notification (received from server)
     */
    private void sendNotification(String msg) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notfication_icon)
                        .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.badge_achievement))
                        .setContentTitle("New achievement")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setContentText(msg);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, QuestListActivity.class);
        //Clear the stack above activity and go to the already running version
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(QuestListActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        Log.d(TAG,"Sending notification #"+notificationId);
        mNotificationManager.notify(notificationId++, mBuilder.build());
    }
}
