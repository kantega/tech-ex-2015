package no.kantega.techex.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import no.kantega.techex.android.R;

/**
 * Dialog Activity for asking the user to turn on the bluetooth
 */
public class BluetoothDialogActivity extends Activity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setMessage(getString(R.string.bluetooth_alert_msg))
                .setTitle(R.string.bluetooth_alert_title);
        builder.setPositiveButton(getString(R.string.bluetooth_alert_allow),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        adapter.enable();
                        dialog.cancel();
                        finish();
                    }
                });
        builder.setNegativeButton(getString(R.string.bluetooth_alert_deny),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        AlertDialog dialog =builder.create();
        dialog.show();
    }
}
