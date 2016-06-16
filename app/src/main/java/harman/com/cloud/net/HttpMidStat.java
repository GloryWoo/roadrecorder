package harman.com.cloud.net;

import org.json.JSONException;

/**
 * Created by jishao on 1/20/16.
 */
public class HttpMidStat {
    public int writelen;


    public HttpMidStat(int len){
        writelen = len;
    }

    public interface Handler {
        void onMidStat(HttpMidStat stat);
    }
}
