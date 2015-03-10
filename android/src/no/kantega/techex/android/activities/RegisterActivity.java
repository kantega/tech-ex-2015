package no.kantega.techex.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import no.kantega.techex.android.R;
import no.kantega.techex.android.rest.OnTaskComplete;
import no.kantega.techex.android.rest.RegisterTask;
import no.kantega.techex.android.rest.wrapper.RegistrationResult;
import no.kantega.techex.android.tools.Configuration;

/**
 * Registering new user
 *
 * //TODO regex restriction on username?
 * //TODO default text in inputtext
 */
public class RegisterActivity extends Activity implements OnTaskComplete<RegistrationResult> {
    private final String TAG = RegisterActivity.class.getSimpleName();

    private Configuration configuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        configuration = Configuration.getInstance();

        Button b = (Button)findViewById(R.id.btnRegister);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Text field
                EditText et = (EditText)findViewById(R.id.nickname);
                String enteredNN = et.getText().toString().trim();

                Activity myActivity=(Activity)(view.getContext()); // all views have a reference to their context
                SharedPreferences prefs = myActivity.getSharedPreferences(configuration.getSharedPreferencesId(), Context.MODE_PRIVATE);
                String gcmId = prefs.getString(configuration.getSpGcmAuthKey(),null);

                String address = configuration.getRegistrationREST();
                new RegisterTask((RegisterActivity)myActivity).execute(enteredNN,address,gcmId);
            }
        });
    }

    @Override
    public void onTaskComplete(RegistrationResult result) {
        Context context = getApplicationContext();
        AlertDialog.Builder builder = null;
        AlertDialog dialog = null;
        switch(result.getResultStatus()) {
            case INTERNAL_ERROR:
                builder = new AlertDialog.Builder(this);
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
                 builder = new AlertDialog.Builder(this);
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
                //save preferences if needed
                //save quest list if needed
                editor.commit();
                Log.d(TAG,"Registered with id "+result.getId());

                //Popup
                Toast toast  = Toast.makeText(context, getString(R.string.register_dialog_success_msg), Toast.LENGTH_LONG);
                toast.show();

                //Going to Quest list (or welcome?)
                Intent i = new Intent(RegisterActivity.this, QuestListActivity.class);
                RegisterActivity.this.startActivity(i);

                finish();
                break;
            default:
                //Not used
                break;
        }
    }
}