package no.kantega.techex.android.rest;

import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Abstract class for handling common functionalities needed for the REST queries
 *
 * T - result wrapper class
 */
public abstract class AbstractRESTTask<T> extends AsyncTask<String,Void,T> {

    private final String TAG = AbstractRESTTask.class.getSimpleName();

    private OnTaskComplete<T> listener;

    public AbstractRESTTask() {
        listener = null;
    }

    public AbstractRESTTask(OnTaskComplete<T> l) {
        listener = l;
    }

    public void setListener(OnTaskComplete<T> l) {
        listener = l;
    }

    @Override
    protected T doInBackground(String... params) {
        //TODO check connection
        try {
            HttpClient client = new DefaultHttpClient();

            //Get Request from specific implementation
            HttpRequestBase request = createHttpRequest(params);

            if (request == null) return null;

            Log.d(TAG,"Rest request: "+request.getMethod()+" _ "+request.getURI().toString());

            HttpResponse response = client.execute(request);

            //HTTP response code
            Integer responseCode = null;
            StatusLine statusLine = response.getStatusLine();
            if (statusLine != null) {
                responseCode = statusLine.getStatusCode();
                Log.d(TAG,"Response code: "+responseCode);
            } else {
                Log.e(TAG,"Couldn't get HTTP response status");
                return null;
            }

            //HTTP response body
            String responseBody = getResponseBody(response.getEntity());
            Log.v(TAG,"Request answer: '"+responseBody+"'");

            return parseResponse(responseCode,responseBody);

        } catch (Exception e) {
            Log.e(TAG, "Error trying to make REST request",e);
            return null;
        }
    }

    protected abstract HttpRequestBase createHttpRequest(String... params);

    protected abstract T parseResponse(int responseCode, String data);

    @Override
    protected void onPostExecute(T t) {
        super.onPostExecute(t);
        if (listener != null) {
            listener.onTaskComplete(t);
        }
    }

    protected String getResponseBody(HttpEntity entity) {
        try {
            StringBuilder outputBuilder = new StringBuilder();
            BufferedReader bufferedReader = null;
            InputStreamReader inputStreamReader = null;
            InputStream inputStream = null;
            if (entity != null) {
                inputStream = entity.getContent();
                inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);

                String line = "";
                while ((line = bufferedReader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }
            }
            return outputBuilder.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error trying to read HTTP response body", e);
            return null;
        }
    }
}
