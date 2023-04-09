package com.chatchannelwatcher;

import com.google.gson.JsonArray;
import okhttp3.*;

import java.io.IOException;

public class ChatChannelWatcherAPI {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static void postEvent(OkHttpClient httpClient, String url, String token, ChatChannelEvent event)
    {
        Request postRequest = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer: " + token)
                .post(RequestBody.create(JSON, event.getJson()))
                .build();

        httpClient.newCall(postRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }
        });
    }

    public static String getPlayerList(OkHttpClient httpClient, String url, String token) {
        Request getRequest = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer: " + token)
                .get()
                .build();

        Call call = httpClient.newCall(getRequest);
        try {
            Response response = call.execute();
            return response.body().string();
        }
        catch (IOException io) {
            return "";
        }
    }
}
