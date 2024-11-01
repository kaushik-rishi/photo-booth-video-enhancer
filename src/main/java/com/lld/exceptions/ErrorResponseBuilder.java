package com.lld.exceptions;

import java.util.Map;

public class ErrorResponseBuilder {
    public static Map<String, Object> buildErrorResponse(VideoEnhancementException vex) {
        return Map.of(
                "status", "error",
                "message", vex.getMessage(),
                "details", Map.of(
                        "videoPath", vex.getVideoPath(),
                        "operation", vex.getOperation()
                )
        );
    }
}
