package lyon.kevin.googlethings.assistant;

import android.app.Application;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import lyon.kevin.googlethings.assistant.Tool.Log;

public class AppController extends Application {

    static String TAG = AppController.class.getName();
    private static AppController appController;
    private TextToSpeech mTtsEngine;
    private static final String UTTERANCE_ID =
            "com.example.androidthings.bluetooth.audio.UTTERANCE_ID";

    AssistantActivity assistantActivity;

    @Override
    public void onCreate() {
        android.util.Log.d(TAG, "onCreate");
        super.onCreate();
        appController = this;
        initTts();
    }

    public static synchronized AppController getInstance() {
        return appController;
    }

    private void initTts() {
        mTtsEngine = new TextToSpeech(AppController.this,
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            mTtsEngine.setLanguage(Locale.US);
                        } else {
                            Log.w(TAG, "Could not open TTS Engine (onInit status=" + status
                                    + "). Ignoring text to speech");
                            mTtsEngine = null;
                        }
                    }
                });
    }
    public void speak(Context context,String utterance) {
        Log.i(TAG, utterance);
        if (mTtsEngine != null) {
            mTtsEngine.speak(utterance, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);

        }
    }

    public void setAssistantActivity(AssistantActivity assistantActivity){
        this.assistantActivity=assistantActivity;
    }

    public AssistantActivity getAssistantActivity( ){
        return assistantActivity;
    }




}
