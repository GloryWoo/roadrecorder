package harman.com.monitor.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import harman.com.R;
import harman.com.Recorder;
import harman.com.cloud.Cloud;
import harman.com.cloud.Response;
import harman.com.cloud.net.HttpMidStat;
import harman.com.monitor.adapters.ServerItemAdapter;
import harman.com.monitor.models.DownRecord;
import harman.com.monitor.models.ServerItem;
import harman.com.monitor.models.VideoItem;
import io.realm.Realm;
import io.realm.RealmResults;

public class FragServer extends Fragment implements AdapterView.OnItemClickListener {

    Recorder recorder;

    private static final String LAST_REFRESH_TIME = "com.harman.ctg.roadrecord.lastrefresh";
    @Bind(R.id.refresh_btn)
    Button refreshBtn;
    @Bind(R.id.record_lv)
    ListView recordLv;
    public static String downPath;
    @Bind(R.id.autodown_btn)
    Button autodownBtn;
    private Cloud cloud;
    private ArrayList<ServerItem> galleryItems = new ArrayList<ServerItem>();
    public Queue<ServerItem> downVideoBlockQ = new LinkedBlockingDeque<ServerItem>();
    public DownAllThread downThread;
    public boolean downThreadRun = false;
    private ServerItemAdapter galleryAdapter;
    private Realm realm;

