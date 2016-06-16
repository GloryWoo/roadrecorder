package harman.com.monitor.fragments;

import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import harman.com.R;
import harman.com.Recorder;
import harman.com.RecService;
import harman.com.videocapture.VideoFile;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by XiaXu on 2015-8-7.
 */
public class MonitorFragment extends Fragment  {
    @Bind(R.id.vr_monitortureview)
    public FrameLayout bk_preview;

    @Bind(R.id.startstop_ins)
    public TextView startstop_ins;
    private static final String ARG_SECTION_NUMBER  = "section_number";

    public static final String	DEFAULT_PREFIX		= "video_";
    public static final String	DEFAULT_EXTENSION	= ".mp4";
    private static final int DEFAULT_MAXFILENUMS    = 6;
    private static final int DEFAULT_MAXDURATION    = 5*60;

//    private static final int RECORDING_NOTIFICATION_ID  = 1001;

    private static final String	SAVE_VIDEOFILE_NUMS		= "com.harman.ctg.roadstyle.filenums";
    private static final String SAVE_MAX_FILEDURATION   = "com.harman.ctg.roadstyle.maxfileduration";
    private static final String	SAVE_VIDEOFILE_INDEX	= "com.harman.ctg.roadstyle.fileindex";

    private int                     fileNums = DEFAULT_MAXFILENUMS;
    private int                     maxFileDuration = DEFAULT_MAXDURATION;
    private int                     fileIndex = 0;
//    private CaptureConfiguration    captureConfiguration;
//    private Realm realm;

    private boolean                 monitoring = false;
    private ArrayList<VideoFile>    videoFileList = new ArrayList<VideoFile>();
    View  fragView = null;
    public int fragHeight = 0;
    private SharedPreferences          pref;
    private static Context             context;
    Recorder recorder;
    Handler handler = new Handler();
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MonitorFragment.
     */
    public MonitorFragment newInstance(Context ctx, int sectionNumber) {
        MonitorFragment fragment = new MonitorFragment(ctx);
        context = ctx;
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public MonitorFragment(Context ctx) {
        // Required empty public constructor
        context = ctx;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        recorder = (Recorder)getActivity();
//        realm = Realm.getInstance(context.getApplicationContext());
//
//        ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).registerListener(this,
//                ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_PROXIMITY),
//                SensorManager.SENSOR_DELAY_NORMAL);
//
//        builder = new NotificationCompat.Builder(context)
//                .setSmallIcon(R.mipmap.ic_stat_av_videocam)
//                .setContentTitle("Video Recording...")
//                .setContentText("Car Monitor")
//                .setOngoing(true);

        pref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragView = inflater.inflate(R.layout.monitor_monitor, container, false);
        ButterKnife.bind(this, fragView);

        fileNums        = pref.getInt(SAVE_VIDEOFILE_NUMS, DEFAULT_MAXFILENUMS);
        maxFileDuration = pref.getInt(SAVE_MAX_FILEDURATION, DEFAULT_MAXDURATION);
        fileIndex       = pref.getInt(SAVE_VIDEOFILE_INDEX, 0);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(recorder.myApp.recService != null) {
                    if (recorder.myApp.recService.monitoring) {
                        startstop_ins.setText("Tap to stop recording");
                        if (recorder.myApp.recService != null && recorder.myApp.recService.monitoring) {
//                            bk_preview.addView(recorder.myApp.recService.preview);
//                            if(recorder.myApp.recService != null && recorder.myApp.recService.monitoring) {
//                                recorder.myApp.recService.loopVideoRecorder.stopRecording(null);
//                                recorder.myApp.recService.monitoring = false;
//                                recorder.myApp.recService.startLoopRecorder();
//                            }
                        }
                    }
                }
            }
        }, 200);
//        SurfaceView preview = new SurfaceView(context.getApplicationContext());
//        bk_preview.addView(preview);
        startstop_ins.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recorder.myApp.recService.monitoring) {
                    recorder.myApp.recService.handler
                            .obtainMessage(RecService.CMD_START_RECORDING).sendToTarget();
                    startstop_ins.setText("Tap to stop recording");
                } else {
                    recorder.myApp.recService.handler
                            .obtainMessage(RecService.CMD_STOP_RECORDING).sendToTarget();
                    startstop_ins.setText("Tap to start recording");
                }
            }
        });

        return fragView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragHeight = fragView.getHeight();
        if (fragHeight != 0 && recorder.myApp.recService != null) {
            recorder.myApp.recService.wmParams.height = fragHeight;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser && bk_preview != null) {
            bk_preview.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

//        if (loopVideoRecorder != null) {
//            loopVideoRecorder.stopRecording(null);
//        }
    }

    @Override
    public void onDestroyView() {

        //int height = fragView.getHeight();
        super.onDestroyView();
        //bk_preview.removeView(recorder.myApp.recService.preview);
//            recorder.myApp.recService.preserveSurfacaView();
        if(bk_preview != null && recorder.myApp.recService.preview != null)
        {

        }
        ButterKnife.unbind(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        realm.close();
//        ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
    }




    ///////////////////////////////////////////////////////////////////////////////
    // interfaces for caller
    public void config(int fileNums, int maxFileDuration) {
        this.fileNums = fileNums;
        this.maxFileDuration = maxFileDuration;

        SharedPreferences.Editor edit = pref.edit();
        edit.putInt(SAVE_VIDEOFILE_NUMS, fileNums);
        edit.putInt(SAVE_MAX_FILEDURATION, maxFileDuration);
        edit.commit();
    }

    public void start() {

    }

    public void stop() {

    }
}
