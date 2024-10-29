package com.lld;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;

import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        final String FFMPEG_PATH = "/opt/homebrew/bin/ffmpeg";

        String rawVideoRecordingPath = "/Users/kmanchukonda/music/te-amo/te-amo-raw-booth.mov";

        FFmpeg ffmpeg = new FFmpeg("/opt/homebrew/bin/ffmpeg");
        FFprobe ffprobe = new FFprobe("/opt/homebrew/bin/ffprobe");

        PhotoBoothVideoEnhancer pb = new PhotoBoothVideoEnhancer(ffmpeg, ffprobe);
        pb.enhanceVideo(rawVideoRecordingPath);
    }
}
