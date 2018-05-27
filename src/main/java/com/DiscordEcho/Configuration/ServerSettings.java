package com.DiscordEcho.Configuration;

public class ServerSettings {
    private String hostUrl = "http://DiscordEcho.com/";

    private int fileTTLinHours = 1;

    private String recordingStoragePath = "recordings/";

    private int recordingCapInMb = 16;

    public String getHostUrl() {
        return hostUrl;
    }

    public int getFileTTLinHours() {
        return fileTTLinHours;
    }

    public String getRecordingStoragePath() {
        return recordingStoragePath;
    }

    public int getRecordingCapInMb() {
        return recordingCapInMb;
    }
}
