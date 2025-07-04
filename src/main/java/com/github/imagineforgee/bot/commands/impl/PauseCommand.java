package com.github.imagineforgee.bot.commands.impl;

import com.clawsoftstudios.purrfectlib.annotations.Command;
import com.github.imagineforgee.commands.CommandContext;

@Command(name = "pause", description = "Pauses the song", group = "music")
public class PauseCommand {
    public void execute(String args, CommandContext ctx) {
        ctx.reply("⏸️ Paused.");
    }
}
