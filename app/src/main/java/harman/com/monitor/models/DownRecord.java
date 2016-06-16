package harman.com.monitor.models;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.RealmClass;

/**
 * Created by  on 2015-7-16.
 */
@RealmClass
public class DownRecord extends RealmObject {
    private String recordId;
    private String recordName;
    private int   recordLen;
    private int   downSize;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getRecordName() {
        return recordName;
    }

    public void setRecordName(String recordName) {
        this.recordName = recordName;
    }

    public int getRecordLen() {
        return recordLen;
    }

    public void setRecordLen(int recordLen) {
        this.recordLen = recordLen;
    }

    public int getDownSize() {
        return downSize;
    }

    public void setDownSize(int downSize) {
        this.downSize = downSize;
    }
}
