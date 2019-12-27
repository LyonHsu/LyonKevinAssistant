package lyon.kevin.googlethings.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;

import com.google.android.things.pio.Gpio;

import java.io.IOException;

import lyon.kevin.googlethings.assistant.Sphinx.CapTechSphinxManager;
import lyon.kevin.googlethings.assistant.TextToSpeech.LyonTextToSpeech;
import lyon.kevin.googlethings.assistant.Tool.Log;
import lyon.kevin.googlethings.assistant.Tool.MainConstant;
import lyon.kevin.googlethings.assistant.Tool.ToastUtile;
import lyon.kevin.googlethings.assistant.Tool.Utils;

public class SphinxActivity extends Activity implements CapTechSphinxManager.SphinxListener {
    String TAG = SphinxActivity.class.getSimpleName();
    Context context;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected ProgressDialog progressDialog;
    protected EmbeddedAssistant mEmbeddedAssistant;
    protected Gpio mLed;
    //Sphinx
    //pocket sphinx for hot key
    protected CapTechSphinxManager captechSphinxManager;
    protected boolean LEDShining = false;
    protected TextToSpeech textToSpeech;
    String openComplete = "開機完畢 你可以使用 " + MainConstant.ACTIVATION_KEYPHRASE + " 來喚醒";
    String AISay="Yes";
    public final int NOTIFYCHANGE=2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        try {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("init");
            progressDialog.setMessage("beging....");
            progressDialog.show();
        }catch (Exception e){
            Log.e(TAG, Utils.FormatStackTrace(e));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //let's clean up.
        if(captechSphinxManager!=null)
            captechSphinxManager.destroy();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech=null;
        }
    }

    //===========Sphinx 喚醒詞=======================================================================
    @Override
    public void onInitializationComplete() {
        Log.d(TAG, "Speech Recognition Ready");
        //讓我們的Sphinx Manager知道我們想要聽短語
        captechSphinxManager.startListeningToActivationPhrase();
        //lets show a blue light to indicate we are ready.
//        playDing(this);
        LEDShining=false;
        mHandler.removeCallbacks(runnable);
        if(textToSpeech!=null) {
            LyonTextToSpeech.speak(context, textToSpeech, openComplete);
            Log.e(TAG,openComplete);
            ToastUtile.showText(this, openComplete);
        }
        progressDialog.dismiss();
    }

    @Override
    public void onActivationPhraseDetected() {
        // TODO開始我們的助理請求
        Log.d(TAG, "Activation Phrase Detected :"+AISay);
//        LyonTextToSpeech.speak(context,textToSpeech,AISay);

        ToastUtile.showText(this,"是的");
        mEmbeddedAssistant.setIsSpecialRequest(false);
        if(mEmbeddedAssistant!=null)
            mEmbeddedAssistant.startConversation();
        if (mLed != null) {
            try {
                mLed.setValue(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case NOTIFYCHANGE:
                    Log.e(TAG,"Youtube NOTIFYCHANGE");
                    break;
            }
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            LEDShining = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(SphinxActivity.this);
            builder.setTitle("Error");
            builder.setMessage(R.string.system_error)
                    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            dialog.dismiss();
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    };

}
