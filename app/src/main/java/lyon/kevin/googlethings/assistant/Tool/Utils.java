package lyon.kevin.googlethings.assistant.Tool;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.UI_MODE_SERVICE;

public class Utils {

    static String TAG = Utils.class.getName();



    public static int dpToPx(Context context, int dp){
        if(context == null) {
            return MainConstant.NO_DATA;
        }

        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static String FormatStackTrace(Throwable throwable) {
        if(throwable==null) return "";
        String rtn = throwable.getStackTrace().toString();
        try {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            throwable.printStackTrace(printWriter);
            printWriter.flush();
            writer.flush();
            rtn = writer.toString();
            printWriter.close();
            writer.close();
        } catch (IOException e) {
            System.out.println(TAG + ": an error FormatStackTrace..." + Utils.FormatStackTrace(e));
        } catch (Exception ex) {
            System.out.println(TAG + ": an error FormatStackTrace..." + Utils.FormatStackTrace(ex));
        }
        return rtn;
    }


    public static String generateTime(long time) {
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }

    static String T =  "true";
    static String F =  "false";
    public static String boolenToString(boolean b){
        if(b){
            return T;
        }else{
            return F;
        }
    }

    public static boolean StringToBoolen(String s){
        if(T.equals(s)){
            return true;
        }else{
            return false;
        }
    }

    public static boolean checkPiDevice(Context mContext){
        boolean isPiDevice = false;
        UiModeManager uiModeManager = (UiModeManager) mContext.getSystemService(UI_MODE_SERVICE);
        Log.d(TAG, "checkTVDevice Running on a Pi Device type:"+uiModeManager.getCurrentModeType());
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_APPLIANCE) {
            Log.d(TAG, "checkTVDevice Running on a Pi Device");
            isPiDevice = true;
        } else {
            Log.d(TAG, "checkTVDevice Running on a non-Pi Device");
        }
        return isPiDevice;
    }
}
