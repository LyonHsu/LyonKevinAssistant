/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lyon.kevin.googlethings.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import lyon.kevin.googlethings.assistant.EmbeddedAssistant.ConversationCallback;
import lyon.kevin.googlethings.assistant.EmbeddedAssistant.RequestCallback;
import lyon.kevin.googlethings.assistant.Sphinx.CapTechSphinxManager;
import lyon.kevin.googlethings.assistant.TextToSpeech.LyonTextToSpeech;
import lyon.kevin.googlethings.assistant.Tool.Log;
import lyon.kevin.googlethings.assistant.Tool.Utils;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.voicehat.Max98357A;
import com.google.android.things.contrib.driver.voicehat.VoiceHat;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.assistant.embedded.v1alpha2.SpeechRecognitionResult;
import com.google.auth.oauth2.UserCredentials;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class AssistantActivity extends SphinxActivity implements Button.OnButtonEventListener  {
    private static final String TAG = AssistantActivity.class.getSimpleName();

    // Peripheral and drivers constants.
    private static final int BUTTON_DEBOUNCE_DELAY_MS = 20;
    // Default on using the Voice Hat on Raspberry Pi 3.
    public static final boolean USE_VOICEHAT_I2S_DAC = Build.DEVICE.equals(BoardDefaults.DEVICE_RPI3);

    // Audio constants.
    private static final String PREF_CURRENT_VOLUME = "current_volume";
    private static final int SAMPLE_RATE = 16000;
    private static final int DEFAULT_VOLUME = 100;

    // Assistant SDK constants.
    private static final String DEVICE_MODEL_ID = "PLACEHOLDER";
    private static final String DEVICE_INSTANCE_ID = "PLACEHOLDER";
    private static final String LANGUAGE_CODE = "en-US";

    // Hardware peripherals.
    private Button mButton;
    private android.widget.Button mButtonWidget;
//    private Gpio mLed;
    private Max98357A mDac;

    private Handler mMainHandler;

    // List & adapter to store and display the history of Assistant Requests.
//    private EmbeddedAssistant mEmbeddedAssistant;
    private ArrayList<String> mAssistantRequests = new ArrayList<>();
    private ArrayAdapter<String> mAssistantRequestsAdapter;
    private CheckBox mHtmlOutputCheckbox;
    private WebView mWebView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "starting assistant demo");



        final ListView assistantRequestsListView = findViewById(R.id.assistantRequestsListView);
        mAssistantRequestsAdapter =
            new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                mAssistantRequests);
        assistantRequestsListView.setAdapter(mAssistantRequestsAdapter);
        mHtmlOutputCheckbox = findViewById(R.id.htmlOutput);
        mHtmlOutputCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean useHtml) {
                mWebView.setVisibility(useHtml ? View.VISIBLE : View.GONE);
                assistantRequestsListView.setVisibility(useHtml ? View.GONE : View.VISIBLE);
                mEmbeddedAssistant.setResponseFormat(useHtml
                        ? EmbeddedAssistant.HTML : EmbeddedAssistant.TEXT);
            }
        });
        mWebView = findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);

        mMainHandler = new Handler(getMainLooper());
        mButtonWidget = findViewById(R.id.assistantQueryButton);
        mButtonWidget.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sphinx
                if (mEmbeddedAssistant != null) {
                    captechSphinxManager.SpeechRecognizerStop();
                    mEmbeddedAssistant.startConversation();
                }
            }
        });


        // Audio routing configuration: use default routing.
        AudioDeviceInfo audioInputDevice = null;
        AudioDeviceInfo audioOutputDevice = null;
        if (USE_VOICEHAT_I2S_DAC) {
            if (MainActivity.isGoogleAIY) {
                audioInputDevice = findAudioDevice(AudioManager.GET_DEVICES_INPUTS, AudioDeviceInfo.TYPE_BUS);//TYPE_USB_DEVICE ,TYPE_BUS
                if (audioInputDevice == null) {
                    Log.e(TAG, "failed to find I2S audio input device, using default");
                } else {
                    Log.d(TAG, " find USB audio input device, using I2S");
                }
                audioOutputDevice = findAudioDevice(AudioManager.GET_DEVICES_OUTPUTS, AudioDeviceInfo.TYPE_BUS);
                if (audioOutputDevice == null) {
                    Log.e(TAG, "failed to found I2S audio output device, using Unknow");
                } else {
                    Log.d(TAG, " find USB audio input device, using I2S");
                }
            } else {
                Log.e(TAG, " find USB audio input device, using default");
                audioInputDevice = findAudioDevice(AudioManager.GET_DEVICES_INPUTS, AudioDeviceInfo.TYPE_USB_DEVICE);
                if (audioInputDevice == null) {
                    Log.e(TAG, "failed to find I2S audio input device, using Unknow");
                } else {
                    Log.d(TAG, " find USB audio input device, using USB");
                }
                audioOutputDevice = findAudioDevice(AudioManager.GET_DEVICES_OUTPUTS, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
                if (audioOutputDevice == null) {
                    Log.e(TAG, "failed to found 3.5mm audio output device, using Unknow");
                } else {
                    Log.d(TAG, " find 3.5mm audio input device, using 3.5mm");
                }
            }
        }

        try {
            if (USE_VOICEHAT_I2S_DAC) {
                Utils.checkPiDevice(this);
                Log.i(TAG, "initializing DAC trigger");
                mDac = VoiceHat.openDac();
                mDac.setSdMode(Max98357A.SD_MODE_SHUTDOWN);

                mButton = VoiceHat.openButton();
                mLed = VoiceHat.openLed();
                setmLed();
            } else if(Utils.checkPiDevice(this)){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                PeripheralManager pioManager = PeripheralManager.getInstance();
                                mButton = new Button(BoardDefaults.getGPIOForButton(),
                                        Button.LogicState.PRESSED_WHEN_LOW);
                                mLed = pioManager.openGpio(BoardDefaults.getGPIOForLED());
                                setmLed();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
            }


        } catch (Exception e) {
            Log.e(TAG, "error configuring peripherals:", e);
            return;
        }

        // Set volume from preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int initVolume = preferences.getInt(PREF_CURRENT_VOLUME, DEFAULT_VOLUME);
        Log.i(TAG, "setting audio track volume to: " + initVolume);

        UserCredentials userCredentials = null;
        try {
            userCredentials =
                    EmbeddedAssistant.generateCredentials(this, R.raw.credentials);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "error getting user credentials", e);
        }
        mEmbeddedAssistant = new EmbeddedAssistant.Builder()
                .setCredentials(userCredentials)
                .setDeviceInstanceId(DEVICE_INSTANCE_ID)
                .setDeviceModelId(DEVICE_MODEL_ID)
                .setLanguageCode(LANGUAGE_CODE)
                .setAudioInputDevice(audioInputDevice)
                .setAudioOutputDevice(audioOutputDevice)
                .setAudioSampleRate(SAMPLE_RATE)
                .setAudioVolume(initVolume)
                .setRequestCallback(new RequestCallback() {
                    @Override
                    public void onRequestStart() {
                        Log.i(TAG, "20191227 starting assistant request, enable microphones");
                        mButtonWidget.setText(R.string.button_listening);
                        mButtonWidget.setEnabled(false);
                    }

                    @Override
                    public void onSpeechRecognition(List<SpeechRecognitionResult> results) {
                        for (final SpeechRecognitionResult result : results) {
                            Log.i(TAG, "20191227 assistant request text: " + result.getTranscript() +
                                " stability: " + Float.toString(result.getStability()));
                            mAssistantRequests.add(result.getTranscript()+" stability: " + Float.toString(result.getStability()));
                        }
                        mAssistantRequestsAdapter.notifyDataSetChanged();
                    }
                })
                .setConversationCallback(new ConversationCallback() {
                    @Override
                    public void onResponseStarted() {
                        super.onResponseStarted();
                        // When bus type is switched, the AudioManager needs to reset the stream volume
                        if (mDac != null) {
                            try {
                                mDac.setSdMode(Max98357A.SD_MODE_LEFT);
                            } catch (IOException e) {
                                Log.e(TAG, "error enabling DAC", e);
                            }
                        }
                    }

                    @Override
                    public void onResponseFinished() {
                        super.onResponseFinished();
                        if (mDac != null) {
                            try {
                                mDac.setSdMode(Max98357A.SD_MODE_SHUTDOWN);
                            } catch (IOException e) {
                                Log.e(TAG, "error disabling DAC", e);
                            }
                        }
                        if (mLed != null) {
                            try {
                                mLed.setValue(false);
                            } catch (IOException e) {
                                Log.e(TAG, "cannot turn off LED", e);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, "assist error: " + throwable.getMessage()+ throwable);
                    }

                    @Override
                    public void onVolumeChanged(int percentage) {
                        Log.i(TAG, "assistant volume changed: " + percentage);
                        // Update our shared preferences
                        Editor editor = PreferenceManager
                                .getDefaultSharedPreferences(AssistantActivity.this)
                                .edit();
                        editor.putInt(PREF_CURRENT_VOLUME, percentage);
                        editor.apply();
                    }

                    @Override
                    public void onConversationFinished() {
                        Log.i(TAG, "sphinx assistant conversation finished");
                        mButtonWidget.setText(R.string.button_new_request);
                        mButtonWidget.setEnabled(true);

                        //the user is done making their request. stop passing data and clean up
                        Log.d(TAG, "sphinx the assistant request finish.");
                        mEmbeddedAssistant.mAudioRecord.stop();
                        //okay we can activate via keyphrase again
                        captechSphinxManager.startListeningToActivationPhrase();

                    }

                    @Override
                    public void onAssistantResponse(final String response) {
                        if(!response.isEmpty()) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mAssistantRequests.add("Google Assistant: " + response);
                                    Log.i(TAG, "20191227 Google Assistant:: " + response);
                                    mAssistantRequestsAdapter.notifyDataSetChanged();
                                }
                            });
                        }

                    }

                    @Override
                    public void onAssistantDisplayOut(final String html) {
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // Need to convert to base64
                                try {
                                    final byte[] data = html.getBytes("UTF-8");
                                    final String base64String =
                                        Base64.encodeToString(data, Base64.DEFAULT);
                                    mWebView.loadData(base64String, "text/html; charset=utf-8",
                                        "base64");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    public void onDeviceAction(String intentName, JSONObject parameters) {
                        if (parameters != null) {
                            Log.d(TAG, "Get device action " + intentName + " with parameters: " +
                                parameters.toString());
                        } else {
                            Log.d(TAG, "Get device action " + intentName + " with no paramete"
                                + "rs");
                        }
                        if (intentName.equals("action.devices.commands.OnOff")) {
                            try {
                                boolean turnOn = parameters.getBoolean("on");
                                mLed.setValue(turnOn);
                            } catch (JSONException e) {
                                Log.e(TAG, "Cannot get value of command", e);
                            } catch (IOException e) {
                                Log.e(TAG, "Cannot set value of LED", e);
                            }
                        }
                    }
                })
                .build();
        mEmbeddedAssistant.connect();



        //instantiate PSphinx
        progressDialog.setMessage("Embedded Sphinx....");
        captechSphinxManager = new CapTechSphinxManager(this, this);
        LEDShining = true;

        // TODO打開一盞燈！
        LEDShining();
        mHandler.postDelayed(runnable,60*1000);
    }

    private void setmLed(){
        try {
            if(mButton!=null) {
                mButton.setDebounceDelay(BUTTON_DEBOUNCE_DELAY_MS);
                mButton.setOnButtonEventListener(this);
            }
            if(mLed!=null) {
                mLed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                mLed.setActiveType(Gpio.ACTIVE_HIGH);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AudioDeviceInfo findAudioDevice(int deviceFlag, int deviceType) {
        AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adis = manager.getDevices(deviceFlag);
        for (AudioDeviceInfo adi : adis) {
            if (adi.getType() == deviceType) {
                return adi;
            }
        }
        return null;
    }

    @Override
    public void onButtonEvent(Button button, boolean pressed) {
        try {
            if (mLed != null) {
                mLed.setValue(pressed);
            }
        } catch (IOException e) {
            Log.d(TAG, "error toggling LED:"+ e);
        }
        if (pressed) {
            LyonTextToSpeech.speak(context,textToSpeech,AISay);
            captechSphinxManager.SpeechRecognizerStop();
            mEmbeddedAssistant.startConversation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroying assistant demo");
        if (mLed != null) {
            try {
                mLed.close();
            } catch (IOException e) {
                Log.w(TAG, "error closing LED"+ e);
            }
            mLed = null;
        }
        if (mButton != null) {
            try {
                mButton.close();
            } catch (IOException e) {
                Log.w(TAG, "error closing button"+ e);
            }
            mButton = null;
        }
        if (mDac != null) {
            try {
                mDac.close();
            } catch (IOException e) {
                Log.w(TAG, "error closing voice hat trigger"+ e);
            }
            mDac = null;
        }
        mEmbeddedAssistant.destroy();
    }



    private void LEDShining(){
        Log.e(TAG, "mLed LEDShining");
        if(mLed!=null){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int i=0;
                    while (LEDShining){
                        try {
                            mLed.setValue(true);
                            Log.e(TAG, "mLed == on");
                            Thread.sleep(250);
                            mLed.setValue(false);
                            Log.e(TAG, "mLed == off");
                            Thread.sleep(200);
                        } catch (IOException e) {
                            Log.e(TAG,"Led IOException:"+e);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        i++;
                    }
                }
            }).start();
        }else{
            Log.e(TAG, "mLed == null");
        }
    }
}
