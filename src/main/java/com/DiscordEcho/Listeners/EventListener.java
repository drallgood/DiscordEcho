package com.DiscordEcho.Listeners;

import com.DiscordEcho.Configuration.GuildSettings;
import com.DiscordEcho.Configuration.ServerSettings;
import com.DiscordEcho.DiscordEcho;
import com.DiscordEcho.Commands.CommandHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;

import static java.lang.Thread.sleep;


public class EventListener extends ListenerAdapter {


    @Override
    public void onGuildJoin(GuildJoinEvent e) {
        DiscordEcho.guildSettings.put(e.getGuild().getId(), new GuildSettings(e.getGuild()));
        DiscordEcho.writeSettingsJson();
        System.out.format("Joined new server '%s', connected to %s guilds\n", e.getGuild().getName(), e.getJDA().getGuilds().size());
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent e) {
        DiscordEcho.guildSettings.remove(e.getGuild().getId());
        DiscordEcho.writeSettingsJson();
        System.out.format("Left server '%s', connected to %s guilds\n", e.getGuild().getName(), e.getJDA().getGuilds().size());
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent e) {
        if(e.getMember() == null || e.getMember().getUser() == null || e.getMember().getUser().isBot())
            return;

        VoiceChannel biggestChannel = DiscordEcho.biggestChannel(e.getGuild().getVoiceChannels());

        if (e.getGuild().getAudioManager().isConnected()) {

            int newSize = DiscordEcho.voiceChannelSize(e.getChannelJoined());
            int botSize = DiscordEcho.voiceChannelSize(e.getGuild().getAudioManager().getConnectedChannel());
            GuildSettings settings  = DiscordEcho.guildSettings.get(e.getGuild().getId());
            int min = settings.autoJoinSettings.get(e.getChannelJoined().getId());
            
            if (newSize >= min && botSize < newSize) {  //check for tie with old server
                if (DiscordEcho.guildSettings.get(e.getGuild().getId()).autoSave)
                    DiscordEcho.writeToFile(e.getGuild());  //write data from voice channel it is leaving

                DiscordEcho.joinVoiceChannel(e.getChannelJoined(), false);
            }

        } else {
            if (biggestChannel != null) {
                DiscordEcho.joinVoiceChannel(e.getChannelJoined(), false);
            }
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent e) {
        if(e.getMember() == null || e.getMember().getUser() == null || e.getMember().getUser().isBot())
            return;

        int min = DiscordEcho.guildSettings.get(e.getGuild().getId()).autoLeaveSettings.get(e.getChannelLeft().getId());
        int size = DiscordEcho.voiceChannelSize(e.getChannelLeft());

        if (size <= min && e.getGuild().getAudioManager().getConnectedChannel() == e.getChannelLeft()) {

            if (DiscordEcho.guildSettings.get(e.getGuild().getId()).autoSave)
                DiscordEcho.writeToFile(e.getGuild());  //write data from voice channel it is leaving

            DiscordEcho.leaveVoiceChannel(e.getGuild().getAudioManager().getConnectedChannel());

            VoiceChannel biggest = DiscordEcho.biggestChannel(e.getGuild().getVoiceChannels());
            if (biggest != null) {
                DiscordEcho.joinVoiceChannel(biggest, false);
            }
        }
    }

    @Override
    public void onGuildVoiceMove (GuildVoiceMoveEvent e) {
        if(e.getMember() == null || e.getMember().getUser() == null || e.getMember().getUser().isBot())
            return;

        //Check if bot needs to join newly joined channel
        VoiceChannel biggestChannel = DiscordEcho.biggestChannel(e.getGuild().getVoiceChannels());

        if (e.getGuild().getAudioManager().isConnected()) {

            int newSize = DiscordEcho.voiceChannelSize(e.getChannelJoined());
            int botSize = DiscordEcho.voiceChannelSize(e.getGuild().getAudioManager().getConnectedChannel());
            GuildSettings settings  = DiscordEcho.guildSettings.get(e.getGuild().getId());
            int min = settings.autoJoinSettings.get(e.getChannelJoined().getId());

            if (newSize >= min && botSize < newSize) {  //check for tie with old server
                if (DiscordEcho.guildSettings.get(e.getGuild().getId()).autoSave)
                    DiscordEcho.writeToFile(e.getGuild());  //write data from voice channel it is leaving

                DiscordEcho.joinVoiceChannel(e.getChannelJoined(), false);
            }

        } else {
            if (biggestChannel != null) {
                DiscordEcho.joinVoiceChannel(biggestChannel, false);
            }
        }

        //Check if bot needs to leave old channel
        int min = DiscordEcho.guildSettings.get(e.getGuild().getId()).autoLeaveSettings.get(e.getChannelLeft().getId());
        int size = DiscordEcho.voiceChannelSize(e.getChannelLeft());

        if (size <= min && e.getGuild().getAudioManager().getConnectedChannel() == e.getChannelLeft()) {

            if (DiscordEcho.guildSettings.get(e.getGuild().getId()).autoSave)
                DiscordEcho.writeToFile(e.getGuild());  //write data from voice channel it is leaving

            DiscordEcho.leaveVoiceChannel(e.getGuild().getAudioManager().getConnectedChannel());;

            VoiceChannel biggest = DiscordEcho.biggestChannel(e.getGuild().getVoiceChannels());
            if (biggest != null) {
                DiscordEcho.joinVoiceChannel(e.getChannelJoined(), false);
            }
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e){
        if(e.getMember() == null || e.getMember().getUser() == null || e.getMember().getUser().isBot())
            return;

        String prefix = DiscordEcho.guildSettings.get(e.getGuild().getId()).prefix;
        //force help to always work with "!" prefix
        if (e.getMessage().getContentRaw().startsWith(prefix) || e.getMessage().getContentRaw().startsWith("!help")) {
            CommandHandler.handleCommand(CommandHandler.parser.parse(e.getMessage().getContentRaw().toLowerCase(), e));
        }
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent e) {
        if(e.getAuthor() == null || e.getAuthor().isBot())
            return;

        if (e.getMessage().getContentRaw().startsWith("!alerts")) {
            if (e.getMessage().getContentRaw().endsWith("off")) {
                for (Guild g : e.getJDA().getGuilds()) {
                    if (g.getMember(e.getAuthor()) != null) {
                        DiscordEcho.guildSettings.get(g.getId()).alertBlackList.add(e.getAuthor().getId());
                    }
                }
                e.getChannel().sendMessage("Alerts now off, message `!alerts on` to re-enable at any time").queue();
                DiscordEcho.writeSettingsJson();

            }

            else if (e.getMessage().getContentRaw().endsWith("on")) {
                for (Guild g : e.getJDA().getGuilds()) {
                    if (g.getMember(e.getAuthor()) != null) {
                        DiscordEcho.guildSettings.get(g.getId()).alertBlackList.remove(e.getAuthor().getId());
                    }
                }
                e.getChannel().sendMessage("Alerts now on, message `!alerts off` to disable at any time").queue();
                DiscordEcho.writeSettingsJson();
            }
            else {
                e.getChannel().sendMessage("!alerts [on | off]").queue();
            }

        /* removed because prefix and aliases are dependent on guild, which cannot be assumed without a message sent from guild
        } else if (e.getMessage().getContent().startsWith("!help")) {

            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor("Discord Echo", DiscordEcho.serverSettings.getHostUrl(), e.getJDA().getSelfUser().getAvatarUrl());
            embed.setColor(Color.RED);
            embed.setTitle("Currently in beta, being actively developed and tested. Expect bugs.");
            embed.setDescription("Join my guild for updates - https://discord.gg/JWNFSZJ");
            embed.setThumbnail("http://www.freeiconspng.com/uploads/information-icon-5.png");
            embed.setFooter("Replace brackets [] with item specified. Vertical bar | means 'or', either side of bar is valid choice.", "http://www.niceme.me");
            embed.addBlankField(false);

            Object[] cmds = CommandHandler.commands.keySet().toArray();
            Arrays.sort(cmds);
            for (Object command : cmds) {
                if (command == "help") continue;
                embed.addField(CommandHandler.commands.get(command).usage("!"), CommandHandler.commands.get(command).description(), true);
            }

            e.getChannel().sendMessage(embed.build()).queue();
        */
        } else {
            e.getChannel().sendMessage("DM commands unsupported, send `!help` in your guild chat for more info.").queue();
        }
    }

    @Override
    public void onReady(ReadyEvent e){
        e.getJDA().getPresence().setGame(new Game("Game") {
            @Override
            public String getName() {
                return "!help | DicordEcho.com";
            }

            @Override
            public String getUrl() {
                return "http://DicordEcho.com";
            }

            @Override
            public GameType getType() {
                return GameType.DEFAULT;
            }
        });

        try {
            System.out.format("ONLINE: Connected to %s guilds!\n", e.getJDA().getGuilds().size(), e.getJDA().getVoiceChannels().size());

            Gson gson = new Gson();

            FileReader guildSettingsFileReader = new FileReader("conf/settings.json");
            Type guildSettingsType = new TypeToken<HashMap<String, GuildSettings>>(){}.getType();
            DiscordEcho.guildSettings = gson.fromJson(guildSettingsFileReader, guildSettingsType);

            FileReader serverSettingsFileReader = new FileReader("conf/server.json");
            Type serverSettingsType = new TypeToken<ServerSettings>(){}.getType();
            DiscordEcho.serverSettings = gson.fromJson(serverSettingsFileReader, serverSettingsType);

            if (DiscordEcho.guildSettings == null)
                DiscordEcho.guildSettings = new HashMap<>();

            guildSettingsFileReader.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        for (Guild g : e.getJDA().getGuilds()) {    //validate settings files
            if (!DiscordEcho.guildSettings.containsKey(g.getId())) {
                DiscordEcho.guildSettings.put(g.getId(), new GuildSettings(g));
                DiscordEcho.writeSettingsJson();
            }
        }

        File dir = new File(DiscordEcho.serverSettings.getRecordingStoragePath());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        System.out.format("ONLINE: Saving recordings to %s\n", dir.getAbsolutePath());

        File[] files = dir.listFiles();

        if(files != null) {
            for (File f : files) {
                if (f.getName().substring(f.getName().lastIndexOf('.'), f.getName().length()).equals(".mp3")) {
                    new Thread(() -> {

                        try {
                            sleep(1000 * 60 * 30);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        f.delete();
                        System.out.println("\tDeleting file " + f.getName() + "...");

                    }).start();
                }
            }
        }

        //check for servers to join
        for (Guild g : e.getJDA().getGuilds()) {
            VoiceChannel biggest = DiscordEcho.biggestChannel(g.getVoiceChannels());
            if (biggest != null) {
                DiscordEcho.joinVoiceChannel(DiscordEcho.biggestChannel(g.getVoiceChannels()), true);
            }
        }
    }
}
