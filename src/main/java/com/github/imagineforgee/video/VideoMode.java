package com.github.imagineforgee.video;

import com.github.imagineforgee.client.VoiceClient;

public interface VideoMode {
    default void setVoiceClient(VoiceClient client) {}
    void start(String source); // e.g., video file or stream URL
    void stop();
    void initialize(); // e.g., encoder setup
    void joinChannel(String guildId, String channelId);
    void shutdown();
    boolean isActive();
}
