package no.kantega.techex.android.rest;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Abstract class for handling common functions needed for the REST queries
 *
 * T - result wrapper class
 */
public abstract class AbstractRESTTask<T> extends AsyncTask<String,Void,T> {

    private final String TAG = AbstractRESTTask.class.getSimpleName();

    /**
     * Listener that needs to be called with the results when the task is completed.
     */
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

    /**
     * This method is run in a background thread. Creates the http request based on the parameters
     * ({@link #createHttpRequest(String...)}, executes it, then parses the result ({@link #parseResponse(int, String)}.
     * The class can be customized for specific REST queries by overriding the aforementioned methods.
     * The returned result is sent to the completion listener in {@link #onPostExecute(Object)}
     * @param params String parameters for creating the Http request
     * @return Specific response of the Rest request
     */
    @Override
    protected T doInBackground(String... params) {
        try {
            DefaultHttpClient client = new DefaultHttpClient();

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

    /**
     * Create the http request that needs to be executed. Should include URL, http method
     * and every other necessary detail.
     * @param params The parameters received by the AsynchTask on the execute call
     * @return configured http request ready for running
     */
    protected abstract HttpRequestBase createHttpRequest(String... params);

    /**
     * Creates the custom response of the asynchronous task.
     * @param responseCode Http response code
     * @param data Http response body
     * @return custom response
     */
    protected abstract T parseResponse(int responseCode, String data);

    /**
     * Calls the task completion listener and passes the results to it.
     * @param t response created by {@link #parseResponse(int, String)}
     */
    @Override
    protected void onPostExecute(T t) {
        super.onPostExecute(t);
        if (listener != null) {
            listener.onTaskComplete(t);
        }
    }

    /**
     * Reads the http response body into a string
     * @param entity http response entity
     * @return string of the body contents (usually json)
     */
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
