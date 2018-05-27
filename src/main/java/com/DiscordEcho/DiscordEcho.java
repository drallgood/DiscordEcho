package com.DiscordEcho;

import com.DiscordEcho.Commands.*;
import com.DiscordEcho.Commands.Audio.ClipCommand;
import com.DiscordEcho.Commands.Audio.EchoCommand;
import com.DiscordEcho.Commands.Audio.MessageInABottleCommand;
import com.DiscordEcho.Commands.Audio.SaveCommand;
import com.DiscordEcho.Commands.Misc.HelpCommand;
import com.DiscordEcho.Commands.Misc.JoinCommand;
import com.DiscordEcho.Commands.Misc.LeaveCommand;
import com.DiscordEcho.Commands.Settings.*;
import com.DiscordEcho.Configuration.GuildSettings;
import com.DiscordEcho.Configuration.ServerSettings;
import com.DiscordEcho.Listeners.AudioReceiveListener;
import com.DiscordEcho.Listeners.AudioSendListener;
import com.DiscordEcho.Listeners.EventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.entities.*;
import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.Lame;
import net.sourceforge.lame.mp3.MPEGMode;

import javax.security.auth.login.LoginException;
import javax.sound.sampled.AudioFormat;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;

import static java.lang.Thread.sleep;

public class DiscordEcho {
    //contains the id of every guild that we are connected to and their corresponding GuildSettings object
    public static HashMap<String, GuildSettings> guildSettings = new HashMap<>();
    public static ServerSettings serverSettings = new ServerSettings();

