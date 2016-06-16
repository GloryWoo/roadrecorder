package harman.com.cloud.net;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by jishao on 1/20/16.
 */
public class HttpResponse {
    public int code;
    public ByteArrayOutputStream body;

    public interface Listener {
        void onResponse(HttpResponse resp);
//        void onProcessing(int writeLen);
    }

    @Override
    public String toString() {
        return "code=" + code + ",body=" + body;
    }
}
