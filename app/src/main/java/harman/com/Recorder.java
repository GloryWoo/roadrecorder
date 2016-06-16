package harman.com;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
//import android.support.v7.app.ActionBarActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Map;

import harman.com.monitor.fragments.*;
import harman.com.toolbarslidingtab.SlidingTabLayout;
import harman.com.util.adapter.MyFragPagerAdapter;
import harman.com.util.proxy.HashMapProxy;
import harman.com.util.widget.CustomViewPager;


public class Recorder extends ActionBarActivity implements ViewPager.OnPageChangeListener {
    static String Tag = "Recorder";
    private ArrayList<Fragment> fragments;
    CustomViewPager viewPager;
    SlidingTabLayout slidingTabLayout;
    MyFragPagerAdapter vpAdapter;
    Toolbar toolbar;
    public static int mWidth, mHeight, statusBarHeight,realHeight, mDensityInt;
    static float scale, mDensity;
    boolean isActive;
    public FragRecord monitor;
    public FragGallery fragGallery;
    public FragNetGallery    fragNetGallery;
    public FragSetting fragSetting;
    ArrayList<Map<String, Object>> adaptParam;
    boolean serviceConn;
//    public RecService.MyBinder localbinder;
    SurfaceView preview;
    public MyApplication myApp;
    MenuItem recordBtn;
    public SharedPreferences pref;
    ServiceConnection connection = new ServiceConnection(){
        public void onServiceDisconnected(ComponentName name){
//			if(panel_s != null)
//				panel_s.setPanelReadData("/", "/", "/","/");
            serviceConn = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
//            localbinder = (RecService.MyBinder)service;
//            serviceConn = true;
//            preview = localbinder.getSurfaceView();
//            if(monitor != null && monitor.bk_preview != null && preview != null
//                    && monitor.bk_preview.getChildCount() == 0){
//                monitor.bk_preview.addView(preview);
//                monitor.bk_preview.setVisibility(View.VISIBLE);
//            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myApp = (MyApplication) getApplication();
        myApp.recorder = this;
        setContentView(R.layout.roadrecorder);
        viewPager = (CustomViewPager) findViewById(R.id.custom_vp);
        fragments = new ArrayList<Fragment>();
        fragments.add(monitor = new FragRecord(this));
        fragments.add(fragGallery = new FragGallery());
        fragments.add(fragNetGallery = new FragNetGallery());
        fragments.add(fragSetting = new FragSetting());
        adaptParam = new ArrayList<Map<String, Object>>();
        adaptParam.add(new HashMapProxy<String, Object>().putObject("title", "Record"));
        adaptParam.add(new HashMapProxy<String, Object>().putObject("title", "Gallery"));
        adaptParam.add(new HashMapProxy<String, Object>().putObject("title", "Down"));
        adaptParam.add(new HashMapProxy<String, Object>().putObject("title", "Setting"));
        vpAdapter = new MyFragPagerAdapter(getSupportFragmentManager(), fragments, adaptParam);
        viewPager.setOffscreenPageLimit(fragments.size());
        viewPager.setAdapter(vpAdapter);
        viewPager.setScanScroll(false);

        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        LocationManager locationManager = (LocationManager)getSystemService(this.LOCATION_SERVICE);
        WindowManager wm = (WindowManager)getSystemService(this.WINDOW_SERVICE);
        mWidth = wm.getDefaultDisplay().getWidth();
        mHeight = wm.getDefaultDisplay().getHeight();
        statusBarHeight = getStatusBarHeight();
        realHeight = mHeight-statusBarHeight;
        scale = getResources().getDisplayMetrics().density/2;
        mDensity = getResources().getDisplayMetrics().density;
        mDensityInt = (int) mDensity;
        //toolbar
        toolbar = (Toolbar) findViewById(R.id.id_toolbar);
//        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.mipmap.recording);
        toolbar.inflateMenu(R.menu.base_toolbar_menu);
        toolbar.setOnMenuItemClickListener(
                new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_record:
                                if (!myApp.recService.monitoring) {
                                    myApp.recService.handler
                                            .obtainMessage(RecService.CMD_START_RECORDING).sendToTarget();
                                    monitor.startstop_ins.setText("Tap to stop recording");
                                } else {
                                    myApp.recService.handler
                                            .obtainMessage(RecService.CMD_STOP_RECORDING).sendToTarget();
                                    monitor.startstop_ins.setText("Tap to start recording");
                                }
                                break;
                            default:
                                break;
                        }
                        return false;
                    }
                }
        );
//        recordBtn = (MenuItem) findViewById(R.id.action_record);
        recordBtn = toolbar.getMenu().getItem(0);
        slidingTabLayout = (SlidingTabLayout) findViewById(R.id.custom_toolbar);
        slidingTabLayout.setCustomTabView(R.layout.toolbaritem, 0, R.id.toolbar_txt, R.id.toolbar_under);
        slidingTabLayout.setViewPager(viewPager, mWidth);

        slidingTabLayout.setOnPageChangeListener(this);

        Intent bindintent = new Intent(this, RecService.class);
        if(myApp.recService == null)
            startService(bindintent);

        if (myApp.recService != null && myApp.recService.monitoring) {
            myApp.recService.showHidePreview(true);
        }


    }

    public void setMenuItemStat(boolean recording){
        if(recording)
            recordBtn.setIcon(R.mipmap.record_icon_on);
        else
            recordBtn.setIcon(R.mipmap.record_icon_off);
    }

    public static int getStatusBarHeight() {
        return Resources.getSystem().getDimensionPixelSize(
                Resources.getSystem().getIdentifier("status_bar_height",
                        "dimen", "android"));
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
//        unbindService(connection);
        if(myApp != null) {
            myApp.recorder = null;
            if(myApp.recService != null && myApp.recService.monitoring) {
                myApp.recService.showHidePreview(false);
//                myApp.recService.neadAutoResume = true;
//                myApp.recService.loopVideoRecorder.stopRecording(null);
//                myApp.recService.monitoring = false;
//                myApp.recService.startLoopRecorder();
//                myApp.recService.preview = null;
//                myApp.recService.stopPreview();
            }

        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if(position == 0){
            if(myApp.recService != null){
                myApp.recService.showHidePreview(true);
            }
        }
        else{
            if(myApp.recService != null){
                myApp.recService.showHidePreview(false);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
