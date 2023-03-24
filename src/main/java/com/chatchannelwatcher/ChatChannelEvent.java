package com.chatchannelwatcher;

import net.runelite.http.api.RuneLiteAPI;

public class ChatChannelEvent {
    public String displayName;
    public boolean joining;
    public int timeStamp;
    public boolean onList;

    public ChatChannelEvent(String displayName, boolean joining, int timeStamp, boolean onList) {
        this.displayName = displayName;
        this.joining = joining;
        this.timeStamp = timeStamp;
        this.onList = onList;
    }

    public String getJson() {
        return RuneLiteAPI.GSON.toJson(this);
    }
}
