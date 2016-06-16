package harman.com.monitor.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.Semaphore;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import harman.com.R;
import harman.com.Recorder;
import harman.com.cloud.Cloud;
import harman.com.cloud.Response;

public class FragSetting extends Fragment {
    private static final String TAG = "FragSetting";
    Recorder recorder;
    @Bind(R.id.getRandAcc)
    Button getRandAcc;
    @Bind(R.id.login)
    Button mLogin;
    @Bind(R.id.username)
    EditText mUsername;
    @Bind(R.id.passwd)
    EditText mPasswd;


    private Context ctx;
    private Cloud cloud;

    private boolean hasAccount = false;
    private String account;
    private String password;
    private boolean logined = false;
    public String loginId;
    public String token;
    SharedPreferences sp;
    Handler uiThreadHandler;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_setting, container, false);
        ButterKnife.bind(this, view);
        if(recorder.myApp.recService == null)
            ;
        else
            cloud = recorder.myApp.recService.cloud;
        getAccountAndLogin();
        return view;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        recorder = (Recorder) getActivity();
        ctx = recorder;
        sp = ctx.getSharedPreferences("password", 0);

        uiThreadHandler = new Handler(ctx.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private synchronized String getDeviceId() {
        String android_type = Build.MANUFACTURER + ":" + Build.MODEL;
        String android_id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (android_id == null) {
            TelephonyManager telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            android_id = telephonyManager.getDeviceId();

            if (android_id == null) {
                // just give some random string
                android_id = UUID.randomUUID().toString();
            }
        }

        final String[] reserveds = {"|", "\\", "?", "*", "<", "\"", ":", ">"};

        String deviceType = android_type.replaceAll("\\s", "");
        String deviceId = android_id.replaceAll("\\s", "");

        for (String s : reserveds) {
            deviceType = deviceType.replace(s, "_");
            deviceId = deviceId.replace(s, "_");
        }

        return deviceType + deviceId;
    }

    private synchronized void getAccountAndLogin() {
        Log.d(TAG, "get Account and login");


        account = sp.getString("account", null);
        password = sp.getString("password", null);
        if (account != null && password != null) {
            hasAccount = true;
            mUsername.setText(account);
            mPasswd.setText(password);
            //mLogin.setText("logined");
        }

        if(cloud == null)
            return;
        final Semaphore available = new Semaphore(0);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String deviceId = getDeviceId();
                if (!hasAccount) {
                    cloud.requestAccount(deviceId, new Response.Handler() {
                        @Override
                        public void onResult(Response response) {
                            if (response.code == 200) {
                                try {
                                    JSONObject extra = (JSONObject) response.body;
                                    if (extra != null) {
                                        extra = extra.getJSONObject("data");
                                        account = extra.optString("account");
                                        password = extra.optString("password");
                                        if (account != null && password != null)
                                            hasAccount = true;

                                        Log.d(TAG, "got a account:" + account + "," + password);

                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            available.release();
                        }

                    });


                    try {
                        available.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ;

                    if (hasAccount) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("account", account);
                        editor.putString("password", password);
                        editor.commit();
//                        uiThreadHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                mPasswd.setText(password);
//                                mLogin.setText("logined");
//                            }
//                        });

                    }
                }

                if (!logined) {
                    cloud.login(account, password, new Response.Handler() {
                        @Override
                        public void onResult(Response response) {
                            if (response.code == 200) {
                                logined = true;
                                JSONObject extra = (JSONObject) response.body;
                                JSONObject data = extra.optJSONObject("data");
                                loginId = extra.optString("id");
                                token = extra.optString("token");
                                SharedPreferences.Editor editor = sp.edit();
                                editor.putString("id", loginId);
                                editor.putString("token", token);
                                Log.d(TAG, response.body.toString());

                                mPasswd.setText(password);
                                mLogin.setText("logined");
                            }
                            available.release();
                        }
                    });

                    try {
                        available.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ;
                }
            }
        });
        t.start();
    }

    Thread loginThread = new Thread(new Runnable() {
        @Override
        public void run() {
            cloud.login(account, password, new Response.Handler() {
                @Override
                public void onResult(Response response){
                    if (response.code == 200) {
                        logined = true;
                        JSONObject extra = (JSONObject) response.body;
                        JSONObject data = extra.optJSONObject("data");
                        loginId = data.optString("id");
                        token = data.optString("token");
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("id", loginId);
                        editor.putString("token", token);
                        Log.d(TAG, response.body.toString());
                    }
                }
            });
        }
    });

    @OnClick({R.id.getRandAcc, R.id.login})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.getRandAcc:
                break;
            case R.id.login:
                if(cloud != null && account != null && password != null)
                    new Thread(loginThread).start();
                break;
        }
    }
}