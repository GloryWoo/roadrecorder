package harman.com.cloud.requests;

import harman.com.cloud.Response;
import harman.com.cloud.StateMachine;
import harman.com.cloud.net.HttpRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jishao on 5/10/16.
 */
public class IntentRecognizeRequest extends UserRequest {
    private String speech;

    public IntentRecognizeRequest(String speech_, Response.Handler h, StateMachine m) {
        super(h, m);
        speech = speech_;
    }

    public HttpRequest getHttpRequest(String baseUrl) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-token", stateMachine.getToken());
        headers.put("X-version", stateMachine.getVersion());

        String url = baseUrl + "/pocserver/onlinevr";

        Map<String, String> params = new HashMap<String, String>();
        if(speech != null)
            params.put("vr_content", speech);

        HttpRequest hr = new HttpRequest(stateMachine.getContext(), HttpRequest.Method.POST, url, headers, HttpRequest.convertParamsToByteArray(params), this);
        return hr;
    }
}
