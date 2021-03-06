package com.DiscordEcho.Listeners;

import com.DiscordEcho.DiscordEcho;
import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.audio.CombinedAudio;
import net.dv8tion.jda.core.audio.UserAudio;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.io.IOException;
import java.util.Arrays;

import static com.DiscordEcho.DiscordEcho.guildSettings;

public class AudioReceiveListener implements AudioReceiveHandler {
    public static final double STARTING_MB = 0.5;
    public static final double PCM_MINS = 2;
    public double AFK_LIMIT = 2;
    public boolean canReceive = true;
    public double volume = 1.0;
    private VoiceChannel voiceChannel;

    public byte[] uncompVoiceData = new byte[(int) (3840 * 50 * 60 * PCM_MINS)]; //3840bytes/array * 50arrays/sec * 60sec = 1 mins
    public int uncompIndex = 0;

    public byte[] compVoiceData = new byte[(int) (1024 * 1024 * STARTING_MB)];    //start with 0.5 MB
    public int compIndex = 0;

    public boolean overwriting = false;

    private int afkTimer;

    public AudioReceiveListener(double volume, VoiceChannel voiceChannel) {
        this.volume = volume;
        this.voiceChannel = voiceChannel;
    }

    @Override
    public boolean canReceiveCombined() {
        return canReceive;
    }

    @Override
    public boolean canReceiveUser() {
        return false;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        if (combinedAudio.getUsers().size() == 0) afkTimer++;
        else afkTimer = 0;

        TextChannel defaultTC = voiceChannel.getGuild().getTextChannelById(guildSettings.get(voiceChannel.getGuild().getId()).defaultTextChannel);
        if (afkTimer >= 50 * 60 * AFK_LIMIT) {   //20ms * 50 * 60 seconds * 2 mins = 2 mins
            System.out.format("AFK detected, leaving '%s' voice channel in %s\n", voiceChannel.getName(), voiceChannel.getGuild().getName());
            DiscordEcho.sendMessage(defaultTC, "No audio for 2 minutes, leaving from AFK detection...");

            voiceChannel.getGuild().getAudioManager().closeAudioConnection();
            DiscordEcho.killAudioHandlers(voiceChannel.getGuild());
            return;
        }

        if (uncompIndex == uncompVoiceData.length / 2 || uncompIndex == uncompVoiceData.length) {
            new Thread(() -> {
                try {
                    if (uncompIndex < uncompVoiceData.length / 2)  //first half
                        addCompVoiceData(DiscordEcho.encodePcmToMp3(Arrays.copyOfRange(uncompVoiceData, 0, uncompVoiceData.length / 2)));
                    else
                        addCompVoiceData(DiscordEcho.encodePcmToMp3(Arrays.copyOfRange(uncompVoiceData, uncompVoiceData.length / 2, uncompVoiceData.length)));
                } catch (IOException e) {
                    e.printStackTrace();
                    DiscordEcho.sendMessage(defaultTC, "Error storing mp3");
                }
            }).start();

            if (uncompIndex == uncompVoiceData.length)
                uncompIndex = 0;
        }

        for (byte b : combinedAudio.getAudioData(volume)) {
            uncompVoiceData[uncompIndex++] = b;
        }
    }

    public byte[] getVoiceData() {
        canReceive = false;

        //flush remaining audio
        byte[] remaining = new byte[uncompIndex];

        int start = uncompIndex < uncompVoiceData.length / 2 ? 0 : uncompVoiceData.length / 2;

        for (int i = 0; i < uncompIndex - start; i++) {
            remaining[i] = uncompVoiceData[start + i];
        }

        try {
            addCompVoiceData(DiscordEcho.encodePcmToMp3(remaining));
        } catch (IOException e) {
            e.printStackTrace();
            TextChannel defaultTC = voiceChannel.getGuild().getTextChannelById(guildSettings.get(voiceChannel.getGuild().getId()).defaultTextChannel);
            DiscordEcho.sendMessage(defaultTC, "Error storing mp3");
        }

        byte[] orderedVoiceData;
        if (overwriting) {
            orderedVoiceData = new byte[compVoiceData.length];
        } else {
            orderedVoiceData = new byte[compIndex + 1];
            compIndex = 0;
        }

        for (int i = 0; i < orderedVoiceData.length; i++) {
            if (compIndex + i < orderedVoiceData.length)
                orderedVoiceData[i] = compVoiceData[compIndex + i];
            else
                orderedVoiceData[i] = compVoiceData[compIndex + i - orderedVoiceData.length];
        }

        canReceive = true;

        return orderedVoiceData;
    }


    public void addCompVoiceData(byte[] compressed) {
        for (byte b : compressed) {
            if (compIndex >= compVoiceData.length && compVoiceData.length != 1024 * 1024 * DiscordEcho.serverSettings.getRecordingCapInMb()) {    //cap at 16MB

                byte[] temp = new byte[compVoiceData.length * 2];
                for (int i = 0; i < compVoiceData.length; i++)
                    temp[i] = compVoiceData[i];

                compVoiceData = temp;

            } else if (compIndex >= compVoiceData.length && compVoiceData.length == 1024 * 1024 * DiscordEcho.serverSettings.getRecordingCapInMb()) {
                compIndex = 0;

                if (!overwriting) {
                    overwriting = true;
                    System.out.format("Hit compressed storage cap in %s on %s", voiceChannel.getName(), voiceChannel.getGuild().getName());
                }
            }


            compVoiceData[compIndex++] = b;
        }
    }

    public byte[] getUncompVoice(int time) {
        canReceive = false;

        if (time > PCM_MINS * 60 * 2) {     //2 mins
            time = (int) (PCM_MINS * 60 * 2);
        }
        int requestSize = 3840 * 50 * time;
        byte[] voiceData = new byte[requestSize];

        for (int i = 0; i < voiceData.length; i++) {
            if (uncompIndex + i < voiceData.length)
                voiceData[i] = uncompVoiceData[uncompIndex + i];
            else
                voiceData[i] = uncompVoiceData[uncompIndex + i - voiceData.length];
        }
        
        canReceive = true;
        return voiceData;
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {
    }
}