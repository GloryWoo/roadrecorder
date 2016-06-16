package harman.com.cloud.requests;



import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import harman.com.Util;
import harman.com.cloud.Response;
import harman.com.cloud.StateMachine;
import harman.com.cloud.net.HttpRequest;

/**
 * Created by jishao on 10/23/15.
 */
public class GetRecordIdRequest extends UserRequest {
    long startTm;
    long endTm;
    //    public String md5;
    byte[] upData;
    public GetRecordIdRequest(long start, long end, Response.Handler h, StateMachine m) {
        super(h, m);
        startTm = start;
        endTm = end;
    }
    @Override
    public HttpRequest getHttpRequest(String baseUrl) {

        Map<String, String> headers = new HashMap<String, String>();

        headers.put("accept", "*/*");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        headers.put("Accept-Encoding","gzip, deflate");
        headers.put("Content-Type", "application/json;charset=utf-8");

        headers.put("X-version", stateMachine.getVersion());
        headers.put("X-token", stateMachine.getToken());
        String url = baseUrl + "/pocserver/searchrecorder?";
        url += String.format("start_time=%d&end_time=%d", startTm, endTm);

        HttpRequest hr = new HttpRequest(stateMachine.getContext(), HttpRequest.Method.GET, url, headers, null, this);
        return hr;
    }


    public boolean requireWaitingInOrder() {
        return false;
    }
}
