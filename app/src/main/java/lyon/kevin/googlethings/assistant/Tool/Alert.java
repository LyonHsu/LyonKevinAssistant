package lyon.kevin.googlethings.assistant.Tool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;

import lyon.kevin.googlethings.assistant.R;

/**
 * Created by Gordon on 2016/12/31.
 */

public class Alert {
    public static void showAlert(Context context, CharSequence title, CharSequence message, CharSequence btnTitle) {
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(context);;
        //dlgBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        dlgBuilder.setTitle(title);
        dlgBuilder.setMessage(message);
        dlgBuilder.setCancelable(false);
        dlgBuilder.setPositiveButton(context.getResources().getString(R.string.chance), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(true);
            }
        });
        dlgBuilder.setNeutralButton(context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(true);
            }
        });
        dlgBuilder.show();
    }
}
