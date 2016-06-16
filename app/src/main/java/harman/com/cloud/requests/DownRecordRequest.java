package harman.com.cloud.requests;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import harman.com.Util;
import harman.com.cloud.Response;
import harman.com.cloud.StateMachine;
import harman.com.cloud.net.HttpMidStat;
import harman.com.cloud.net.HttpRequest;
import harman.com.cloud.net.HttpResponse;


/**
 * Created by on 10/23/15.
 */
public class DownRecordRequest extends UserRequest {
    public String storePath;
    public String recordId;
    int downOff;
    int fileLen;

    public DownRecordRequest(String path, String id, int offset, int len, Response.Handler h, StateMachine m) {
        super(h, m);
        storePath = path;
        recordId = id;
        downOff = offset;
        fileLen = len;

    }
    @Override
    public HttpRequest getHttpRequest(String baseUrl) {

        Map<String, String> headers = new HashMap<String, String>();

        headers.put("accept", "application/json, text/plain, */*");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        headers.put("Accept-Encoding","gzip, deflate");
        headers.put("Content-Type", "application/octet-stream");

        headers.put("X-version", stateMachine.getVersion());
        headers.put("X-token", stateMachine.getToken());
        if(fileLen == 0)
            headers.put("Range", "bytes="+downOff+"-");
        else
            headers.put("Range", "bytes="+downOff+"-"+fileLen);
        String url = baseUrl + "/pocserver/downrecorder?recorder_id=" + recordId;


        HttpRequest hr = new HttpRequest(stateMachine.getContext(), HttpRequest.Method.GET, url, headers, null, this);
        hr.storePath = storePath;
        hr.offset = downOff;
        return hr;
    }


    public boolean requireWaitingInOrder() {
        return false;
    }

}

