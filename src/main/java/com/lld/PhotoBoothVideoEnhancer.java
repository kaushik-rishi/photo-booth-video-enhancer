package com.lld;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class PhotoBoothVideoEnhancer {
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;
    private String sandboxDirPath;

    public PhotoBoothVideoEnhancer(FFmpeg ffmpeg, FFprobe ffprobe) throws IOException {
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
    }

    private String convertVideoToMP4(String rawInputMovFilePath) {
        FFmpegBuilder command = new FFmpegBuilder();
//        String rawInputMP4FilePath = rawInputMovFilePath.split("\\.", 2)[0] + ".mp4";
        command.addInput(rawInputMovFilePath)
                .addOutput(sandboxDirPath + "/raw-video-and-audio.mp4");
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(command).run();
        return sandboxDirPath + "/raw-video-and-audio.mp4";
    }

    private List<String> splitVideoAndAudio(String rawVideoAndAudio) {
        FFmpegBuilder command = new FFmpegBuilder();
        // ffmpeg -i raw-video.mp4 -q:a 0 -map a te-amo-audio-raw.mp3
        command.addInput(rawVideoAndAudio)
                .addOutput(sandboxDirPath + "/raw-audio.mp3")
                .addExtraArgs("-map", "a")
                .addExtraArgs("-q:a", "0");
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(command).run();

        // ffmpeg -i raw-video.mp4 -an vcodec copy te-amo-video-raw.mp4
        command = new FFmpegBuilder();
        command.addInput(rawVideoAndAudio)
                .addOutput(sandboxDirPath + "/raw-video.mp4")
                .setVideoCodec("copy")
                .disableAudio();

        executor.createJob(command).run();
        return Arrays.asList(sandboxDirPath + "/raw-audio.mp3", sandboxDirPath + "/raw-video.mp4");
    }

    private String increaseAudioLevelsBy10x(String rawAudio) {
        FFmpegBuilder command = new FFmpegBuilder();
        // ffmpeg -i te-amo-audio-raw.mp3 -filter:a "volume=10" te-amo-audio-2x.mp3
        command.addInput(rawAudio)
                .addOutput(sandboxDirPath + "/raw-audio-10x.mp3")
                .setAudioFilter("volume=10");

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(command).run();
        return sandboxDirPath + "/raw-audio-10x.mp3";
    }

    private String combineVideoAndAudio(String rawVideo, String enhancedAudio) {
        FFmpegBuilder command = new FFmpegBuilder();

        // ffmpeg -i raw-video.mp4 -i raw-audio-10x.mp3 -c:v copy -c:a aac final.mp4
        command.addInput(rawVideo)
                .addInput(enhancedAudio)
                .addOutput(sandboxDirPath + "/final.mp4")
                .setVideoCodec("copy")
                .setAudioCodec("aac");

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(command).run();

        return sandboxDirPath + "/final.mp4";
    }

    private boolean cp(String sourcePath, String destinationPath) throws IOException {
        File sourceFile = new File(sourcePath);
        File destinationFile = new File(destinationPath);

        try (InputStream inStream = new FileInputStream(sourceFile);
             OutputStream outStream = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }
        }

        return true;
    }

    public void enhanceVideo(String pathToRawRecording) throws IOException {
        String recordingName = pathToRawRecording.substring(pathToRawRecording.lastIndexOf("/") + 1);
        sandboxDirPath = System.getenv("HOME") + "/photo-booth-enhancer/" + recordingName.split("\\.")[0];

        boolean isSandboxDirCreated = new File(sandboxDirPath).mkdirs();
        if (!isSandboxDirCreated) {
            throw new IOException("unable to create directory" + sandboxDirPath);
        }

        String pathToRawRecordingSandbox = sandboxDirPath + "/" + recordingName;
        boolean fileCopyStatus = cp(pathToRawRecording, pathToRawRecordingSandbox);
        if (!fileCopyStatus) {
            throw new RuntimeException("unable to copy raw recording to sandbox");
        }

        String rawAudioAndVideoMP4 = convertVideoToMP4(pathToRawRecordingSandbox);
        List<String> rawAudioAndVideo = splitVideoAndAudio(rawAudioAndVideoMP4);
        String rawAudio = rawAudioAndVideo.get(0);
        String rawVideo = rawAudioAndVideo.get(1);

        String volumeBoostedAudio = increaseAudioLevelsBy10x(rawAudio);
        String finalPath = combineVideoAndAudio(rawVideo, volumeBoostedAudio);
        System.out.println(finalPath);
    }
}
