package harman.com.cloud.requests;



import harman.com.cloud.Response;
import harman.com.cloud.StateMachine;
import harman.com.cloud.net.HttpRequest;
import harman.com.cloud.net.HttpResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jishao on 10/22/15.
 */
public class LoginRequest extends UserRequest {

    private String account;
    private String password;

    public LoginRequest(String account_, String password_, Response.Handler h, StateMachine m) {
        super(h, m);
        account = account_;
        password = password_;
    }

    public HttpRequest getHttpRequest(String baseUrl) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-version", stateMachine.getVersion());

        String url = null;
        try {
            url = baseUrl + "/pocserver/login?account=" + URLEncoder.encode(account, "utf-8") + "&password=" + URLEncoder.encode(password, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        HttpRequest hr = new HttpRequest(stateMachine.getContext(), HttpRequest.Method.GET, url, headers, null, this);
        return hr;
    }

    @Override
    public boolean requireLogined() {
        return false;
    }

    @Override
    public boolean isLogin() {
        return true;
    }

    @Override
    public void onResponse(HttpResponse httpResp) {
        try {
            int code = httpResp.code;

            if(code == 200) {
                JSONObject body = parseJSON(httpResp.body);

                JSONObject dataObj = body.getJSONObject("data");
                String token = dataObj.getString("token");
                String id = dataObj.getString("id");

                stateMachine.logined(account, password, token, id);
            } else {
                stateMachine.loginFailed();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onResponse(httpResp);
    }
}
