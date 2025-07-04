package com.github.imagineforgee.video;

public interface VideoMode {
    void start(String source); // e.g., video file or stream URL
    void stop();
    void initialize(); // e.g., encoder setup
    void joinChannel(String guildId, String channelId);
    void shutdown();
    boolean isActive();
}
