package harman.com.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

import harman.com.RecService;
import harman.com.Util;
import harman.com.cloud.net.HttpMidStat;
import harman.com.cloud.requests.DownRecordRequest;
import harman.com.cloud.requests.GetRandomAccountRequest;
import harman.com.cloud.requests.GetRecordIdRequest;
import harman.com.cloud.requests.IntentRecognizeRequest;
import harman.com.cloud.requests.LoginRequest;
import harman.com.cloud.requests.UploadRecordRequest;

/**
 * Created by jishao on 5/5/16.
 */
public class Cloud {
    String TAG = "Cloud";
    private Handler uiHandler;
    private StateMachine stateMachine;
    public Cloud(Context ctx, String url) {
        if(url == null || !url.startsWith("http"))
            url = "http://192.168.1.52:8081";
        stateMachine = new StateMachine(ctx, url);
        uiHandler = new Handler(ctx.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
    }

    public void start() {
        stateMachine.start();
    }

    public void stop() {
        stateMachine.stop();
    }

    public void login(String account, String password, Response.Handler h) {
        LoginRequest r = new LoginRequest(account, password, h, stateMachine);
        stateMachine.handleRequest(r);
    }

    public void requestAccount(String deviceId, Response.Handler h) {
        GetRandomAccountRequest r = new GetRandomAccountRequest(deviceId, h, stateMachine);
        stateMachine.handleRequest(r);
    }

    public void intentRecognize(String speech, Response.Handler h) {
        IntentRecognizeRequest r = new IntentRecognizeRequest(speech, h, stateMachine);
        stateMachine.handleRequest(r);
    }

    //uploadIdx start from 1
    public void uploadRecord(File file, String uploadname, int blockNumber, int uploadIdx, Response.Handler h){
        byte[] zipdata = null;
        ByteArrayOutputStream buff = readbuff(file, RecService.BlockSize, blockNumber, uploadIdx);
        byte[] rawData = buff.toByteArray();
        UploadRecordRequest r = new UploadRecordRequest(rawData, uploadname, (int) blockNumber, uploadIdx, h, stateMachine);
        stateMachine.handleRequest(r);

    }

    public void getRecordId(long start, long end, Response.Handler h){
        GetRecordIdRequest r = new GetRecordIdRequest(start, end, h, stateMachine);
        stateMachine.handleRequest(r);
    }

    public void getFileSocket(String path, String recordid, int offset, int len, Response.Handler h){
        DownRecordRequest r = new DownRecordRequest(path, recordid, offset, len, h, stateMachine);
        stateMachine.handleRequest(r);
    }

    public static ByteArrayOutputStream readbuff(File f,int blocksize,int blockNumber,int blockIndex){
        byte b[] = new byte[1024];//暂存容器

        int buffsz = blocksize;
        int fsz = (int) f.length();
        if(blockIndex == blockNumber){
            buffsz = fsz - blocksize * (blockIndex-1);
        }
        ByteArrayOutputStream  buff = new ByteArrayOutputStream();
        int n = 0;
        int readLength = 0;// 记录已读字节数
        try {
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            if (blockIndex < blockNumber) {//
                raf.seek(blocksize * (blockIndex-1));// 跳过前[块数*固定大小 ]个字节
                while (readLength <= blocksize - 1024) {// 大部分字节在这里读取
                    n = raf.read(b, 0, 1024);
                    readLength += n;
                    buff.write(b, 0, n);
                }
                if (readLength <= blocksize) {// 余下的不足 1024 个字节在这里读取
                    n = raf.read(b, 0, (int) (blocksize - readLength));
                    buff.write(b, 0, n);
                }
            } else {// 最后一块
                raf.seek(blocksize * (blockIndex-1));// 跳过前[块数*固定大小 ]个字节
                while ((n = raf.read(b, 0, 1024)) != -1) {
                    readLength += n;
                    buff.write(b, 0, n);
                }
            }

            buff.close();

            return buff;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
