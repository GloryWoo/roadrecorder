package harman.com.cloud.net;

import android.content.Context;

import org.json.JSONObject;

import harman.com.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by jishao on 9/29/15.
 */
public class HttpRequest {
    private static final String TAG = "HttpRequest";
    private static final String dftFile = "data";
    public interface Method {
        int GET = 1;
        int POST = 2;
    }

    public static byte[] convertParamsToByteArray(Map<String, String> params) {
        if (params != null && params.size() > 0) {
            StringBuffer sb = new StringBuffer();

            boolean isFirst = true;
            Iterator it = params.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();
                if(!isFirst) {
                    sb.append("&");
                } else {
                    isFirst = false;
                }
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(entry.getValue());
            }
            return sb.toString().getBytes();
        }

        return null;
    }


    private int method;
    private String url;
    private final HttpResponse.Listener listener;
    private final Map<String, String> headers;
    private final byte[] body;
    public  String storePath;
    public  int offset;
    private Context context;

    public HttpRequest(Context ctx_, int method_, String url_, Map<String, String> headers_, byte[] body_,
                       HttpResponse.Listener listener_) {
        context = ctx_;
        method = method_;
        url = url_;
        headers = headers_;
        listener = listener_;

        body = body_;
    }

    private String getDefaultContentType() {
        return "application/x-www-form-urlencoded; charset=UTF-8";
    }

    private String getRequestMethod() {
        if (method == Method.GET)
            return "GET";
        if (method == Method.POST)
            return "POST";

        return null;
    }


    public void process() {
        boolean hasException = false;
        String exceptionMessage = null;

        HttpResponse resp = new HttpResponse();
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        try {
            URL httpUrl = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) httpUrl.openConnection();
            if(url.startsWith("https://")) {
                KeyStore keystore = KeyStore.getInstance("BKS");
                InputStream istream = context.getResources().openRawResource(R.raw.client);
                keystore.load(istream, "harman123".toCharArray());
                istream.close();

                String algorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                tmf.init(keystore);

                SSLContext sslcontext = SSLContext.getInstance("TLS");
                sslcontext.init(null, tmf.getTrustManagers(), null);

                HttpsURLConnection httpsconn = (HttpsURLConnection) conn;
                httpsconn.setSSLSocketFactory(sslcontext.getSocketFactory());
            }
            conn.setReadTimeout(10 * 1000);
            conn.setConnectTimeout(10 * 1000);
            conn.setDoInput(true);
            conn.setDefaultUseCaches(false);
            conn.setUseCaches(false);

            // set method
            if (getRequestMethod() != null)
                conn.setRequestMethod(getRequestMethod());

            // set headers
            if(headers != null && headers.size() > 0) {
                Iterator it = headers.entrySet().iterator();
                while(it.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            if(!headers.containsKey("Content-Type")) {
                conn.setRequestProperty("Content-Type", getDefaultContentType());
            }

            //conn.setRequestProperty("Connection", "close");
            //conn.connect();
            // set body
            if(body != null && method == Method.POST) {
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(body);
                os.flush();
                os.close();
            }

            // get response
            int responseCode = conn.getResponseCode();
            resp.code = responseCode;

            InputStream is = null;
            try {
                is = conn.getInputStream();
            } catch (IOException ioe) {
                if(responseCode != 200) {
                    is = conn.getErrorStream();
                }
            }
            String fileNm = conn.getHeaderField("fileName");

            RandomAccessFile rmfile;
            if(storePath == null || fileNm == null || fileNm.equals("")) {
                byte[] buffer = new byte[2048];
                while (true) {
                    int n = is.read(buffer, 0, 2048);
                    if (n == -1)
                        break;

                    byteArray.write(buffer, 0, n);
                }
                resp.body = byteArray;
            }
            else{
                String contentLenStr = conn.getHeaderField("Content-Length");
                int contentLen = 0;
                if(contentLenStr != null && !contentLenStr.equals(""))
                    contentLen = Integer.parseInt(contentLenStr);

                if(fileNm.contains(".gzip"))
                    fileNm = fileNm.substring(0, fileNm.length()-5);
                GZIPInputStream gzi = new GZIPInputStream(is);
                rmfile = new RandomAccessFile(storePath+"/"+fileNm, "rw");
                rmfile.seek(rmfile.length());
                byte[] buf = new byte[4096];
                int size = 0;
                int writeLen = offset;
                int loopi = 0;
                while ((size = gzi.read(buf, 0, 4096)) != -1) {
                    rmfile.write(buf, 0, size);
                    writeLen += size;
//                    if(++loopi % 100 == 0)
//                        listener.onProcessing(writeLen);
                }
                rmfile.close();
                gzi.close();
                JSONObject json = new JSONObject();
                json.put("filename", fileNm);
                json.put("contentlen", contentLen);
                json.put("writelen", writeLen);
                byteArray.write(json.toString().getBytes());
                resp.body = byteArray;


            }
            is.close();


            conn.disconnect();
        } catch (Exception e) {
            hasException = true;
            exceptionMessage = "Excpetion:" + e.getClass().getName() + "," + "Message:" + e.getMessage();
        }
        finally {
            if(hasException) {
                resp.code = -1;
                try {
                    byteArray.write(exceptionMessage.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                resp.body = byteArray;
            }

            listener.onResponse(resp);
        }
    }



}
