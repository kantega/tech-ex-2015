package no.kantega.techex.android.tools;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import no.kantega.techex.android.rest.OnTaskComplete;

import java.io.IOException;

/**
 * ASyncTask for registering to GCM in the background
 */
public class GCMRegisterTask extends AsyncTask<String,Void,String> {
    private static final String TAG = GCMRegisterTask.class.getSimpleName();

    private OnTaskComplete<String> listener;

    private Context context;

    public GCMRegisterTask(OnTaskComplete<String> listener, Context context) {
        this.listener = listener;
        this.context = context;
    }

    /**
     * Registers the device to GCM
     * @param params 0 - GCM project id
     * @return
     */
    @Override
    protected String doInBackground(String... params) {
        if (params.length < 1) return null;

        String gcmProjectId = params[0];
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        try {
            return gcm.register(gcmProjectId);
        } catch (IOException e) {
            Log.e(TAG, "Error trying to register for GCM.", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Log.i(TAG, "Registered for GCM, received id: "+s);
        if (listener != null) {
            listener.onTaskComplete(s); //Called with null as well to let task know how to continue
        }
    }
}
