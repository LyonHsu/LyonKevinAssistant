package lyon.kevin.googlethings.assistant.Tool;

import android.Manifest;
import android.app.Activity;
import android.content.Context;


import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;


/**
 * Created by Ellis on 2016/9/21.
 */

public class Permission {


    public boolean checBluetoothPermission(Context context, String[] permissionModule) {
        boolean checkStatus = false;
        String[] str_permissionModule = permissionModule;
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permission == PERMISSION_GRANTED){
            checkStatus = true;
        }else{
            ActivityCompat.requestPermissions((Activity) context, permissionModule, MainConstant.ACCESS_FINE_LOCATION);
        }
        return checkStatus;
    }

    public static boolean checBROADCASTPermission(Context context, String permissionModule) {
        boolean checkStatus = false;
        String[] str_permissionModule = {permissionModule};
        int permission = ActivityCompat.checkSelfPermission(context, permissionModule);
        if (permission == PERMISSION_GRANTED){
            checkStatus = true;
        }else{
            ActivityCompat.requestPermissions((Activity) context, str_permissionModule, MainConstant.ACCESS_FINE_LOCATION);
        }
        return checkStatus;
    }

    public static boolean checAudioRecordPermission(Context context) {
        boolean checkStatus = false;
        String RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
        String[] str_permissionModule = {RECORD_AUDIO};
        int permission = ActivityCompat.checkSelfPermission(context, RECORD_AUDIO);
        if (permission == PERMISSION_GRANTED){
            checkStatus = true;
        }else{
            ActivityCompat.requestPermissions((Activity) context, str_permissionModule, MainConstant.ACCESS_FINE_LOCATION);
        }
        return checkStatus;
    }



}
