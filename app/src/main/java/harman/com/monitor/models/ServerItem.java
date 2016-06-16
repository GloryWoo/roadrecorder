package harman.com.monitor.models;

import android.graphics.Bitmap;

import java.util.Date;

/**
 * Created by root on 16-6-12.
 */
public class ServerItem{

    public String record_id;
    public String record_name;
    public int   recordLen;
    public int   downSize;

    public ServerItem(String id, String name, int downSz, int len) {
        //dummy constructor
        record_id = id;
        record_name = name;
        downSize = downSz;
        recordLen = len;
    }

//    public ServerItem clone(){
//        ServerItem newItem = new ServerItem(record_id, record_name, downSize, recordLen);
//        return newItem;
//    }
}