    private ActionMode actionMenu = null;
    private ServerItem itemLongClicked = null;
    private int itemPositionLongClicked = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_server, container, false);
        ButterKnife.bind(this, view);

        if (recorder.myApp.recService != null)
            realm = recorder.myApp.recService.realm;
        galleryAdapter = new ServerItemAdapter(recorder, galleryItems);
        recordLv.setAdapter(galleryAdapter);
        recordLv.setOnItemClickListener(this);
        initializeGallary();

        if (recorder.myApp.recService == null)
            ;
        else
            cloud = recorder.myApp.recService.cloud;
        return view;
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        recorder = (Recorder) getActivity();
        File downDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "down");
        if (!downDir.exists())
            downDir.mkdirs();
        downPath = downDir.getPath();
        downThread = new DownAllThread();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    public void updateGallery(ServerItem si) {
        galleryAdapter.notifyDataSetChanged();
    }

    public void initializeGallary() {
        galleryItems.clear();

        RealmResults<DownRecord> r = realm.where(DownRecord.class).findAllSorted("recordName");
        for (DownRecord rec : r) {
            if (rec.getDownSize() != rec.getRecordLen())
                galleryItems.add(new ServerItem(rec.getRecordId(), rec.getRecordName(), rec.getDownSize(), rec.getRecordLen()));
        }
        galleryAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ServerItem curItm = (ServerItem) parent.getItemAtPosition(position);

        if (curItm.downSize >= curItm.recordLen) {
            Toast.makeText(recorder, "Download Completed!", Toast.LENGTH_SHORT).show();
            return;
        }
        cloud.getFileSocket(downPath, curItm.record_id, curItm.downSize, curItm.recordLen - curItm.downSize,
            new Response.Handler() {
                @Override
                public void onResult(Response response) throws JSONException {
                    if (response.code == 200) {
                        JSONObject json = (JSONObject) response.body;
                        String fn = json.optString("filename");
                        int contentlen = json.optInt("contentlen");
                        int writelen = json.optInt("writelen");

                        String startTmS = fn.split("_")[0];
                        long startTmL = Long.parseLong(startTmS);
                        Date date = new Date(startTmL);
                        VideoItem vdo = new VideoItem(downPath + "/" + fn, null, date, false, false);

                        curItm.downSize += contentlen;
                        galleryAdapter.notifyDataSetInvalidated();

                        recorder.fragNetGallery.fragDown.addVideoItem(vdo);
                        realm.beginTransaction();
                        RealmResults<DownRecord> r = realm.where(DownRecord.class).equalTo("recordName", fn).findAll();
                        if (!r.isEmpty()) {
                            DownRecord dr = r.first();
                            dr.setDownSize(curItm.downSize);
                        }
                        realm.commitTransaction();

                    }
                }
            });
    }


    private void delete() {
        if (itemLongClicked != null) {

        }

        itemLongClicked = null;
    }

    @OnClick(R.id.refresh_btn)
    public void onRefreshClick() {
        if (cloud != null) {

            long startTime = recorder.pref.getLong(LAST_REFRESH_TIME, 0);
            long endTime = System.currentTimeMillis();
            if (startTime == 0) {
                Date start = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(start);
                int year = cal.get(Calendar.YEAR);
                cal.set(Calendar.YEAR, year - 1);
                startTime = cal.getTimeInMillis();
            }

            SharedPreferences.Editor edit = recorder.pref.edit();
            edit.putLong(LAST_REFRESH_TIME, endTime);
            edit.commit();

            cloud.getRecordId(startTime, endTime, new Response.Handler() {
                @Override
                public void onResult(Response response) throws JSONException {
                int code = response.code;
                if (code == 200) {
                    JSONObject json = (JSONObject) response.body;
                    JSONArray jsonArr = json.optJSONArray("data");
                    String recordname = "";
                    RealmResults<DownRecord> r;
                    DownRecord f;
                    if (jsonArr != null) {
                        int len = jsonArr.length();
                        JSONObject obj = null;
                        realm.beginTransaction();

                        for (int i = 0; i < len; i++) {
                            obj = jsonArr.getJSONObject(i);
                            recordname = obj.optString("recorder_name");
                            r = realm.where(DownRecord.class).equalTo("recordName", recordname).findAll();
                            if (r != null && r.size() > 0)
                                f = r.first();
                            else
                                f = realm.createObject(DownRecord.class);
                            f.setRecordId(obj.optString("recorder_id"));
                            f.setRecordName(recordname);
                            f.setRecordLen(obj.optInt("length"));
                        }
                        realm.commitTransaction();
                        if (len > 0)
                            initializeGallary();
                    }
                }

                }
            });
        }
    }

    @OnClick(R.id.autodown_btn)
    public void onAutoDownClick() {
        if(!downThreadRun){
            new Thread(downThread).start();
        }
    }



    class DownAllThread extends Thread{
        ServerItem curItm;

        @Override
        public void run() {
            downThreadRun = true;
            final Semaphore available = new Semaphore(0);
            for(ServerItem itm : galleryItems){
                if(itm.downSize != itm.recordLen)
                    downVideoBlockQ.add(itm);
            }
            while(!downVideoBlockQ.isEmpty()){
                curItm = downVideoBlockQ.poll();

                cloud.getFileSocket(downPath, curItm.record_id, curItm.downSize, curItm.recordLen - curItm.downSize,
                    new Response.Handler() {
                        @Override
                        public void onResult(Response response) throws JSONException {
                            if (response.code == 200) {
                                JSONObject json = (JSONObject) response.body;
                                String fn = json.optString("filename");
                                int contentlen = json.optInt("contentlen");
                                int writelen = json.optInt("writelen");

                                String startTmS = fn.split("_")[0];
                                long startTmL = Long.parseLong(startTmS);
                                Date date = new Date(startTmL);
                                VideoItem vdo = new VideoItem(downPath + "/" + fn, null, date, false, false);

                                curItm.downSize += contentlen;
                                galleryAdapter.notifyDataSetInvalidated();

                                recorder.fragNetGallery.fragDown.addVideoItem(vdo);
                                realm.beginTransaction();
                                RealmResults<DownRecord> r = realm.where(DownRecord.class).equalTo("recordName", fn).findAll();
                                if (!r.isEmpty()) {
                                    DownRecord dr = r.first();
                                    dr.setDownSize(curItm.downSize);
                                }
                                realm.commitTransaction();

                            }
                            available.release();
                        }
                    });
                try {
                    available.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            downThreadRun = false;
        }
    }

}