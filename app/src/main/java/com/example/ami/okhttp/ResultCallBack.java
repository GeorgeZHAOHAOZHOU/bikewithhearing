package com.example.ami.okhttp;

import java.io.IOException;
import okhttp3.Request;

public interface ResultCallBack {

    void onError(Request request, Exception e);
    void onResponse(int code, String str) throws IOException;

}
