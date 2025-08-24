package com.example.debate.stream;

public class StreamEvents {
    public record Token(String type, String requestId, String source, String token, int chunkIx) {}
    public record Status(String type, String requestId, String stage, String detail) {}
    public record Error(String type, String requestId, String message) {}
    public record Done(String type, String requestId) {}
    public record FinalResult(String type, String requestId, Object result) {}
}
