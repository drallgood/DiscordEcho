package com.DiscordEcho.Configuration;

public class ServerSettings {
    private String hostUrl = "http://DiscordEcho.com/";

    private int fileTTLinHours = 1;

    private String recordingStoragePath = "recordings/";

    public String getHostUrl() {
        return hostUrl;
    }

    public int getFileTTLinHours() {
        return fileTTLinHours;
    }

    public String getRecordingStoragePath() {
        return recordingStoragePath;
    }
}
