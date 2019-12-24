package lyon.kevin.googlethings.assistant;

import android.content.Context;
import android.os.Bundle;

import lyon.kevin.googlethings.assistant.Tool.Alert;
import lyon.kevin.googlethings.assistant.Tool.Permission;

public class MainActivity extends AssistantActivity {
    String TAG = MainActivity.class.getSimpleName();
    Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppController.getInstance().setAssistantActivity(this);
        context = this;
        Permission permission = new Permission();
        if(!permission.checAudioRecordPermission(context)){
            Alert.showAlert(this, getString(R.string.wifititle), getString(R.string.wifioffmassage), "ok");
        }else {
            setContentView(R.layout.activity_main);
            super.onCreate(savedInstanceState);
        }
    }
}
