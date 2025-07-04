package com.github.imagineforgee.bot.commands.impl;

import com.clawsoftstudios.purrfectlib.annotations.Command;
import com.github.imagineforgee.commands.CommandArgs;
import com.github.imagineforgee.commands.CommandContext;
import com.github.imagineforgee.dispatch.events.VoiceStateUpdateEvent;
import com.github.imagineforgee.util.VoiceStateRegistry;
import reactor.core.publisher.Mono;

@Command(name = "join", description = "Join a voice channel", group = "music")
public class JoinCommand {
    public void execute(CommandArgs args, CommandContext ctx) {
        String vcId = args.getKey("vc");
        if (vcId == null) {
            vcId = ctx.getMsgEvent().getUserVoiceChannelId();
            System.out.println(vcId);
            if (vcId == null) {
                ctx.reply("❌ You must be in a voice channel or specify one with `vc={id}`.");
                return;
            }
        }

        ctx.getBot().getVoiceClient()
                .joinAndConnect(ctx.getGuildId(), vcId)
                .doOnSuccess(v -> ctx.reply("✅ Joined voice channel!"))
                .doOnError(e -> ctx.reply("❌ Failed to join: " + e.getMessage()))
                .subscribe();


    }
}

