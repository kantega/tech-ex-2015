package no.kantega.techex.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import no.kantega.techex.android.R;
import no.kantega.techex.android.rest.OnTaskComplete;
import no.kantega.techex.android.rest.RegisterTask;
import no.kantega.techex.android.rest.wrapper.RegistrationResult;
import no.kantega.techex.android.rest.wrapper.RegistrationResultStatus;
import no.kantega.techex.android.tools.Configuration;
import no.kantega.techex.android.tools.ConnectionChecker;
import no.kantega.techex.android.tools.GCMRegisterTask;

/**
 * Activity for registering new user
 *
 * When login button is pressed =>
 * * Registers to Google Cloud Messaging
 * * Registers on server
 */
public class RegisterActivity extends Activity {
    private final String TAG = RegisterActivity.class.getSimpleName();

    private Configuration configuration;

    private Context context;

    /**
     * The GCM project id for the notification server (loaded from config)
     */
    private String gcmProjectId;

    /**
     * The GCM registration id for the device
     */
    private String gcmRegistrationId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        configuration = Configuration.getInstance();
        gcmProjectId = configuration.getGcmProjectId();

        Button b = (Button)findViewById(R.id.btnRegister);

        context = this;

        //Login button listener
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText et = (EditText) findViewById(R.id.nickname);
                String enteredNN = et.getText().toString().trim();

                if (!enteredNN.isEmpty()) {
                    if (ConnectionChecker.getInstance().checkConnection(context, "login", true)) {
                        if (gcmRegistrationId == null) {
                            //First registering to GCM, and that will trigger server registration
                            GCMRegisterTask task = new GCMRegisterTask(gcmCompleter, context);
                            task.execute(gcmProjectId);
                        } else {
                            //Already registered for GCM, register on server
                            registerOnServer();
                        }
                    }
                }
            }
        });
    }

    /**
     * This object is called when the asynchronous GCM registration task is finished.
     * If that was successful, it triggers registration on server.
     */
    private OnTaskComplete<String> gcmCompleter = new OnTaskComplete<String>() {
        @Override
        public void onTaskComplete(String result) {
            if (result != null) {
                //Save GCM ID
                SharedPreferences.Editor editor = getSharedPreferences(Configuration.getInstance().getSharedPreferencesId(),Context.MODE_PRIVATE).edit();
                editor.putString("gcm_id",result);
                editor.commit();

                gcmRegistrationId = result;

                registerOnServer();
            } else {
                //Couldn't register to GCM - alert.
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom));
                builder.setMessage(R.string.register_gcm_fail_msg)
                        .setTitle(R.string.register_gcm_fail_title);
                builder.setPositiveButton(getString(R.string.register_gcm_fail_btn),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.create().show();
            }
        }
    };

    /**
     * Starts asynchronous REST task for registering use ron the server.
     */
    private void registerOnServer() {
        //Get name from input
        EditText et = (EditText) findViewById(R.id.nickname);
        String enteredNN = et.getText().toString().trim();

        // Providing context as well for certification update
        new RegisterTask(registrationCompleter).execute(enteredNN, gcmRegistrationId);
    }

    /**
     * This object is called when server registration is finished.
     */
    private OnTaskComplete<RegistrationResult> registrationCompleter = new OnTaskComplete<RegistrationResult>() {
        @Override
        public void onTaskComplete(RegistrationResult result) {
            if (result == null) {
                result = new RegistrationResult();
                result.setResultStatus(RegistrationResultStatus.INTERNAL_ERROR);
            }

            AlertDialog.Builder builder = null;
            AlertDialog dialog = null;
            switch(result.getResultStatus()) {
                case INTERNAL_ERROR:
                    builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom));
                    builder.setMessage(R.string.register_dialog_error_msg)
                            .setTitle(R.string.register_dialog_error_title);
                    builder.setPositiveButton(getString(R.string.register_dialog_error_btn),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    dialog = builder.create();
                    dialog.show();
                    break;
                case NICK_TAKEN:
                    builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom));
                    builder.setMessage(String.format(getString(R.string.register_dialog_taken_msg),result.getNickname()))
                            .setTitle(R.string.register_dialog_taken_title);
                    builder.setPositiveButton(getString(R.string.register_dialog_taken_btn),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    dialog = builder.create();
                    dialog.show();
                    break;
                case SUCCESS:
                    //Saving registration data
                    SharedPreferences prefs = getSharedPreferences(configuration.getSharedPreferencesId(), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(configuration.getSpUserNameKey(),result.getNickname());
                    editor.putString(configuration.getSpUserIdKey(),result.getId());
                    editor.commit();
                    Log.d(TAG,"Registered with id "+result.getId());

                    //Popup
                    Toast toast  = Toast.makeText(getApplicationContext(), getString(R.string.register_dialog_success_msg), Toast.LENGTH_SHORT);
                    toast.show();

                    //Going to Quest list
                    Intent i = new Intent(RegisterActivity.this, QuestListActivity.class);
                    i.putExtra("fromRegister",true);
                    RegisterActivity.this.startActivity(i);

                    finish();
                    break;
                default:
                    //Not used
                    break;
            }
        }
    };
}