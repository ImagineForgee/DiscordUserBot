package com.github.imagineforgee.util;

import com.goterl.lazysodium.LazySodiumJava;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioPlayer {
    private final LazySodiumJava lazySodium;
    private final DatagramSocket udp;
    private final AtomicBoolean isConnected;
    private final AudioPlayerManager playerManager;
    private final com.sedmelluq.discord.lavaplayer.player.AudioPlayer lavaPlayer;

    private final int ssrc;
    private final byte[] secretKey;
    private final InetAddress destinationAddress;
    private final int destinationPort;

    private Disposable currentStream;

    public AudioPlayer(LazySodiumJava lazySodium, DatagramSocket udp, AtomicBoolean isConnected,
                       int ssrc, byte[] secretKey, InetAddress destinationAddress, int destinationPort) {
        this.lazySodium = lazySodium;
        this.udp = udp;
        this.isConnected = isConnected;
        this.playerManager = new DefaultAudioPlayerManager();

        this.ssrc = ssrc;
        this.secretKey = secretKey;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;

        playerManager.getConfiguration().setOutputFormat(StandardAudioDataFormats.DISCORD_OPUS);
        YoutubeAudioSourceManager youtube = new YoutubeAudioSourceManager(true);
        playerManager.registerSourceManager(youtube);
        this.lavaPlayer = playerManager.createPlayer();
    }

    public void loadAndPlay(String url) {
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
                System.out.println("[Voice] Playlist loaded with " + playlist.getTracks().size() + " tracks");
                if (!playlist.getTracks().isEmpty()) {
                    AudioTrack firstTrack = playlist.getTracks().get(0);
                    System.out.println("[Voice] Playing first track: " + firstTrack.getInfo().title);
                    lavaPlayer.playTrack(firstTrack);
                    startAudioStream();
                } else {
                    System.err.println("[Voice] Playlist is empty!");
                }
            }

            @Override
            public void noMatches() {
                System.err.println("[Voice] No matches found for: " + url);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                System.err.println("[Voice] Failed to load track: " + exception.getMessage());
                exception.printStackTrace();
            }
        });
    }

    private void startAudioStream() {
        System.out.println("[Voice] Starting audio stream with SSRC: " + ssrc);

        stopStreaming();

        AtomicInteger sequence = new AtomicInteger(0);
        AtomicInteger timestamp = new AtomicInteger((int) (System.currentTimeMillis() & 0xFFFFFFFF));
        AtomicInteger frameCount = new AtomicInteger(0);

        currentStream = Flux.interval(Duration.ofMillis(20))
                .takeWhile(tick -> isConnected.get())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(tick -> getAudioFrame())
                .filter(frame -> frame.getData() != null && frame.getData().length > 0)
                .doOnNext(frame -> {
                    int count = frameCount.incrementAndGet();
                    if (count % 50 == 0) {
                        System.out.println("[Voice] Sent " + count + " audio frames");
                    }
                })
                .subscribe(
                        frame -> sendAudioFrame(
                                ssrc,
                                secretKey,
                                frame.getData(),
                                sequence.getAndUpdate(s -> (s + 1) & 0xFFFF),
                                timestamp.getAndUpdate(t -> (t + 960) & 0xFFFFFFFF)
                        ),
                        error -> {
                            System.err.println("[Voice] Audio stream error: " + error.getMessage());
                            error.printStackTrace();
                        },
                        () -> {
                            System.out.println("[Voice] Audio stream completed. Total frames sent: " + frameCount.get());
                        }
                );
    }

    private Mono<AudioFrame> getAudioFrame() {
        return Mono.fromCallable(() -> lavaPlayer.provide())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(throwable -> {
                    System.err.println("[Voice] Error getting audio frame: " + throwable.getMessage());
                    return Mono.empty();
                });
    }

    private void sendAudioFrame(int ssrc, byte[] secretKey, byte[] opusFrame, int sequence, int timestamp) {
        try {
            byte[] rtpHeader = createRtpHeader(ssrc, sequence, timestamp);
            byte[] nonce = new byte[24];
            System.arraycopy(rtpHeader, 0, nonce, 0, 12);
            byte[] encrypted = new byte[opusFrame.length + 16];

            if (!lazySodium.cryptoSecretBoxEasy(encrypted, opusFrame, opusFrame.length, nonce, secretKey)) {
                System.err.println("[Voice] Encryption failed!");
                return;
            }

            byte[] packet = new byte[12 + encrypted.length];
            System.arraycopy(rtpHeader, 0, packet, 0, 12);
            System.arraycopy(encrypted, 0, packet, 12, encrypted.length);

            DatagramPacket dp = new DatagramPacket(packet, packet.length, destinationAddress, destinationPort);
            udp.send(dp);
        } catch (Exception e) {
            System.err.println("[Voice] Failed to send audio packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private byte[] createRtpHeader(int ssrc, int sequence, int timestamp) {
        byte[] header = new byte[12];
        header[0] = (byte) 0x80;
        header[1] = (byte) 0x78;
        header[2] = (byte) ((sequence >> 8) & 0xFF);
        header[3] = (byte) (sequence & 0xFF);
        header[4] = (byte) ((timestamp >> 24) & 0xFF);
        header[5] = (byte) ((timestamp >> 16) & 0xFF);
        header[6] = (byte) ((timestamp >> 8) & 0xFF);
        header[7] = (byte) (timestamp & 0xFF);
        header[8] = (byte) ((ssrc >> 24) & 0xFF);
        header[9] = (byte) ((ssrc >> 16) & 0xFF);
        header[10] = (byte) ((ssrc >> 8) & 0xFF);
        header[11] = (byte) (ssrc & 0xFF);
        return header;
    }

    public boolean isPlaying() {
        return lavaPlayer.getPlayingTrack() != null;
    }

    public void stop() {
        lavaPlayer.stopTrack();
        stopStreaming();
        System.out.println("[Voice] Stopped audio playback");
    }

    private void stopStreaming() {
        if (currentStream != null && !currentStream.isDisposed()) {
            currentStream.dispose();
            currentStream = null;
        }
    }

    public void shutdown() {
        stop();
        playerManager.shutdown();
    }
}