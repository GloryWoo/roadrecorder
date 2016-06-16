package harman.com.cloud.requests;


import android.util.Log;

import harman.com.cloud.Response;
import harman.com.cloud.StateMachine;
import harman.com.cloud.net.HttpMidStat;
import harman.com.cloud.net.HttpRequest;
import harman.com.cloud.net.HttpResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by jishao on 9/28/15.
 */
public abstract class UserRequest implements HttpResponse.Listener{
    protected static final String TAG = "UserRequest";
    protected Response.Handler handler;
//    protected HttpMidStat.Handler midHandler;
    protected StateMachine stateMachine;

    public UserRequest(Response.Handler h, StateMachine m) {
        handler = h;
        stateMachine = m;
    }

    public abstract HttpRequest getHttpRequest(String baseUrl);


    public boolean isLogin() {
        return false;
    }

    public boolean requireLogined() {
        return true;
    }

    public boolean requireWaitingInOrder() {
        return true;
    }


    @Override
    public void onResponse(HttpResponse httpResp) {
        Response resp = new Response();
        resp.code = httpResp.code;

        if(resp.code == -1) {
            resp.body = parseError(httpResp.body);
        } else {
            resp.body = parseJSON(httpResp.body);
        }

        stateMachine.handleResponse(handler, resp);
    }

//    @Override
//    public void onProcessing(int write){
//        if(midHandler != null){
//            HttpMidStat mid = new HttpMidStat(write);
//            stateMachine.handleMidStat(midHandler, mid);
//        }
//    }

    protected String parseError(ByteArrayOutputStream byteArray) {
        String errorMsg = null;
        try {
            errorMsg = byteArray.toString("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return errorMsg;
    }


    protected JSONObject parseJSON(ByteArrayOutputStream byteArray) {
        String jsonString = null;

        try {
            jsonString = byteArray.toString("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Log.d(TAG, jsonString);

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}


