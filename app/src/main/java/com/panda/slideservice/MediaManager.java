package com.panda.slideservice;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;

import java.io.IOException;


public class MediaManager {
    private static MediaManager instance;
    public MediaPlayer player;
    public boolean isPlaying;
    private onStopCallBack event;

    private Handler handler = new Handler();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            stopSound();
        }
    };

    public interface onStopCallBack {
        void stopCallBack();
    }

    public static MediaManager getInstance() {
        if (instance == null) {
            instance = new MediaManager();
        }
        return instance;
    }


    public void playSound(Context context, String soundId, int duration) {
        stopSound(); // Dừng âm thanh đang phát trước khi phát âm thanh mới
        AssetFileDescriptor afd = null;
        try {
            afd = context.getAssets().openFd(soundId);
            player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.setLooping(true);
            player.start();
            isPlaying = true;
            handler.postDelayed(runnable, duration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void playSound(Context context, String soundId, int duration, onStopCallBack event) {
        try {
            stopSound(); // Dừng âm thanh đang phát trước khi phát âm thanh mới
            AssetFileDescriptor afd = null;
            afd = context.getAssets().openFd(soundId);
            player = new MediaPlayer();
            player.setDataSource(afd);
            player.prepare();
            player.setLooping(true);
            player.start();
            isPlaying = true;

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (event != null) {
                        event.stopCallBack(); // Gọi hàm stopCallBack khi đạt đến số lần loop
                    }
                    stopSound();
                }
            }, duration);
        } catch (Exception ignored) {

        }
    }


    public void stopSound() {
        try {
            if (isPlaying) {
                player.pause();
                player.seekTo(0);
                player.setLooping(false);
                player.release(); // Giải phóng tài nguyên MediaPlayer
                isPlaying = false;
            }
            handler.removeCallbacks(runnable);
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }
}

