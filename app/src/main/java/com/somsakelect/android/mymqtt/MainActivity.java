package com.somsakelect.android.mymqtt;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.somsakelect.android.mqtt.MqttAndroidClient;
import com.somsakelect.android.mymqtt.helper.BetterActivityResult;

import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private MqttAndroidClient mqtt;

    public static final String MQTT_HOST = "broker.hivemq.com";
    public static final int MQTT_PORT = 1883;
    public static final String MQTT_USERNAME = "";
    public static final String MQTT_PASSWORD = "";
    public static final String MQTT_URL = "tcp://"+MQTT_HOST+":"+MQTT_PORT;
    public static final String MQTT_ID = MqttClient.generateClientId();

    private TextView subTv, stTv;
    private EditText eSubTp, ePubTp, ePubMsg;
    private static final String TAG = "MainActivity";
    private static final int SCHEDULE_EXACT_ALARM_CODE = 1433;
    protected final BetterActivityResult<Intent, ActivityResult> activityLauncher = BetterActivityResult.registerActivityForResult(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subTv = findViewById(R.id.sub_tv);
        stTv = findViewById(R.id.st_tv);
        eSubTp = findViewById(R.id.sub_e);
        ePubTp = findViewById(R.id.pub_tp_e);
        ePubMsg = findViewById(R.id.pub_pay_e);

        //MQTT
        mqtt = new MqttAndroidClient(this, MQTT_URL, MQTT_ID);
        mqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w(TAG, "MQTT reconnect..."+reconnect);
                stTv.setText(reconnect? "Reconnecting...":"Connected");
            }

            @Override
            public void connectionLost(Throwable cause) {
                if (cause!=null){
                    Log.e(TAG, "MQTT lost..."+cause.getMessage());
                    String st = "MQTT lost! "+cause.getMessage();
                    stTv.setText(st);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String mess = message.toString();
                String log = String.format("MQTT RX [%s]: %s", topic, mess);
                Log.w(TAG, log);
                //Debug
                subTv.setText(mess);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.w(TAG, "Publish success...");
                showToast("Publish success");
            }
        });

        //Connect
        findViewById(R.id.con_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectMQTT();
            }
        });

        //Subscribe
        findViewById(R.id.sub_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subscribe(getEnter(eSubTp));
            }
        });

        //Publish
        findViewById(R.id.pub_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publish(getEnter(ePubTp), getEnter(ePubMsg));
            }
        });

        //Try connect
        connectMQTT();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.w(TAG, "onActivityResult: request="+requestCode+", result="+resultCode);
        if(requestCode == SCHEDULE_EXACT_ALARM_CODE) {
            if(resultCode == RESULT_OK) {
                connectMQTT();
            } else {
                showToast("Permission is denied.");
                stTv.setText("Please grant permission.");
            }
        }
    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String getEnter(EditText e){
        return e.getText().toString();
    }

    private void connectMQTT(){
        // Check if the SCHEDULE_EXACT_ALARM permission is granted
        // https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
            boolean v = alarmManager.canScheduleExactAlarms();
            Log.d(TAG, "SCHEDULE_EXACT_ALARM is granted..."+v);
            if(!v) {
                //Request permission
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult2(intent, SCHEDULE_EXACT_ALARM_CODE);
                return;
            }
        }

        Log.w(TAG, "Connecting MQTT server...");
        stTv.setText("Connecting...");
        //Set option
        MqttConnectOptions options = new MqttConnectOptions();
        if(!TextUtils.isEmpty(MQTT_USERNAME) && !TextUtils.isEmpty(MQTT_PASSWORD)) {
            options.setUserName(MQTT_USERNAME);
            options.setPassword(MQTT_PASSWORD.toCharArray());
        }
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        try {
            IMqttToken token = mqtt.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "Connect success");
                    stTv.setText("Connected");
                    //Subscribe
                    subscribe(getEnter(eSubTp));
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Error..."+exception.getMessage());
                    String tsx = "Connect onFailure: "+exception.getMessage();
                    stTv.setText(tsx);
                }
            });
        }catch (MqttException e){
            e.printStackTrace();

            String tsx = "Connect MqttException: "+e.getMessage();
            stTv.setText(tsx);
        }
    }

    private void disconnectMQTT() {
        Log.d(TAG, "Disconnecting MQTT server...");
        try {
            IMqttToken token = mqtt.disconnect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "Disconnect success...");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Disconnect failed...");
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
            Log.e(TAG, "Error..."+e.getMessage());
        }
    }

    private void subscribe(@NonNull String topic){
        Log.w(TAG, "Try to subscribe "+topic+" topics");
        //Connect
        if (!mqtt.isConnected()){
            showToast("Please connect before retry again"); return;
        }
        //Action
        if (TextUtils.isEmpty(topic)){
            showToast("Please enter topic!"); return;
        }
        topic = topic.trim();
        try {
            //Set
            IMqttToken token = mqtt.subscribe(topic, 0);
            //Check result
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "Subscribed..."
                            + Arrays.toString(asyncActionToken.getTopics()));

                    showToast("Subscribed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Subscribe failed..."
                            +Arrays.toString(asyncActionToken.getTopics()));

                    showToast("Subscribe error!");
                }
            });
        }catch (MqttException e){
            e.printStackTrace();

            showToast(e.getMessage());
        }
    }

    private void unsubscribe(@NonNull String topic){
        try {
            //Set
            IMqttToken token = mqtt.unsubscribe(topic);
            //Check result
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "UnSubscribed..."
                            + Arrays.toString(asyncActionToken.getTopics()));
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "UnSubscribed failed..."
                            +Arrays.toString(asyncActionToken.getTopics()));
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    public void publish(@NonNull String topic, @NonNull String payload){
        //Connect
        if (!mqtt.isConnected()){
            showToast("Please connect before retry again"); return;
        }
        //Action
        if (TextUtils.isEmpty(topic) || TextUtils.isEmpty(payload)){
            showToast("Please enter topic and payload!"); return;
        }
        topic = topic.trim();
        payload = payload.trim();
        try {
            byte[] encodedPayload = payload.getBytes(StandardCharsets.UTF_8);
            MqttMessage message = new MqttMessage(encodedPayload);
            mqtt.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void startActivityForResult2(final Intent intent, final int requestCode){
        activityLauncher.launch(intent, new BetterActivityResult.OnActivityResult<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                MainActivity.this.onActivityResult(requestCode, result.getResultCode(), result.getData());
            }
        });
    }
}