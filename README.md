# Discord Echo

[![Build Status](https://travis-ci.org/drallgood/DiscordEcho.svg?branch=master)](https://travis-ci.org/drallgood/DiscordEcho)

This little application allows you to record any Discord voice channel on your server

## How to build

### Build a docker image

`./gradlew dockerBuildImage`

## How to set up

### Set up Discord Echo
- Create a `conf` directory
- Aquire a bot token (see below) and store it in `conf/shark_token`
- Add the bot to your server(s) `https://discordapp.com/api/oauth2/authorize?client_id=<CLIENT_ID>&permissions=0&scope=bot` (replace `<CLIENT_ID>` with your just created application's Client ID).

### How to set up a bot

- Register a new application with Discord: https://discordapp.com/developers/applications/me
- Click "Create Bot User" and follow the instructions 
- Copy the token 

### Running as a docker container

```docker run -v `pwd`/conf/:/conf -v `pwd`/recordings:/recordings discordecho:1.0 bash```

You should obviously make sure that your `recordings/` directory is reachable from the outside.

## Using Discord Echo

Once the bot has joined a server, it will automatically listen to commands in any channel.
Simply type `!help` to get an overview what the server can do.
  