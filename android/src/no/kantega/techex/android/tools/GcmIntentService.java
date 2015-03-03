package no.kantega.techex.android.tools;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
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

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            //Achievement received!
            String msg = extras.getString("message");

            //Intent to advertise data update (can have extras if needed)
            Intent localIntent = new Intent(BROADCAST_ACTION);
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

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        // Adds the back stack for the Intent (but not the Intent itself)
//        stackBuilder.addParentStack(QuestListActivity.class);
//        // Adds the Intent that starts the Activity to the top of the stack
//        stackBuilder.addNextIntent(resultIntent);
//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(
//                        0,
//                        PendingIntent.FLAG_UPDATE_CURRENT
//                );

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        mNotificationManager.notify(1, mBuilder.build());
    }
}
