package com.lld.exceptions;

/**
 * Custom exception for video enhancement related exceptions
 * Extends `RuntimeException` - reason: video processing errors are typically unrecoverable
 * and should not force explicit exception handling
 */
public class VideoEnhancementException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String videoPath;
    private String operation;

    public VideoEnhancementException(String errorMessage) {
        super(errorMessage);
    }

    public VideoEnhancementException(String errorMessage, Throwable cause, String videoPath, String operation) {
        this(errorMessage, cause);
        this.videoPath = videoPath;
        this.operation = operation;
    }

    public VideoEnhancementException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

    public VideoEnhancementException(Throwable cause) {
        super(cause);
    }

    public String getVideoPath() {
        return videoPath;
    }

    public String getOperation() {
        return operation;
    }

    // remove it when you migrate to spring's `GlobalExceptionHandler`
    @Override
    public String toString() {
        return String.format("VideoEnhancementException{message='%s', videoPath='%s', operation='%s'}", getMessage(), this.videoPath, this.operation);
    }
}

/*
  - why runtime exceptions don't need explicit exception handling ?
  - what's with the serialVersionUID, that too a private field ? of what use is it even ?
 */
