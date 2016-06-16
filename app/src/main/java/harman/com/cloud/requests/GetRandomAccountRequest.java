package harman.com.cloud.requests;


import harman.com.cloud.Response;
import harman.com.cloud.StateMachine;
import harman.com.cloud.net.HttpRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jishao on 2/23/16.
 */
public class GetRandomAccountRequest extends UserRequest {
    private String deviceId;
    public GetRandomAccountRequest(String deviceId_, Response.Handler h, StateMachine m) {
        super(h, m);

        deviceId = deviceId_;
    }

    @Override
    public HttpRequest getHttpRequest(String baseUrl) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-version", stateMachine.getVersion());
        String url = null;
        try {
            url = baseUrl + "/pocserver/getRandomAccount?account=" + URLEncoder.encode(deviceId, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        HttpRequest hr = new HttpRequest(stateMachine.getContext(), HttpRequest.Method.GET, url, headers, null, this);
        return hr;
    }

    public boolean requireLogined() {
        return false;
    }
}
