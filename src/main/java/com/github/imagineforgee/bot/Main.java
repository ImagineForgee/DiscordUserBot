package com.github.imagineforgee.bot;

import com.clawsoftstudios.purrfectlib.scanner.CommandInfo;
import com.clawsoftstudios.purrfectlib.scanner.CommandScanner;
import com.github.imagineforgee.client.UserBotClient;
import com.google.gson.*;
import reactor.core.publisher.Mono;

import java.util.List;

public class Main {
    private static UserBotClient bot;
    private static final CommandScanner scanner =
            new CommandScanner("com.github.imagineforgee.bot.commands.impl");

    public static void main(String[] args) throws Exception {
        String userToken = null;
        for (String arg : args) {
            if (arg.startsWith("--token=")) {
                userToken = arg.substring("--token=".length());
            }
        }
        if (userToken == null || userToken.isEmpty()) {
            System.err.println("❌ Missing user token! Please run with --token=<your_token>");
            System.exit(1);
        }

        // scan your commands
        List<CommandInfo> commands = scanner.scanCommands();
        bot = new UserBotClient(userToken);
        bot.registerCommands(commands);

        // build the poll payload
        JsonObject payload = new JsonObject();
        payload.addProperty("content", "🗳️ Vote to blacklist <@123…>!");

        JsonObject poll = new JsonObject();

// ─── Correct question object ───────────────────────────────────────
        JsonObject questionObj = new JsonObject();
        questionObj.addProperty("type", "text");
        questionObj.addProperty("content", "Should we blacklist this user?");
        poll.add("question", questionObj);
// ───────────────────────────────────────────────────────────────────

        poll.addProperty("duration", 60);
        poll.addProperty("allow_multiple_answers", false);

// build answers with required poll_media
        JsonArray answers = new JsonArray();

// YES answer
        JsonObject yesAns = new JsonObject();
        yesAns.addProperty("text", "Yes");
        yesAns.addProperty("emoji", "✅");
// add the required poll_media
        JsonObject yesMedia = new JsonObject();
        yesMedia.addProperty("type", "text");
        yesMedia.addProperty("content", "Yes");
        yesAns.add("poll_media", yesMedia);
        answers.add(yesAns);

// NO answer
        JsonObject noAns = new JsonObject();
        noAns.addProperty("text", "No");
        noAns.addProperty("emoji", "❌");
// add the required poll_media
        JsonObject noMedia = new JsonObject();
        noMedia.addProperty("type", "text");
        noMedia.addProperty("content", "No");
        noAns.add("poll_media", noMedia);
        answers.add(noAns);

// use "answers" (not "options")
        poll.add("answers", answers);
        payload.add("poll", poll);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        String prettyJson = gson.toJson(payload);
        System.out.println(prettyJson);
        // connect and send
        bot.connect()
                .then(Mono.fromRunnable(() -> {
                    System.out.println("✅ Bot connected!");
                    bot.sendRawMessage("1385461917115482242", payload);
                    bot.startCommandListener();
                }))
                .subscribe();
    }
}