    public static void main(String[] args) {
        try (FileReader serverSettingsFileReader = new FileReader("conf/server.json")) {
            Type serverSettingsType = new TypeToken<ServerSettings>() {
            }.getType();
            Gson gson = new Gson();
            DiscordEcho.serverSettings = gson.fromJson(serverSettingsFileReader, serverSettingsType);
            System.out.format("HostURL: %s\n", DiscordEcho.serverSettings.getHostUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //read the bot's token from a file name "token" in the main directory
        try (FileReader fr = new FileReader("conf/shark_token"); BufferedReader br = new BufferedReader(fr)) {

            String token = br.readLine();

            //create bot instance
            JDA api = new JDABuilder(AccountType.BOT)
                    .setToken(token)
                    .addEventListener(new EventListener())
                    .buildBlocking();
        } catch (LoginException e) {
            //If anything goes wrong in terms of authentication, this is the exception that will represent it
            e.printStackTrace();
        } catch (InterruptedException e) {
            //Due to the fact that buildBlocking is a blocking method, one which waits until JDA is fully loaded,
            // the waiting can be interrupted. This is the exception that would fire in that situation.
            //As a note: in this extremely simplified example this will never occur. In fact, this will never occur unless
            // you use buildBlocking in a thread that has the possibility of being interrupted (async thread usage and interrupts)
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //register commands and their aliases
        CommandHandler.commands.put("help", new

                HelpCommand());

        CommandHandler.commands.put("join", new

                JoinCommand());
        CommandHandler.commands.put("leave", new

                LeaveCommand());

        CommandHandler.commands.put("save", new

                SaveCommand());
        CommandHandler.commands.put("clip", new

                ClipCommand());
        CommandHandler.commands.put("echo", new

                EchoCommand());
        CommandHandler.commands.put("miab", new

                MessageInABottleCommand());

        CommandHandler.commands.put("autojoin", new

                AutoJoinCommand());
        CommandHandler.commands.put("autoleave", new

                AutoLeaveCommand());

        CommandHandler.commands.put("prefix", new

                PrefixCommand());
        CommandHandler.commands.put("alias", new

                AliasCommand());
        CommandHandler.commands.put("removealias", new

                RemoveAliasCommand());
        CommandHandler.commands.put("volume", new

                VolumeCommand());
        CommandHandler.commands.put("autosave", new

                AutoSaveCommand());
        CommandHandler.commands.put("savelocation", new

                SaveLocationCommand());
        CommandHandler.commands.put("alerts", new

                AlertsCommand());

    }


    //UTILITY FUNCTIONS

    //find the biggest voice channel that surpases the server's autojoin minimum
    public static VoiceChannel biggestChannel(List<VoiceChannel> vcs) {
        int large = 0;
        VoiceChannel biggest = null;

        for (VoiceChannel v : vcs) {
            //does current interation beat old biggest?
            if (voiceChannelSize(v) > large) {
                GuildSettings settings = guildSettings.get(v.getGuild().getId());

                //we only want servers that beat the autojoin minimum (so we don't have to check later)
                if (voiceChannelSize(v) >= settings.autoJoinSettings.get(v.getId())) {
                    biggest = v;
                    large = voiceChannelSize(v);
                }
            }
        }
        return biggest;
    }

    //returns the effective size of the voice channel (bots don't count)
    public static int voiceChannelSize(VoiceChannel vc) {
        if (vc == null) return 0;

        int i = 0;
        for (Member m : vc.getMembers()) {
            if (!m.getUser().isBot()) i++;
        }
        return i;
    }

    public static void writeToFile(Guild guild) {
        writeToFile(guild, -1, null);
    }

    public static void writeToFile(Guild guild, TextChannel tc) {
        writeToFile(guild, -1, tc);
    }

    public static void writeToFile(Guild guild, int time, TextChannel tc) {
        if (tc == null)
            tc = guild.getTextChannelById(guildSettings.get(guild.getId()).defaultTextChannel);

        AudioReceiveListener ah = (AudioReceiveListener) guild.getAudioManager().getReceiveHandler();
        if (ah == null) {
            sendMessage(tc, "I wasn't recording!");
            return;
        }

        ;
        try {

            File dest = new File(DiscordEcho.serverSettings.getRecordingStoragePath() + getPJSaltString() + ".mp3");
            byte[] voiceData;

            if (time > 0 && time <= ah.PCM_MINS * 60 * 2) {
                voiceData = ah.getUncompVoice(time);
                voiceData = encodePcmToMp3(voiceData);

            } else {
                voiceData = ah.getVoiceData();
            }

            FileOutputStream fos = new FileOutputStream(dest);
            fos.write(voiceData);
            fos.close();

            System.out.format("Saving audio file '%s' from %s on %s of size %f MB\n",
                    dest.getName(), guild.getAudioManager().getConnectedChannel().getName(), guild.getName(), (double) dest.length() / 1024 / 1024);

            if (dest.length() / 1024 / 1024 < 8) {
                final TextChannel channel = tc;
                tc.sendFile(dest, (Message) null).queue(null, (Throwable) -> {
                    sendMessage(guild.getTextChannelById(guildSettings.get(guild.getId()).defaultTextChannel),
                            "I don't have permissions to send files in " + channel.getName() + "!");
                });

                new Thread(() -> {
                    try {
                        sleep(1000 * 20);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }    //20 second life for files set to discord (no need to save)

                    dest.delete();
                    System.out.println("\tDeleting file " + dest.getName() + "...");

                }).start();

            } else {
                sendMessage(tc, DiscordEcho.serverSettings.getHostUrl() + dest.getName());

                new Thread(() -> {
                    try {
                        sleep(1000 * 60 * 60 * DiscordEcho.serverSettings.getFileTTLinHours());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    dest.delete();
                    System.out.println("\tDeleting file " + dest.getName() + "...");

                }).start();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            sendMessage(tc, "Unknown error sending file");
        }
    }

    //write the current state of all server settings to the settings.json file
    public static void writeSettingsJson() {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(DiscordEcho.guildSettings);

            FileWriter fw = new FileWriter("conf/settings.json");
            fw.write(json);
            fw.flush();
            fw.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //sends alert DM to anyone in the given voicechannel who isn't on the blacklist
    public static void alert(VoiceChannel vc) {
        for (Member m : vc.getMembers()) {
            //ignore bots
            if (m.getUser().isBot()) continue;

            //check the guild's blacklist and ignore the user if they are on it
            if (!guildSettings.get(vc.getGuild().getId()).alertBlackList.contains(m.getUser().getId())) {

                //make an embeded alert message to warn the user
                EmbedBuilder embed = new EmbedBuilder();
                embed.setAuthor("Discord Echo", "https://devpost.com/software/discord-recorder", vc.getJDA().getSelfUser().getAvatarUrl());
                embed.setColor(Color.RED);
                embed.setTitle("Your audio is now being recorded in '" + vc.getName() + "' on '" + vc.getGuild().getName() + "'");
                embed.setDescription("Disable this alert with `!alerts off`");
                embed.setThumbnail("http://www.freeiconspng.com/uploads/alert-icon-png-red-alert-round-icon-clip-art-3.png");
                embed.setTimestamp(OffsetDateTime.now());

                //open private channel with the user and send the embeded message
                m.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(embed.build()).queue());

            }
        }
    }

    //generate a random string of 13 length with a namespace of around 2e23
    public static String getPJSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 13) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();

        //check for a collision on the 1/2e23 chance that it matches another salt string (lul)
        File dir = new File(DiscordEcho.serverSettings.getRecordingStoragePath());

        for (File f : dir.listFiles()) {
            if (f.getName().equals(saltStr))
                saltStr = getPJSaltString();
        }

        return saltStr;
    }

    //encode the passed array of PCM (uncompressed) audio to mp3 audio data
    public static byte[] encodePcmToMp3(byte[] pcm) {
        LameEncoder encoder = new LameEncoder(new AudioFormat(48000.0f, 16, 2, true, true), 128, MPEGMode.STEREO, Lame.QUALITY_HIGHEST, false);
        ByteArrayOutputStream mp3 = new ByteArrayOutputStream();
        byte[] buffer = new byte[encoder.getPCMBufferSize()];

        int bytesToTransfer = Math.min(buffer.length, pcm.length);
        int bytesWritten;
        int currentPcmPosition = 0;
        while (0 < (bytesWritten = encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer))) {
            currentPcmPosition += bytesToTransfer;
            bytesToTransfer = Math.min(buffer.length, pcm.length - currentPcmPosition);

            mp3.write(buffer, 0, bytesWritten);
        }

        encoder.close();

        return mp3.toByteArray();
    }

    //kill off the audio handlers and clear their memory for the given guild
    public static void killAudioHandlers(Guild g) {
        AudioReceiveListener ah = (AudioReceiveListener) g.getAudioManager().getReceiveHandler();
        if (ah != null) {
            ah.canReceive = false;
            ah.compVoiceData = null;
            g.getAudioManager().setReceivingHandler(null);
        }

        AudioSendListener sh = (AudioSendListener) g.getAudioManager().getSendingHandler();
        if (sh != null) {
            sh.canProvide = false;
            sh.voiceData = null;
            g.getAudioManager().setSendingHandler(null);
        }

        System.out.println("Destroyed audio handlers for " + g.getName());
        System.gc();
    }

    //general purpose function that sends a message to the given text channel and handles errors
    public static void sendMessage(TextChannel tc, String message) {
        tc.sendMessage("\u200B" + message).queue(null, (Throwable) -> {
            tc.getGuild().getDefaultChannel().sendMessage("\u200BI don't have permissions to send messages in " + tc.getName() + "!").queue();
        });
    }

    //general purpose function for joining voice channels while warning and handling errors
    public static void joinVoiceChannel(VoiceChannel vc, boolean warning) {
        System.out.format("Joining '%s' voice channel in %s\n", vc.getName(), vc.getGuild().getName());

        //don't join afk channels
        if (vc == vc.getGuild().getAfkChannel()) {
            if (warning) {
                TextChannel tc = vc.getGuild().getTextChannelById(guildSettings.get(vc.getGuild().getId()).defaultTextChannel);
                sendMessage(tc, "I don't join afk channels!");
            }
        }

        //attempt to join channel and warn if permission is not available
        try {
            vc.getGuild().getAudioManager().openAudioConnection(vc);
        } catch (Exception e) {
            if (warning) {
                TextChannel tc = vc.getGuild().getTextChannelById(guildSettings.get(vc.getGuild().getId()).defaultTextChannel);
                sendMessage(tc, "I don't have permission to join " + vc.getName() + "!");
                return;
            } else {
                e.printStackTrace();
            }
        }

        //send alert to correct users in the voice channel
        DiscordEcho.alert(vc);

        //initalize the audio reciever listener
        double volume = DiscordEcho.guildSettings.get(vc.getGuild().getId()).volume;
        vc.getGuild().getAudioManager().setReceivingHandler(new AudioReceiveListener(volume, vc));

    }

    //general purpose function for leaving voice channels
    public static void leaveVoiceChannel(VoiceChannel vc) {
        System.out.format("Leaving '%s' voice channel in %s\n", vc.getName(), vc.getGuild().getName());

        vc.getGuild().getAudioManager().closeAudioConnection();
        DiscordEcho.killAudioHandlers(vc.getGuild());
    }
}
