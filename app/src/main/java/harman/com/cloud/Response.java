package harman.com.cloud;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jishao on 1/20/16.
 */
public class Response {
    public int code;
    public Object body;

    public interface Handler {
        void onResult(Response response) throws JSONException;
    }
}
