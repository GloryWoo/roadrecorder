package harman.com;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

import harman.com.R;
import harman.com.cloud.Cloud;
import harman.com.cloud.Response;
import harman.com.monitor.models.FileModel;
import harman.com.monitor.models.FileModel;
import harman.com.videocapture.VideoFile;
import harman.com.videocapture.camera.CameraWrapper;
import harman.com.videocapture.configuration.CaptureConfiguration;
import harman.com.videocapture.configuration.PredefinedCaptureConfigurations;
import harman.com.videocapture.recorder.LoopVideoRecorder;
import harman.com.videocapture.recorder.VideoRecorderInterface;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 *
 */
public class RecService extends Service implements VideoRecorderInterface, LoopVideoRecorder.LoopVideoRecorderListener, SensorEventListener {
    private static final String TAG = "RecService";
    private HandlerThread handlerThread;
    public ServiceHandler handler;
    private static final int RECORDING_NOTIFICATION_ID  = 1001;
    private static final String ARG_SECTION_NUMBER  = "section_number";

    public static final String	DEFAULT_PREFIX		= "video_";
    public static final String	DEFAULT_EXTENSION	= ".mp4";
    private static final int DEFAULT_MAXFILENUMS    = 100;
    private static final int DEFAULT_MAXDURATION    = 5*60;

    private static final String	SAVE_VIDEOFILE_NUMS		= "com.harman.ctg.roadstyle.filenums";
    private static final String SAVE_MAX_FILEDURATION   = "com.harman.ctg.roadstyle.maxfileduration";
    private static final String	SAVE_VIDEOFILE_INDEX	= "com.harman.ctg.roadstyle.fileindex";
    private boolean quit;

    public static final int CMD_START_RECORDING = 0x00000001;
    public static final int CMD_STOP_RECORDING = 0x00000002;

    private static final int CMD_CLOUD_ACQUIRE = 0x00010001;
    public final static int BlockSize = 4000000;

    public LoopVideoRecorder       loopVideoRecorder = null;
    public SurfaceView preview;
    public Realm realm;
    private NotificationCompat.Builder builder;
    private int                     fileNums = DEFAULT_MAXFILENUMS;
    private int                     maxFileDuration = DEFAULT_MAXDURATION;
    private int                     fileIndex = 0;
    private CaptureConfiguration captureConfiguration;
    private SharedPreferences          pref;
    private ArrayList<VideoFile>    videoFileList = new ArrayList<VideoFile>();
    public static boolean monitoring;
    private MyBinder mBinder = new MyBinder();
    MyApplication myApp;
    public WindowManager windowManager;
    public LayoutParams wmParams;
    public Cloud cloud;
    public Queue<FileModel> uploadVideoBlockQ;
    public UploadRecordBlockThread upBlockThread;
    public boolean upBlockThreadRun = false;
//    public boolean neadAutoResume;
    public class MyBinder extends Binder {
        public SurfaceView getSurfaceView(){
            return preview;
        }

        public boolean isMonitoring(){
            return monitoring;
        }

        public ServiceHandler getServiceHandler(){
            return handler;
        }
    };

