package com.github.imagineforgee.bot.commands.impl;

import com.clawsoftstudios.purrfectlib.annotations.Command;
import com.github.imagineforgee.bot.util.Blacklist;
import com.github.imagineforgee.commands.CommandArgs;
import com.github.imagineforgee.commands.CommandContext;

@Command(name = "blacklist", description = "Manage blacklist for users and guilds", group = "admin")
public class BlacklistCommand {

    public void execute(CommandArgs args, CommandContext ctx) {
        String action = args.get(0);
        String type = args.get(1);
        String id = args.get(2);

        if (!ctx.getMsgEvent().getAuthorId().equals("")) {
            ctx.reply("❌ You don't have permission to manage the blacklist.");
            return;
        }

        if ("list".equalsIgnoreCase(action)) {
            ctx.reply(buildList());
            return;
        }

        if (action == null || type == null || id == null) {
            ctx.reply("❌ Usage: blacklist <add|remove|list> <user|guild> <id>");
            return;
        }

        switch (action.toLowerCase()) {
            case "add" -> {
                if (type.equalsIgnoreCase("user")) {
                    Blacklist.addUser(id);
                    ctx.reply("✅ User `" + id + "` blacklisted.");
                } else if (type.equalsIgnoreCase("guild")) {
                    Blacklist.addGuild(id);
                    ctx.reply("✅ Guild `" + id + "` blacklisted.");
                } else {
                    ctx.reply("❌ Unknown type: " + type);
                }
            }
            case "remove" -> {
                if (type.equalsIgnoreCase("user")) {
                    Blacklist.removeUser(id);
                    ctx.reply("✅ User `" + id + "` removed from blacklist.");
                } else if (type.equalsIgnoreCase("guild")) {
                    Blacklist.removeGuild(id);
                    ctx.reply("✅ Guild `" + id + "` removed from blacklist.");
                } else {
                    ctx.reply("❌ Unknown type: " + type);
                }
            }
            default -> ctx.reply("❌ Unknown action: " + action);
        }
    }

    private String buildList() {
        StringBuilder sb = new StringBuilder("📝 **Blacklist:**\n");
        sb.append("**Users:**\n");
        for (String user : Blacklist.getBlacklistedUsers()) {
            sb.append("- ").append(user).append("\n");
        }
        sb.append("**Guilds:**\n");
        for (String guild : Blacklist.getBlacklistedGuilds()) {
            sb.append("- ").append(guild).append("\n");
        }
        return sb.toString();
    }
}
