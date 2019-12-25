package lyon.kevin.googlethings.assistant;

import android.content.Context;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;

import lyon.kevin.googlethings.assistant.NetWork.NetWork;
import lyon.kevin.googlethings.assistant.TextToSpeech.LyonTextToSpeech;
import lyon.kevin.googlethings.assistant.Tool.Alert;
import lyon.kevin.googlethings.assistant.Tool.Log;
import lyon.kevin.googlethings.assistant.Tool.MainConstant;
import lyon.kevin.googlethings.assistant.Tool.Permission;
import lyon.kevin.googlethings.assistant.Tool.ToastUtile;

public class MainActivity extends AssistantActivity {
    String TAG = MainActivity.class.getSimpleName();
    Context context;
    //Is Use Google AIY Device
    public static boolean isGoogleAIY = true;
    NetWork netWork;
    final int OPENBLUETOOTH = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppController.getInstance().setAssistantActivity(this);
        context = this;
        Permission permission = new Permission();
        if(!permission.checAudioRecordPermission(context)){
            Alert.showAlert(this, getString(R.string.AudioRecordtitle), getString(R.string.AudioRecordPermission), "ok");
        }else {
            setContentView(R.layout.activity_main);
            setTurnScreenOn(true);
            setTitle(getString(R.string.app_name)+" Hot key:"+ MainConstant.ACTIVATION_KEYPHRASE);
            textToSpeech= new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    Log.d(TAG, "getTextToSpeech TTS init status:" + status);
                    if (status != TextToSpeech.ERROR) {
//                        int result = textToSpeech.setLanguage(Locale.getDefault());//Locale.);
                        textToSpeech.setPitch(1.0f); // 音調
                        textToSpeech.setSpeechRate(1.0f); // 速度
                        int result = textToSpeech.setLanguage(Locale.getDefault());
                        HashMap myHash = new HashMap<String, String>();
                        myHash.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                                String.valueOf(AudioManager.MODE_NORMAL));
//                        textToSpeech.speak(openComplete, TextToSpeech.QUEUE_FLUSH, myHash);
                        Log.d(TAG, "getTextToSpeech speak result init:" + result);


                    }else{
                        Log.e(TAG, "getTextToSpeech TTS init Error:" + status);
                        ToastUtile.showText(context,"getTextToSpeech TTS init Error:" + status);
                    }
                }
            });
            netWork = (NetWork) findViewById(R.id.network);
            netWork.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    netWork.superOnClick();
                    LyonTextToSpeech.speak(context,textToSpeech,netWork.getLocalIpAddress(context));

                }
            });
            netWork.setOnWifiStatusListener(new NetWork.OnWifiStatusListener() {
                @Override
                public void wifiStatue(NetworkInfo.DetailedState status) {
                    Log.i(TAG, " onReceive: intent action wifiStatue:" + status);
                    if(status.equals(NetworkInfo.DetailedState.CONNECTED)) {
                        String ipp =  "no connect wifi!";
                        String ip =ipp;
                        WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInf = wifiMan.getConnectionInfo();
                        int ipAddress = wifiInf.getIpAddress();
                        ip= String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
                        String IP=""+ip;
                        String ssID = wifiInf.getSSID();
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("IP", IP);
                            jsonObject.put("SSID", ssID);
                            blueToothWrite(jsonObject);
                        }catch (JSONException e){
                            Log.e(TAG,"");
                        }
                    }else if(status.equals(NetworkInfo.DetailedState.SCANNING) ||
                            status.equals(NetworkInfo.DetailedState.DISCONNECTING) ||
                            status.equals(NetworkInfo.DetailedState.FAILED) ||
                            status.equals(NetworkInfo.DetailedState.BLOCKED) ||
                            status.equals(NetworkInfo.DetailedState.DISCONNECTED)
                    ){
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("IP", "DISCONNECTED");
                            blueToothWrite(jsonObject);
                        }catch (JSONException e){
                            Log.e(TAG,"");
                        }
                    }else if(status.equals(NetworkInfo.DetailedState.CONNECTING) ||
                            status.equals(NetworkInfo.DetailedState.AUTHENTICATING) ||
                            status.equals(NetworkInfo.DetailedState.OBTAINING_IPADDR)
                    ){
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("IP", "CONNECTING");
                            blueToothWrite(jsonObject);
                        }catch (JSONException e){
                            Log.e(TAG,"");
                        }
                    }else if(status.equals(NetworkInfo.DetailedState.SUSPENDED)){
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("IP", "SUSPENDED");
                            blueToothWrite(jsonObject);
                        }catch (JSONException e){
                            Log.e(TAG,"");
                        }
                    }
                }
            });

            super.onCreate(savedInstanceState);
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case OPENBLUETOOTH:
                    Log.d(TAG, "handler openBluetoothTime:" + message.obj + "s");
                    break;
            }
        }
    };

    public void blueToothWrite(JSONObject jsonObject){

    }
}
