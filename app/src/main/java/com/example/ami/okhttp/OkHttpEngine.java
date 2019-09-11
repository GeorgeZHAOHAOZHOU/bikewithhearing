package com.example.ami.okhttp;

import android.content.Context;
import android.os.Handler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpEngine {
    private static volatile OkHttpEngine mInstance;
    private OkHttpClient mOkHttpClient;
    private Handler mHander;

    public static OkHttpEngine getInstance(Context context)
    {
        if (mInstance == null)
        {
            synchronized (OkHttpEngine.class)
            {
                if (mInstance == null)
                {
                    mInstance = new OkHttpEngine(context);
                }
            }
        }
        return  mInstance;
    }

    private OkHttpEngine(Context context)
    {
        File sdcache = context.getExternalCacheDir();
        int cacheSize = 10 * 1024 * 1024;
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS);
        mOkHttpClient = builder.build();
        mHander = new Handler();

    }

    /**
    * 异步GET请求
    * @param url
    * @param callback
    * */
    public void get(String url, ResultCallBack callback)
    {
        final Request request = new Request
                .Builder()
                .url(url)
                .build();
        Call call =  mOkHttpClient.newCall(request);
        dealResult(call, callback);
    }


    /**
     * 异步POST请求
     * @param url
     * @param map
     * @param callBack
     * */
    public void post(String url, Map<String, String> map, ResultCallBack callBack)
    {
        FormBody.Builder builder = new FormBody.Builder();

        for(Map.Entry<String, String> entry : map.entrySet())
        {
            builder.add(entry.getKey(),entry.getValue());
        }
        RequestBody formBody = builder.build();

        final Request request = new Request
                .Builder()
                .url(url)
                .post(formBody)
                .build();
        Call call =  mOkHttpClient.newCall(request);
        dealResult(call, callBack);
    }


    /**
     * 上传文件
     * @param uploadFilePath
     * @param url
     * @param callBack
     * */
    public void upload(String uploadFilePath, String url, ResultCallBack callBack)
    {


        File f  = new File(uploadFilePath);
        String content_type  = "text/x-markdown; charset=utf-8";
        RequestBody file_body = RequestBody.create(MediaType.parse(content_type),f);
        RequestBody request_body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("type",content_type)
                .addFormDataPart("uploaded_file",uploadFilePath.substring(uploadFilePath.lastIndexOf("/")+1), file_body)
                .build();
        final Request request = new Request.Builder()
                .url(url)
                .post(request_body)
                .build();
        Call call =  mOkHttpClient.newCall(request);
        dealResult(call, callBack);
    }

    public void download(String url, String destDir, String fileName, ResultCallBack callBack)
    {
        final File f = new File(destDir, fileName);
        final Request request = new Request.Builder()
                .url(url)
                .build();

        Call call = mOkHttpClient.newCall(request);
        dealResultFile(call,callBack,f);
    }



    private void dealResult(Call call, final ResultCallBack callBack)
    {
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendFailedCallback(call.request(),e,callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                sendSuccessCallback(response.code(),response.body().string(),callBack);
            }
        });
    }

    private void dealResultFile(Call call, final ResultCallBack callBack, final File file)
    {
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendFailedCallback(call.request(),e,callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                sendSuccessFileCallBack(response.code(),response.body().byteStream(), callBack, file);
            }
        });
    }

    private void sendSuccessCallback(final int code,final String str, final ResultCallBack callBack)
    {
            mHander.post(new Runnable() {
                @Override
                public void run() {
                    if (callBack != null)
                    {
                        try
                        {
                            callBack.onResponse(code,str);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            });
    }

    private void sendSuccessFileCallBack(final int code, final InputStream inputStream, final ResultCallBack callBack, final File file)
    {

            try {
                FileOutputStream fos = null;
                if (file != null) {
                    fos = new FileOutputStream(file);
                    byte[] buffer = new byte[4096];
                    int len = 0;
                    while ((len = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    fos.close();
                }
                mHander.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callBack != null) {
                            try {
                                callBack.onResponse(code, "success");
                            } catch (IOException e) {

                            }
                        }
                    }
                });
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }



    }

    private void sendFailedCallback(final Request request, final Exception e, final ResultCallBack callBack)
    {
            mHander.post(new Runnable() {
                @Override
                public void run() {
                    if (callBack != null)
                    {
                        callBack.onError(request,e);
                    }
                }
            });
    }


}
