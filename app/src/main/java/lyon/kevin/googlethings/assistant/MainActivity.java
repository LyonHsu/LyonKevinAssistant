package lyon.kevin.googlethings.assistant;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lyon.kevin.googlethings.assistant.BlueTooth.BluetoothTool;
import lyon.kevin.googlethings.assistant.NetWork.NetWork;
import lyon.kevin.googlethings.assistant.TextToSpeech.LyonTextToSpeech;
import lyon.kevin.googlethings.assistant.Tool.Alert;
import lyon.kevin.googlethings.assistant.Tool.Log;
import lyon.kevin.googlethings.assistant.Tool.MainConstant;
import lyon.kevin.googlethings.assistant.Tool.Permission;
import lyon.kevin.googlethings.assistant.Tool.ToastUtile;
import lyon.kevin.googlethings.assistant.Tool.Utils;

public class MainActivity extends AssistantActivity {
    String TAG = MainActivity.class.getSimpleName();
    Context context;
    //Is Use Google AIY Device
    public static boolean isGoogleAIY = true;
    NetWork netWork;


    BluetoothTool bluetoothTool;
    final int OPENBLUETOOTH = 0;
    final int REQUEST_ENABLE_BT = 100;
    private static final int REQUEST_CODE = 2; // 请求码
    public static int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private android.widget.Button blueToothBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppController.getInstance().setAssistantActivity(this);
        context = this;
        Permission permission = new Permission();
        if(!permission.checAudioRecordPermission(context)){
            Alert.showAlert(this, getString(R.string.AudioRecordtitle), getString(R.string.AudioRecordPermission), "ok");
        }else {
            setContentView(R.layout.activity_main);
            BlueToothInit();
            
            setVolume();
            
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
            NetWorkInit();

            super.onCreate(savedInstanceState);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult requestCode:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == REQUEST_ENABLE_BT) {
            Log.d(TAG, "BluetoothTool Enable requestCode:" + requestCode);
        }
        // 拒绝时, 关闭页面, 缺少主要权限, 无法运行
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                // SYSTEM_ALERT_WINDOW permission not granted...
                Toast.makeText(this, "Permission Denieddd by user.Please Check it in Settings", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        bluetoothTool.onDestroy();
    }
    
    private void setVolume(){
        //設定音量
        try {
            int systemName = AudioManager.STREAM_SYSTEM;
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

            /**
             *MODE_NORMAL : 普通模式，既不是鈴聲模式也不是通話模式
             * MODE_RINGTONE : 鈴聲模式
             * MODE_IN_CALL : 通話模式
             * MODE_IN_COMMUNICATION : 通訊模式，包括音/視訊,VoIP通話.(3.0加入的，與通話模式類似)
             */
//            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
            int maVolume = audioManager.getStreamMaxVolume(systemName) / 3;
            audioManager.setStreamVolume(systemName, maVolume, AudioManager.FLAG_SHOW_UI);
            systemName = AudioManager.STREAM_MUSIC;//STREAM_RING
            maVolume = audioManager.getStreamMaxVolume(systemName);
            audioManager.setStreamVolume(systemName, maVolume, AudioManager.FLAG_VIBRATE);
            systemName = AudioManager.STREAM_RING;//STREAM_RING
            maVolume = audioManager.getStreamMaxVolume(systemName);
            audioManager.setStreamVolume(systemName, maVolume, AudioManager.FLAG_VIBRATE);
            ToastUtile.showText(context, "設定音量為：" + maVolume);
            Log.e(TAG, "設定音量為：" + maVolume);
        }catch (Exception e){
            Log.e(TAG, Utils.FormatStackTrace(e));
        }
    }

    private void NetWorkInit(){
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
    }

    public void BlueToothInit(){
        bluetoothTool = new BluetoothTool(this) {
            @Override
            public void getBluetoothDeviceName(HashMap<String, String> bluetoothDeviceName, BluetoothDevice device) {

                int i = 0;
                for (Map.Entry<String, String> entry : bluetoothDeviceName.entrySet()) {
                    Log.d(TAG,""+"[" + i + "]:" + entry.getValue() + ", " + entry.getKey());
                    i++;
                }
            }
            @Override
            public void openBluetoothTime(int time) {
                android.util.Log.e(TAG, "openBluetoothTime:" + time);
                Message message = new Message();
                message.obj = time;
                message.what = OPENBLUETOOTH;
                handler.sendMessage(message);

            }

            @Override
            public void startBT(Intent intent) {
                android.util.Log.d(TAG, "startBT intent:" + intent.getAction());
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }

            @Override
            public void reSearchOldBluetoothdevice() {
                super.reSearchOldBluetoothdevice();
            }
        };
        String bluetoothType = bluetoothTool.getBlueToothType(bluetoothTool.getBluetoothClass());
        String blueDate = "bluetooth Name:" + bluetoothTool.getBluetoothName(MainConstant.BlueToothName+" Pi3_" + Build.MODEL) + ",   Mac:" + bluetoothTool.getBluetoothMac();
        AppController.getInstance().setBluetoothTool(bluetoothTool);
        android.util.Log.d(TAG,"bluetooth:"+blueDate+" type:"+bluetoothType);
        bluetoothTool.findBuletoothDevice();
        bluetoothTool.openBlueTooth();

        blueToothBtn = findViewById(R.id.blueToothBtn);
        blueToothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bluetoothTool!=null) {
                    bluetoothTool.openBlueTooth();
                }
            }
        });


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
        if(bluetoothTool!=null){
            bluetoothTool.bluetoothWrite(jsonObject);
        }
    }

}
