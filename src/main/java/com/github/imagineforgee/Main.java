package com.github.imagineforgee;

import com.clawsoftstudios.purrfectlib.scanner.CommandInfo;
import com.clawsoftstudios.purrfectlib.scanner.CommandScanner;
import com.github.imagineforgee.client.UserBotClient;
import reactor.core.publisher.Mono;

import java.util.List;

public class Main {
    private static UserBotClient bot;
    private static final CommandScanner scanner = new CommandScanner("com.github.imagineforgee.commands.impl");

    public static void main(String[] args) throws Exception {
        String userToken = null;

        for (String arg : args) {
            if (arg.startsWith("--token=")) {
                userToken = arg.substring("--token=".length());
            }
        }

        if (userToken == null || userToken.isEmpty()) {
            System.err.println("❌ Missing user token! Please run the program with --token=<your_token>");
            System.exit(1);
        }

        List<CommandInfo> commands = scanner.scanCommands();
        bot = new UserBotClient(userToken);
        bot.registerCommands(commands);

        bot.connect()
                .then(Mono.fromRunnable(() -> {
                    System.out.println("✅ Bot connected!");
                    bot.startCommandListener();
                }))
                .subscribe();
    }
}
