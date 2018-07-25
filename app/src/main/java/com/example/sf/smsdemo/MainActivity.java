package com.example.sf.smsdemo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {
    private final static String MSG_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private TextView smsInfo;
    private EditText editText;
    private Button okBtn;
    private OkHttpClient client = new OkHttpClient();
    private SharedPreferences settings;
    private static final String data = "DATA";
    private static final String address = "address";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] perms = {Manifest.permission.RECEIVE_SMS};
        if (!EasyPermissions.hasPermissions(MainActivity.this, perms)) {
            EasyPermissions.requestPermissions(MainActivity.this, "讀取簡訊權限",
                    100, perms);
        }
        setContentView(R.layout.activity_main);
        smsInfo = findViewById(R.id.sms_info);
        editText = findViewById(R.id.input_edit);
        okBtn = findViewById(R.id.ok_btn);
        readData();
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String url = editText.getText().toString();
                if(TextUtils.isEmpty(url) || !url.startsWith("https") || !url.startsWith("http")){
                    smsInfo.setText("網址輸入錯誤...");
                    return;
                }
                saveData();
                smsInfo.setText("讀取中...");
                sendData("", true);
            }
        });
        registerReceiver(mBroadcastReceiver,new IntentFilter(MSG_RECEIVED));
    }

    private void sendData(String data, final boolean isTest){
        HttpUrl.Builder builder = HttpUrl.parse(editText.getText().toString()).newBuilder();
        builder.addQueryParameter("smsData", data);
        Request request = new Request.Builder()
                .url(builder.toString())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(isTest) {
                            smsInfo.setText("連線失敗");
                        } else{
                            smsInfo.setText("傳送失敗");
                        }
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(isTest) {
                            smsInfo.setText("連線成功");
                        } else{
                            smsInfo.setText("傳送成功");
                        }
                    }
                });
            }
        });
    }
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context arg0, Intent intent) {
            if(intent.getAction().equals(MSG_RECEIVED)){
                Bundle msg = intent.getExtras();
                Object[] messages = (Object[]) msg.get("pdus");
                SmsMessage sms = SmsMessage.createFromPdu((byte[])messages[0]);

                StringBuilder strBuilder = new StringBuilder();
                strBuilder.append(editText.getText().toString());
                strBuilder.append("收到的簡訊內容為:"+sms.getMessageBody());
                smsInfo.setText(strBuilder.toString());
                smsInfo.setText("將簡訊內容傳送中...");
                sendData(sms.getMessageBody(), false);
            }
        }
    };

    public void readData(){
        settings = getSharedPreferences(data,0);
        editText.setText(settings.getString(address, "https://jsonplaceholder.typicode.com/posts"));
    }

    public void saveData(){
        settings = getSharedPreferences(data,0);
        settings.edit()
                .putString(address, editText.getText().toString())
                .commit();
    }

}
