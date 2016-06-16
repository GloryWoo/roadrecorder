package harman.com;

import android.app.Application;

/**
 * Created by ZheWu on 2015/12/11.
 */
public class MyApplication extends Application {
    public static MyApplication Instance;

    public Recorder recorder;
    public RecService recService;
    public void onCreate() {
        super.onCreate();
        Instance=this;
        //appVersion=UIHelper.getAppVersionName(Instance);


//        MyUncaughtExceptionHandler.SetOnUncaughtExceptionHandler();

        //copyLog2DownloadPath();
    }
}
