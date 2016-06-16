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
 * Created by on 10/23/15.
 */
public class UploadRecordRequest extends UserRequest {
    public String path;
    public int fileCount;
    public int fileIdx;
    public String upName;
//    public String md5;
    byte[] upData;
    public UploadRecordRequest(byte[] data, String uploadname, int count, int idx, Response.Handler h, StateMachine m) {
        super(h, m);
        upData = data;
        fileCount = count;
        fileIdx = idx;
        upName = uploadname;
//        this.md5 = md5;
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
        String url = baseUrl + "/pocserver/uploadrecorder";

        byte[] gzipData = null;
        try {
            gzipData = Util.gzip(upData);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        StringBuffer fi=new StringBuffer();
        String md5 = Util.makeMD5(gzipData);
        fi.append("fn=").append(upName).append(";md5=").append(md5)
                .append(";fbn=").append(fileCount).append(";fcbn=").append(fileIdx);
        headers.put("X-fileInfo", fi.toString());
        HttpRequest hr = new HttpRequest(stateMachine.getContext(), HttpRequest.Method.POST, url, headers, gzipData, this);
        return hr;
    }


    public boolean requireWaitingInOrder() {
        return false;
    }
}
