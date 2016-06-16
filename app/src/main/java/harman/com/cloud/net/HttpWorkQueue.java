package harman.com.cloud.net;

import android.content.Context;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jishao on 9/28/15.
 */
public class HttpWorkQueue extends Thread {
    private static final String TAG = "HttpWorkQueue";

    private boolean working ;

    private BlockingQueue<HttpRequest> requestQueue;
    private Lock lockRequestQueue;
    private Context context;

    public HttpWorkQueue(Context ctx) {
        context = ctx;

        requestQueue = new LinkedBlockingQueue<HttpRequest>();
        lockRequestQueue = new ReentrantLock();

        working = false;
    }

    public void add(HttpRequest r) {
        lockRequestQueue.lock();
        if(working)
            requestQueue.add(r);
        lockRequestQueue.unlock();
    }

    public void clear() {
        lockRequestQueue.lock();
        if(working)
            requestQueue.clear();
        lockRequestQueue.unlock();
    }

    public void run() {
        working = true;
        while(working) {
            try {
                HttpRequest r = requestQueue.take();
                r.process();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void quit() {
        if(!working)
            return;

        working = false;

        lockRequestQueue.lock();
        if(requestQueue != null)
            requestQueue.clear();
        lockRequestQueue.unlock();

        this.interrupt();
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
