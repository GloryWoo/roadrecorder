package harman.com.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import harman.com.cloud.net.HttpMidStat;
import harman.com.cloud.net.HttpRequest;
import harman.com.cloud.net.HttpResponse;
import harman.com.cloud.net.HttpWorkQueue;
import harman.com.cloud.requests.UserRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by jishao on 5/5/16.
 */
public class StateMachine {
    private Context context;
    private String baseUrl;

    private int state;
    private final static int STATE_IDLE = 0x00000000;
    private final static int STATE_LOGIN = 0x00000001 << 0;
    private final static int STATE_LOGINING = 0x00000001 << 1;

    private LinkedList<UserRequest> holdonQueue;
    private BlockingQueue<UserRequest> waitingQueue;
    private boolean isWorking = false;
    private Thread workingThread;

    private HttpWorkQueue httpWorkingQueue;

    private String account;
    private String password;
    private String token;
    private String uid;

    private Handler uiHandler;

    public StateMachine(Context ctx, String url) {
        context = ctx;
        baseUrl = url;

        uiHandler = new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
    }

    public void handleMidStat(final HttpMidStat.Handler handler, final HttpMidStat stat) {
        if(uiHandler != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    handler.onMidStat(stat);
                }
            });
        }
    }

    public void handleResponse(final Response.Handler handler, final Response resp) {
        if(uiHandler != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        handler.onResult(resp);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public synchronized void handleRequest(UserRequest r)  {
        if(!isWorking) {
            // have not started the working thread
            return;
        }

        try {
            waitingQueue.put(r);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void start() {
        if(isWorking) {
            // prevent duplicated start call
            return;
        }

        // run the http thread
        httpWorkingQueue = new HttpWorkQueue(context);
        httpWorkingQueue.start();

        // create a working queue thread
        isWorking = true;
        holdonQueue = new LinkedList<UserRequest>();
        waitingQueue = new LinkedBlockingQueue<UserRequest>();
        workingThread = new Thread(new Runnable() {

            public void run() {
                while(isWorking) {
                    try {
                        UserRequest r = waitingQueue.take();
                        onRequest(r);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        workingThread.start();

        setStateIdle();
    }

    public synchronized void stop() {
        if(!isWorking) {
            return;
        }
        isWorking = false;

        if(workingThread != null) {
            workingThread.interrupt();
            try {
                workingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(httpWorkingQueue != null) {
            httpWorkingQueue.clear();
            httpWorkingQueue.quit();
            httpWorkingQueue = null;
        }

        waitingQueue.clear();
        holdonQueue.clear();

        setStateIdle();
    }

    private synchronized void onRequest(final UserRequest r) {
        if(r.requireLogined()) {
            if(isStateLogined()) {
                //continue
            } else if(isStateLogining()) {
                holdonQueue.add(r);
                return;
            } else {
                // generate a fake http response
                HttpResponse httpResp = new HttpResponse();
                httpResp.code = 401;
                JSONObject jo = new JSONObject();
                try {
                    jo.put("error", "Not Logined yet");
                    ByteArrayOutputStream ba = new ByteArrayOutputStream();
                    ba.write(jo.toString().getBytes());
                    httpResp.body = ba;
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                r.onResponse(httpResp);
                return;
            }
        } else {
            // continue
        }

        if(r.isLogin()) {
            setStateLogining(true);
        }

        HttpRequest hr = r.getHttpRequest(baseUrl);
        if(hr == null)
            return;
        httpWorkingQueue.add(hr);
    }


    public synchronized  void loginFailed() {
        setStateLogining(false);

        // move the hold-on requests into waiting queue
        for(UserRequest r : holdonQueue) {
            try {
                waitingQueue.put(r);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        holdonQueue.clear();
    }

    public synchronized void logined(String account_, String password_, String token_, String id_) {
        setStateLogining(false);

        if(isStateLogined()) {
            logouted();
        }

        account = account_;
        password = password_;
        token = token_;
        uid = id_;
        setStateLogined(true);

        // move the hold-on requests into waiting queue
        for(UserRequest r : holdonQueue) {
            try {
                waitingQueue.put(r);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        holdonQueue.clear();
    }

    public synchronized  void logouted() {
        if(!isStateLogined())
            return;

        waitingQueue.clear();
        httpWorkingQueue.clear();
        setStateIdle();
    }

    private synchronized  void setStateIdle() {
        state = STATE_IDLE;
    }

    private synchronized  void setStateLogined(boolean logined) {
        if(logined) {
            state |= STATE_LOGIN;
        } else {
            state &= ~STATE_LOGIN;
        }
    }

    private synchronized void setStateLogining(boolean logining) {
        if(logining) {
            state |= STATE_LOGINING;
        } else {
            state &= ~STATE_LOGINING;
        }
    }

    private synchronized boolean isStateLogined() {
        return (state & STATE_LOGIN) != 0;
    }

    private synchronized boolean isStateLogining() {
        return (state & STATE_LOGINING) != 0;
    }

    public synchronized String getVersion() {
        return "1.0";
    }

    public synchronized Context getContext() {
        return context;
    }

    public synchronized String getToken() {
        return token;
    }
}