    public class UploadRecordBlockThread extends Thread{
        public FileModel curFileItm;
        int curFileBlockNum = 0;
        int needUpBlockNum = 0;
        int curFileRetNum = 0;
        byte[] upInitSz;
        byte[] curUpRet;
        byte[] needUpSz;
        long timeStart, timeEnd;
        String upfn = null, fn = null;
        JSONObject json = null;
        byte[] byteSzRet;
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), getResources().getString(R.string.monitor_name));


        File curFile;
        int fs;
        public UploadRecordBlockThread(){}

        @Override
        public void run() {
            final Semaphore available = new Semaphore(0);
            while(!uploadVideoBlockQ.isEmpty()){
                upBlockThreadRun = true;

                curFileRetNum = 0;
                curFileItm = uploadVideoBlockQ.poll();
                fn = curFileItm.getFilename();
                curFile = new File(dir, fn);
                if(!curFile.exists()){
                    available.release();
                    continue;
                }
                fs = (int) curFile.length();
                if(fs%BlockSize==0){
                    curFileBlockNum=fs/BlockSize;
                }else{
                    curFileBlockNum=fs/BlockSize+1;
                }
                upInitSz = curFileItm.getUploadSz();
                if(upInitSz == null)
                    needUpSz = new byte[curFileBlockNum];
                else
                    needUpSz = new byte[curFileBlockNum - upInitSz.length];
                needUpBlockNum = needUpSz.length;
                curUpRet = new byte[needUpBlockNum];
                for(byte i = 1, k = 0; i < curFileBlockNum+1; i++){
                    if(upInitSz == null || !Util.ByteArrayContains(upInitSz, i)){
                        needUpSz[k++] = i;
                    }
                }
                if(curFileItm.getDate() == null || curFileItm.getStopDate() == null){
                    available.release();
                    continue;
                }
                timeStart = curFileItm.getDate().getTime();
                timeEnd = curFileItm.getStopDate().getTime();
                upfn = String.format("%d_%d.mp4", timeStart, timeEnd);

                for(byte i : needUpSz){
                    cloud.uploadRecord(curFile, upfn, curFileBlockNum, i,
                        new Response.Handler() {
                            @Override
                            public void onResult(Response response) throws JSONException {
                                if (response.code == 200) {
                                    json = (JSONObject) response.body;
                                    String retArr = json.optString("data");

                                    if (retArr != null) {
                                        byteSzRet = Util.StringToByteArray(retArr);
                                        for(byte b : byteSzRet){
                                            if(!Util.ByteArrayContains(curUpRet, b)){
                                                curUpRet[curFileRetNum] = b;
                                                break;
                                            }
                                        }
                                    }
                                } else {

                                }
                                curFileRetNum++;
                                if(curFileRetNum == needUpBlockNum) {
                                    final byte[] actualRetSz = Util.ByteArrayStrim0(curUpRet);
                                    final FileModel tmpFile = Util.FileModelClone(curFileItm);

                                    realm.beginTransaction();
                                    RealmResults<FileModel> r = realm.where(FileModel.class).equalTo("filename", tmpFile.getFilename()).findAll();
                                    FileModel fm = r.first();
                                    if (fm != null) {
                                        if (actualRetSz.length == needUpBlockNum) {
                                            fm.setUploadStat((byte) 2);
                                            fm.setUploadSz(null);
                                        } else {
                                            fm.setUploadStat((byte) 1);
                                            fm.setUploadSz(actualRetSz);
                                            uploadVideoBlockQ.add(tmpFile);
                                        }
                                    }
                                    realm.commitTransaction();
                                    available.release();
                                }
                            }
                        });
                }
                try {
                    available.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            upBlockThreadRun = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myApp = (MyApplication) getApplication();
        myApp.recService = this;
        quit = false;

        windowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        wmParams = new WindowManager.LayoutParams();
        /**
         * 以下都是WindowManager.LayoutParams的相关属性 具体用途可参考SDK文档
         */
        // 设置window type
        wmParams.type = LayoutParams.TYPE_PHONE;

        // 设置Window flag
        // 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
        wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                | LayoutParams.FLAG_NOT_FOCUSABLE
                | LayoutParams.FLAG_NOT_TOUCHABLE;

        wmParams.gravity = Gravity.CENTER | Gravity.BOTTOM; // 调整悬浮窗口至左上角
        wmParams.width = LayoutParams.MATCH_PARENT;
        wmParams.height = 1460;
        handler = new ServiceHandler(getMainLooper());
//        Message m = handler.obtainMessage(CMD_CLOUD_ACQUIRE);
//        m.sendToTarget();

        ((SensorManager)getSystemService(Context.SENSOR_SERVICE)).registerListener(this,
                ((SensorManager) this.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_PROXIMITY),
                SensorManager.SENSOR_DELAY_NORMAL);


//        RealmConfiguration realmConfig = new RealmConfiguration.Builder(context).build();
//        Realm.setDefaultConfiguration(realmConfig);

// Get a Realm instance for this thread
        realm = Realm.getInstance(getApplicationContext());



        builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.record_icon_on)
                .setContentTitle("Video Recording...")
                .setContentText("Car Monitor")
                .setOngoing(true);
        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        fileNums = pref.getInt(SAVE_VIDEOFILE_NUMS, DEFAULT_MAXFILENUMS);
        maxFileDuration = pref.getInt(SAVE_MAX_FILEDURATION, DEFAULT_MAXDURATION);
        fileIndex       = pref.getInt(SAVE_VIDEOFILE_INDEX, 0);

        cloud = new Cloud(this, null);
        cloud.start();
//        uploadVideoQ = new LinkedBlockingDeque<>();
        uploadVideoBlockQ = new LinkedBlockingDeque<>();
//        upThread = new UploadRecordThread();
        upBlockThread = new UploadRecordBlockThread();
        initializeLoopRecorder();
    }




    private void initializeLoopRecorder() {
        // insert new file mode to realm
        String appName = getResources().getString(R.string.monitor_name);
        int sz = realm.allObjects(FileModel.class).size();
        if(sz != fileNums) {
            realm.beginTransaction();
            realm.allObjects(FileModel.class).clear();
            realm.commitTransaction();

            realm.beginTransaction();
            for(int n=0; n<fileNums; n++) {
                FileModel f = realm.createObject(FileModel.class);
                f.setSubdir(appName);
                f.setFilename(DEFAULT_PREFIX + String.valueOf(n) + DEFAULT_EXTENSION);
                f.setDate(new Date());
                f.setLocked(false);
            }
            realm.commitTransaction();

            //update
            if(sz > fileNums) {
                fileIndex = 0;

                SharedPreferences.Editor edit = pref.edit();
                edit.putInt(SAVE_VIDEOFILE_INDEX, fileIndex);
                edit.commit();
            }
        }
        realm.refresh();

        // restrive
        videoFileList.clear();
        for (FileModel f : realm.allObjects(FileModel.class)) {
            videoFileList.add(new VideoFile(f.getSubdir(), f.getFilename(), f.getDate(), f.getLocked()));
        }

        captureConfiguration =  createCaptureConfiguration();
    }

    public void showHidePreview(boolean isShow){
        if(preview == null || !monitoring)
            return;
        if(isShow){
            wmParams.width = LayoutParams.MATCH_PARENT;
            wmParams.height = 1460;
            windowManager.updateViewLayout(preview, wmParams);
        }
        else{
            wmParams.width = 1;
            wmParams.height = 1;
            windowManager.updateViewLayout(preview, wmParams);
        }
    }

    public void startLoopRecorder() {
        if(!monitoring) {

//            initializeLoopRecorder();
            if (loopVideoRecorder == null) {
//                bk_preview.removeAllViews();
//                SurfaceView preview = new SurfaceView(context.getApplicationContext());
//                bk_preview.addView(preview);

                preview = new SurfaceView(getApplicationContext());
                loopVideoRecorder = new LoopVideoRecorder(this, captureConfiguration, videoFileList, fileIndex, new CameraWrapper(), preview.getHolder());
                loopVideoRecorder.setLoopVideoRecorderListener(this);
//                windowManager.addView(preview, wmParams);

                if(myApp.recorder != null && myApp.recorder.monitor != null) {
//                    myApp.recorder.monitor.bk_preview.addView(preview);

                }
            }
            windowManager.addView(preview, wmParams);
            // start recroding auto
            Handler handler = new Handler();
            final Runnable r = new Runnable() {
                public void run() {
                    loopVideoRecorder.toggleRecording();  // start
                    monitoring = true;
                }
            };
            handler.postDelayed(r, 500);
        }
        else{

        }
    }

    private CaptureConfiguration createCaptureConfiguration() {
        final PredefinedCaptureConfigurations.CaptureResolution resolution = getResolution(1); //{ "1080p", "720p", "480p" };
        final PredefinedCaptureConfigurations.CaptureQuality quality = getQuality(2); //{ "high", "medium", "low" };
        int fileDuration = maxFileDuration;  //CaptureConfiguration.NO_DURATION_LIMIT;
        int fileSize = CaptureConfiguration.NO_FILESIZE_LIMIT;
        return new CaptureConfiguration(resolution, quality, fileDuration, fileSize);
    }

    private PredefinedCaptureConfigurations.CaptureQuality getQuality(int position) {
        final PredefinedCaptureConfigurations.CaptureQuality[] quality = new PredefinedCaptureConfigurations.CaptureQuality[] { PredefinedCaptureConfigurations.CaptureQuality.HIGH, PredefinedCaptureConfigurations.CaptureQuality.MEDIUM,
                PredefinedCaptureConfigurations.CaptureQuality.LOW };
        return quality[position];
    }

    private PredefinedCaptureConfigurations.CaptureResolution getResolution(int position) {
        final PredefinedCaptureConfigurations.CaptureResolution[] resolution = new PredefinedCaptureConfigurations.CaptureResolution[] { PredefinedCaptureConfigurations.CaptureResolution.RES_1080P,
                PredefinedCaptureConfigurations.CaptureResolution.RES_720P, PredefinedCaptureConfigurations.CaptureResolution.RES_480P };
        return resolution[position];
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null)
            handleIntent(intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        quit = true;

        if (loopVideoRecorder != null) {
            loopVideoRecorder.stopRecording(null);
            loopVideoRecorder.releaseAllResources();
            loopVideoRecorder = null;
        }
//        handlerThread.quit();

        ((SensorManager) getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
        realm.close();

        if(myApp != null) {
            myApp.recService = null;
        }
        cloud.stop();
        Log.d(TAG, "onDestroy");
    }

    private void handleIntent(Intent i) {
        int cmd = i.getIntExtra("cmd", 0);
        Message m = handler.obtainMessage(cmd);
        // TODO customize the message for different command
        m.sendToTarget();
    }


    public class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if(quit)
                return;

            switch(msg.what) {
                case CMD_START_RECORDING:
                    startLoopRecorder();
                    break;
                case CMD_STOP_RECORDING:
                    if (loopVideoRecorder != null && monitoring) {
                        loopVideoRecorder.stopRecording(null);
                        monitoring = false;
                        windowManager.removeView(preview);
//                        preview = null;
//                        if(myApp.recorder != null)
//                            myApp.recorder.monitor.bk_preview.removeAllViews();
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }



    @Override
    public void onRecordingStopped(VideoFile vf) {
        //no-op
        if(myApp.recorder != null){
            myApp.recorder.fragGallery.updateGallery(vf);
        }
        realm.beginTransaction();
        RealmResults<FileModel> r = realm.where(FileModel.class).equalTo("filename", vf.getFilename()).findAll();
        if(r != null){
            FileModel fm = r.first();
            fm.setDate(vf.getDate());
            fm.setStopDate(vf.getStopDate());
            fm.setLocked(vf.getLocked());
            uploadVideoBlockQ.add(Util.FileModelClone(fm));
        }
        realm.commitTransaction();
        if(uploadVideoBlockQ.size() != 0 && !upBlockThreadRun)
            new Thread(upBlockThread).start();
    }

    @Override
    public void onRecordingStarted() {
        NotificationManager notifier = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notifier.notify(RECORDING_NOTIFICATION_ID, builder.build());
        if(myApp.recorder != null){
            myApp.recorder.setMenuItemStat(true);
        }
    }

    @Override
    public void onRecordingSuccess() {
        //no-op
    }

    @Override
    public void onRecordingFailed(String message) {
        //no-op
    }

    @Override
    public void onLoopRecordingStopped(ArrayList<VideoFile> videoFileList, int fileIndex) {
//        if(loopVideoRecorder != null) {
//            loopVideoRecorder.releaseAllResources();
//            loopVideoRecorder = null;
//        }

        if(myApp.recorder != null){
            myApp.recorder.setMenuItemStat(false);
        }


        this.fileIndex = (fileIndex + 1) % fileNums;
        SharedPreferences.Editor edit = pref.edit();
        edit.putInt(SAVE_VIDEOFILE_INDEX, fileIndex);
        edit.commit();

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(RECORDING_NOTIFICATION_ID);


    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] == 0) { //near
            if (loopVideoRecorder != null) {
                //Toast.makeText(getApplicationContext(), "Lock the video file.", Toast.LENGTH_SHORT).show();
                loopVideoRecorder.lockVideoFile();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //no-op
    }

    //////////////////////////////////////////////////////////////////////////
    //////////////////////////download func///////////////////////////////////
    //////////////////////////////////////////////////////////////////////////
    public void getRecordIdList(){

    }

}
