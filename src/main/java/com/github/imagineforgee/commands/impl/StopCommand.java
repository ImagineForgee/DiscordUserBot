package com.github.imagineforgee.commands.impl;

import com.clawsoftstudios.purrfectlib.annotations.Command;
import com.github.imagineforgee.client.VoiceClient;
import com.github.imagineforgee.commands.CommandArgs;
import com.github.imagineforgee.commands.CommandContext;

@Command(name = "stop", description = "Stops the playback", group = "music")
public class StopCommand {
    public void execute(CommandArgs args, CommandContext ctx) {
        VoiceClient voiceClient = ctx.getBot().getVoiceClient();
        voiceClient.stop();
        ctx.reply("⏹️ Stopped playback.");
    }
}
