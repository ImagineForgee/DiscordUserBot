package com.github.imagineforgee.commands.impl;

import com.clawsoftstudios.purrfectlib.annotations.Command;
import com.github.imagineforgee.commands.CommandArgs;
import com.github.imagineforgee.commands.CommandContext;
import reactor.core.publisher.Mono;

@Command(name = "leave", description = "Leaves the voice channel", group = "voice")
public class LeaveCommand {
    public void execute(CommandArgs args, CommandContext ctx) {
        String guildId = ctx.getMsgEvent().getGuildId();

        if (guildId == null) {
            ctx.reply("❌ This command can only be used in a guild!");
            return;
        }

        ctx.getBot().getVoiceClient().leaveVoice(guildId)
                .then(ctx.reply("✅ Left the voice channel!"))
                .subscribe();
    }
}
