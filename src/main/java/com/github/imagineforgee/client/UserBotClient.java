package com.github.imagineforgee.client;

import com.clawsoftstudios.purrfectlib.scanner.CommandInfo;
import com.github.imagineforgee.commands.CommandContext;
import com.github.imagineforgee.commands.CommandManager;
import com.github.imagineforgee.dispatch.EventDispatcher;
import com.github.imagineforgee.dispatch.events.*;
import com.github.imagineforgee.gateway.GatewayClient;
import com.github.imagineforgee.dispatch.Event;
import com.github.imagineforgee.http.DmAndChannelService;
import com.github.imagineforgee.http.MessageSender;
import com.github.imagineforgee.util.VoiceStateRegistry;
import com.google.gson.JsonObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class UserBotClient {

    private final GatewayClient gatewayClient;
    private final EventDispatcher dispatcher;
    private final MessageSender messageSender;
    private final DmAndChannelService dmService;
    private final VoiceClient voiceClient;
    private final CommandManager commandManager = new CommandManager();

    private String selfId;

    public UserBotClient(String token) {
        this.gatewayClient = new GatewayClient(token);
        this.dispatcher = new EventDispatcher();
        this.messageSender = new MessageSender(token);
        this.dmService = new DmAndChannelService(token);
        this.voiceClient = new VoiceClient(this);

        dispatcher.registerParser("READY", ReadyEvent::new);
        this.onEvent(ReadyEvent.class)
                .subscribe(evt -> {
                    this.selfId = evt.getUserId();
                    System.out.println("[READY] Bot user ID set: " + selfId);
                });

        dispatcher.registerParser("MESSAGE_CREATE", MessageCreateEvent::new);
        dispatcher.registerParser("VOICE_STATE_UPDATE", VoiceStateUpdateEvent::new);
        this.onEvent(VoiceStateUpdateEvent.class)
                .subscribe(evt -> {
                    String userId = evt.getUserId();
                    String channelId = evt.getChannelId();
                    System.out.printf("[VOICE_STATE_UPDATE] %s -> %s%n", userId, channelId);
                    VoiceStateRegistry.update(userId, channelId);
                });

        dispatcher.registerParser("VOICE_SERVER_UPDATE", VoiceServerUpdateEvent::new);

        gatewayClient.getEventFlux()
                .filter(json -> json.has("op") && json.get("op").getAsInt() == 0)
                .subscribe(dispatcher::dispatch);
    }

    public GatewayClient getGatewayClient() {
        return gatewayClient;
    }

    public Mono<Void> connect() {
        return gatewayClient.connect();
    }

    public Flux<Event> getEvents() {
        return dispatcher.getEventFlux();
    }

    public <T extends Event> Flux<T> onEvent(Class<T> clazz) {
        return dispatcher.onEvent(clazz);
    }

    public Flux<MessageCreateEvent> onMessageCreate() {
        return onEvent(MessageCreateEvent.class);
    }

    public Mono<Void> sendMessage(String channelId, String content) {
        return Mono.fromFuture(messageSender.sendMessage(channelId, content));
    }

    public Mono<Void> replyToMessage(String channelId, String messageId, String content) {
        return Mono.fromFuture(
                messageSender.sendMessage(channelId, content, messageId)
        );
    }

    public Mono<Void> sendDm(String userId, String content) {
        return Mono.fromFuture(dmService.createDmChannel(userId))
                .flatMap(channelId -> sendMessage(channelId, content));
    }

    public void registerCommands(List<CommandInfo> cmds) {
        commandManager.registerCommands(cmds);
    }

    public void startCommandListener() {
        this.onMessageCreate().subscribe(event -> {
            String content = event.getContent();

            String mention = "<@1385453543707639838>";

            String message = content.trim();
            String cmdName = null;
            String args = "";

            if (message.startsWith(mention)) {
                message = message.substring(mention.length()).trim();
                String[] parts = message.split("\\s+", 2);
                cmdName = parts[0];
                if (parts.length > 1) args = parts[1];
            } else {
                return;
            }

            CommandContext ctx = new CommandContext(event, this);
            commandManager.handleCommand(cmdName, args, ctx);
        });
    }

    public VoiceClient getVoiceClient() {
        return voiceClient;
    }

    public String getSelfId() {
        return selfId;
    }
}
