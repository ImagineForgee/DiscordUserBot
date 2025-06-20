package com.github.imagineforgee.commands;

import com.clawsoftstudios.purrfectlib.scanner.CommandInfo;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager {

    private final Map<String, CommandInfo> commands = new HashMap<>();

    public void registerCommands(List<CommandInfo> commandList) {
        for (CommandInfo cmd : commandList) {
            commands.put(cmd.getName(), cmd);
        }
    }

    public void handleCommand(String commandName, String args, CommandContext context) {
        CommandInfo cmd = commands.get(commandName);
        if (cmd == null) {
            context.reply("Unknown command: " + commandName);
            return;
        }

        try {
            Object cmdInstance = cmd.getCommandClass().getDeclaredConstructor().newInstance();
            CommandArgs parsedArgs = new CommandArgs(args);
            cmd.getCommandClass()
                    .getMethod("execute", CommandArgs.class, CommandContext.class)
                    .invoke(cmdInstance, parsedArgs, context);

        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            context.reply("Failed to execute command: " + commandName);
        }
    }
}
