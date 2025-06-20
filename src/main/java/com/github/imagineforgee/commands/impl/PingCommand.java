package com.github.imagineforgee.commands.impl;

import com.clawsoftstudios.purrfectlib.annotations.Command;
import com.github.imagineforgee.commands.CommandContext;

import java.time.OffsetDateTime;

@Command(name = "ping", description = "Replies with pong!", group = "general")
public class PingCommand {
    public void execute(String args, CommandContext ctx) {
        ctx.reply("Pong!");
    }
}
