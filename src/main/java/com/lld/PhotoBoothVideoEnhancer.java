package com.lld;

import com.lld.exceptions.VideoEnhancementException;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// TODO: add retries

public class PhotoBoothVideoEnhancer {
    private static final Logger logger = LoggerFactory.getLogger(PhotoBoothVideoEnhancer.class);

    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;
    private String sandboxDirPath;

    public PhotoBoothVideoEnhancer(final FFmpeg ffmpeg, final FFprobe ffprobe) {
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;

        logger.info("PhotoBoothVideoEnhancer initialized with ffmpeg: {}, ffprobe: {}", ffmpeg, ffprobe);
    }

    private String convertVideoToMP4(String rawInputMovFilePath) {
        try {
            logger.debug("start: convertVideoToMP4({})", rawInputMovFilePath);

            FFmpegBuilder command = new FFmpegBuilder();
            String outputPath = sandboxDirPath + "/raw-video-and-audio.mp4";

            command.addInput(rawInputMovFilePath).addOutput(outputPath);

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            long startTime = System.currentTimeMillis();
            executor.createJob(command).run();
            long duration = System.currentTimeMillis() - startTime;

            logger.debug("end: convertVideoToMP4({})", rawInputMovFilePath);
            logger.info("Video conversion completed. Duration: {}ms, Output: {}", duration, outputPath);

            return outputPath;
        } catch (Exception e) {
            logger.error("failed to convert video to mp4", e);
            throw new VideoEnhancementException("video conversion from mov to mp4 failed", e, rawInputMovFilePath, "convertVideoToMP4");
        }
    }

    private List<String> splitVideoAndAudio(String rawVideoAndAudio) {
        try {
            logger.debug("start: splitVideoAndAudio({})", rawVideoAndAudio);

            final String rawAudioPath = sandboxDirPath + "/raw-audio.mp3";
            final String rawVideoPath = sandboxDirPath + "/raw-video.mp4";

            FFmpegBuilder command = new FFmpegBuilder();
            // ffmpeg -i raw-video.mp4 -q:a 0 -map a te-amo-audio-raw.mp3
            command.addInput(rawVideoAndAudio).addOutput(rawAudioPath).addExtraArgs("-map", "a").addExtraArgs("-q:a", "0");
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(command).run();

            // ffmpeg -i raw-video.mp4 -an vcodec copy te-amo-video-raw.mp4
            command = new FFmpegBuilder();
            command.addInput(rawVideoAndAudio).addOutput(rawVideoPath).setVideoCodec("copy").disableAudio();

            long startTime = System.currentTimeMillis();
            executor.createJob(command).run();
            long duration = System.currentTimeMillis() - startTime;

            logger.info("Completed splitting audio and video in duration: {}ms, audio file path: {}, video file path: {}", duration, rawAudioPath, rawVideoPath);
            return Arrays.asList(rawAudioPath, rawVideoPath);
        } catch (Exception e) {
            logger.error("failed to split video", e);
            throw new VideoEnhancementException("video splitting failed", e, rawVideoAndAudio, "splitVideoAndAudio");
        }
    }

    private String increaseAudioLevelsBy10x(String rawAudio) {
        try {
            String enhancedAudioPath = sandboxDirPath + "/raw-audio-10x.mp3";

            FFmpegBuilder command = new FFmpegBuilder();

            // ffmpeg -i te-amo-audio-raw.mp3 -filter:a "volume=10" te-amo-audio-2x.mp3

            command.addInput(rawAudio).addOutput(enhancedAudioPath).setAudioFilter("volume=10");

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(command).run();

            return enhancedAudioPath;
        } catch (Exception e) {
            throw new VideoEnhancementException("failed bumping the audio level up to 10x", e, rawAudio, "increaseAudioLevelsBy10x");
        }
    }

    private String combineVideoAndAudio(String rawVideo, String enhancedAudio) {
        final String finalVideoPath = sandboxDirPath + "/final.mp4";

        try {
            FFmpegBuilder command = new FFmpegBuilder();

            // ffmpeg -i raw-video.mp4 -i raw-audio-10x.mp3 -c:v copy -c:a aac final.mp4
            command.addInput(rawVideo).addInput(enhancedAudio).addOutput(finalVideoPath).setVideoCodec("copy").setAudioCodec("aac");

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(command).run();

            return finalVideoPath;
        } catch (Exception e) {
            throw new VideoEnhancementException("failed merging enhanced audio and raw video", e, finalVideoPath, "combineVideoAndAudio");
        }
    }

    private void cp(String sourcePath, String destinationPath) throws IOException {
        try {
            File sourceFile = new File(sourcePath);
            File destinationFile = new File(destinationPath);

            try (InputStream inStream = new FileInputStream(sourceFile); OutputStream outStream = new FileOutputStream(destinationFile)) {
                byte[] buffer = new byte[1024];
                int length;
                long totalBytes = 0;

                while ((length = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, length);
                    totalBytes += length;
                }

                logger.debug("File copy completed. Total bytes: {}", totalBytes);
            }
        } catch (IOException e) {
            logger.error("Failed to copy file from {} to {}", sourcePath, destinationPath, e);
            throw e;
        }
    }

    public void enhanceVideo(String pathToRawRecording) throws VideoEnhancementException, IOException {
        String processId = UUID.randomUUID().toString();
        MDC.put("processId", processId);
        MDC.put("inputFile", pathToRawRecording);

        try {
            logger.info("Starting video enhancement process");

            String recordingName = pathToRawRecording.substring(pathToRawRecording.lastIndexOf("/") + 1);
            sandboxDirPath = System.getenv("HOME") + "/photo-booth-enhancer/" + recordingName.split("\\.")[0];
            MDC.put("sandboxDir", sandboxDirPath);

            logger.debug("Creating sandbox directory: {}", sandboxDirPath);
            boolean isSandboxDirCreated = new File(sandboxDirPath).mkdirs();
            if (!isSandboxDirCreated) {
                logger.error("Failed to create sandbox directory: {}", sandboxDirPath);
                throw new VideoEnhancementException("Unable to create directory " + sandboxDirPath, new Exception("mkdirs nahi chal rha"), pathToRawRecording, "enhanceVideo");
            }

            String pathToRawRecordingSandbox = sandboxDirPath + "/" + recordingName;
            logger.debug("Copying raw recording to sandbox: {}", pathToRawRecordingSandbox);

            cp(pathToRawRecording, pathToRawRecordingSandbox);
            //  TODO: how to handle cases like this ?, should i leave everything to exception handling or do manual error handling like we do i golang
            //  if (!fileCopyStatus) {
            //      throw new RuntimeException("unable to copy raw recording to sandbox");
            //  }

            String rawAudioAndVideoMP4 = convertVideoToMP4(pathToRawRecordingSandbox);
            List<String> rawAudioAndVideo = splitVideoAndAudio(rawAudioAndVideoMP4);
            String rawAudio = rawAudioAndVideo.get(0);
            String rawVideo = rawAudioAndVideo.get(1);

            String volumeBoostedAudio = increaseAudioLevelsBy10x(rawAudio);
            String finalPath = combineVideoAndAudio(rawVideo, volumeBoostedAudio);

            logger.info("Video enhancement completed successfully. Final output: {}", finalPath);
        } catch (Exception e) {
            logger.error("Video enhancement process failed", e);
            throw new VideoEnhancementException("Video enhancement failed", e, pathToRawRecording, "enhanceVideo");
        } finally {
            MDC.clear();
        }
    }
}
