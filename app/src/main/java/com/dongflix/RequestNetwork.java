package com.dongflix;

import android.app.Activity;
import java.util.HashMap;

public class RequestNetwork {
    private HashMap<String, Object> params = new HashMap<>();
    private HashMap<String, Object> headers = new HashMap<>();
    private Activity activity;
    private int requestType = 0;

    public RequestNetwork(Activity activity) {
        this.activity = activity;
    }

    public void startRequestNetwork(String method, String url, String tag, RequestListener requestListener) {
        // Menggunakan OkHttp untuk menangani request network
        okhttp3.OkHttpClient client = NetworkClient.getInstance();
        okhttp3.Request.Builder reqBuilder = new okhttp3.Request.Builder().url(url);
        
        if (method.equals("GET")) {
            reqBuilder.get();
        }

        client.newCall(reqBuilder.build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                activity.runOnUiThread(() -> requestListener.onErrorResponse(tag, e.getMessage()));
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                String body = response.body() != null ? response.body().string() : "";
                activity.runOnUiThread(() -> requestListener.onResponse(tag, body, new HashMap<>()));
            }
        });
    }

    public interface RequestListener {
        void onResponse(String tag, String response, HashMap<String, Object> responseHeaders);
        void onErrorResponse(String tag, String message);
    }
}
