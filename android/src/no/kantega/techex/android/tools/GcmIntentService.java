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
 * Created by zsuhor on 27.02.2015.
 */
public class GcmIntentService extends IntentService {

    private static final String TAG = GcmIntentService.class.getSimpleName();

    // Defines a custom Intent action used for broadcasting that user information has changed
    public static final String BROADCAST_ACTION = "no.kantega.techex.android.BROADCAST";

    public static final String BROADCAST_EXTRA_BADGE_ID = "badge";

   private int notificationId;

    public GcmIntentService() {
        super("GcmIntentService");
        notificationId = 1;
    }

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

    // Put the message into a notification and post it.
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
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //loads the already running instance of the QuestList

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
        notificationId +=2;
        mNotificationManager.notify(notificationId, mBuilder.build());
    }
}
