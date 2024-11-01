package com.lld;

import com.lld.exceptions.ErrorResponseBuilder;
import com.lld.exceptions.VideoEnhancementException;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        final String FFMPEG_PATH = "/opt/homebrew/bin/ffmpeg";
        final String FFPROBE_PATH = "/opt/homebrew/bin/ffprobe";

        String rawVideoRecordingPath = "/Users/kmanchukonda/music/phir-mohabbat/phir-mohabbat.mov";

        FFmpeg ffmpeg = new FFmpeg(FFMPEG_PATH);
        FFprobe ffprobe = new FFprobe(FFPROBE_PATH);

        PhotoBoothVideoEnhancer pb = new PhotoBoothVideoEnhancer(ffmpeg, ffprobe);

        try {
            pb.enhanceVideo(rawVideoRecordingPath);
        } catch (VideoEnhancementException vex) {
            // logger.error("Failed to enhance video. Path: {}, Operation: {}", vex.getVideoPath(), vex.getOperation());
            logger.error("Error details: {}", ErrorResponseBuilder.buildErrorResponse(vex));
        }
    }
}
