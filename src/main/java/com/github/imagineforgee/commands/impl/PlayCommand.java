package com.github.imagineforgee.commands.impl;

import com.clawsoftstudios.purrfectlib.annotations.Command;
import com.github.imagineforgee.commands.CommandArgs;
import com.github.imagineforgee.commands.CommandContext;
import com.github.imagineforgee.client.UserBotClient;
import com.github.imagineforgee.client.VoiceClient;

@Command(name = "play", description = "Play a song", group = "music")
public class PlayCommand {

    public void execute(CommandArgs args, CommandContext ctx) {
        String song = args.getKey("song");
        boolean loop = args.getFlag("loop") || args.getBool("loop", false);

        String voiceChannelId = ctx.getMemberVoiceChannelId();

        if (voiceChannelId == null) {
            ctx.reply("❌ You must be in a voice channel to use this command.");
            return;
        }

        VoiceClient voiceClient = ctx.getBot().getVoiceClient();
        ctx.reply("🎶 Playing: " + song + (loop ? " (looping)" : ""));
        voiceClient.playTrack(song);
    }
}
