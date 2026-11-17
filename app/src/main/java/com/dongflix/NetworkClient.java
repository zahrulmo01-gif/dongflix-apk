package com.dongflix;

import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

public class NetworkClient {
    private static OkHttpClient client;

    public static OkHttpClient getInstance() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }
}
