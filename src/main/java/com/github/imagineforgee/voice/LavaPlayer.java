package com.github.imagineforgee.voice;

import com.github.imagineforgee.client.VoiceClient;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import reactor.core.publisher.Flux;

import java.io.IOException;

public class LavaPlayer implements VoiceMode {
    private final AudioPlayerManager playerManager;
    private final com.sedmelluq.discord.lavaplayer.player.AudioPlayer lavaPlayer;
    private final OpusUdpStreamer streamer;
	private final VoiceClient voiceClient;

    public LavaPlayer(VoiceClient voiceClient, OpusUdpStreamer opusUdpStreamer) throws IOException {

        this.playerManager = new DefaultAudioPlayerManager();
        this.playerManager.getConfiguration().setOutputFormat(StandardAudioDataFormats.DISCORD_OPUS);
        this.playerManager.registerSourceManager(new YoutubeAudioSourceManager(true));

        this.voiceClient = voiceClient;
        this.lavaPlayer = playerManager.createPlayer();
        this.streamer = opusUdpStreamer;
    }

    public void start(String url) {
        System.out.println("[Voice] Loading track: " + url);
        stopStreaming();

        playerManager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                System.out.println("[Voice] Track loaded successfully: " + track.getInfo().title);
                lavaPlayer.playTrack(track);
                startAudioStream();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    AudioTrack firstTrack = playlist.getTracks().get(0);
                    lavaPlayer.playTrack(firstTrack);
                    startAudioStream();
                }
            }

            @Override
            public void noMatches() {
                System.err.println("[Voice] No matches found for: " + url);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                System.err.println("[Voice] Failed to load track: " + exception.getMessage());
            }
        });
    }

    private void startAudioStream() {
        System.out.println("[Voice] Starting audio stream");
        stopStreaming();

        Flux<byte[]> opusFrames = Flux.generate(sink -> {
            AudioFrame frame = lavaPlayer.provide();
            if (frame != null && frame.getData() != null && frame.getData().length > 0) {
                sink.next(frame.getData());
            }
        });

        voiceClient.setSpeaking(SpeakingFlag.MICROPHONE);
        streamer.start(opusFrames);
    }
    public void stop() {
        lavaPlayer.stopTrack();
        stopStreaming();
    }

    @Override
    public void initialize() {
        System.out.println("[Voice] LavaPlayer initialized and ready to stream");
    }

    @Override
    public void joinChannel(String guildId, String channelId) {
        System.out.println("[Voice] LavaPlayer aware of joined channel: " + guildId + "/" + channelId);
    }

    private void stopStreaming() {
        streamer.stop();
    }

    public void shutdown() {
        stop();
        playerManager.shutdown();
    }

    @Override
    public boolean isActive() {
        return lavaPlayer.getPlayingTrack() != null;
    }
}
