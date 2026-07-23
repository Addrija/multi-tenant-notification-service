package com.notification.service.channel;

public record SendResult(boolean success, String errorMessage) {
    public static SendResult ok() {
        return new SendResult(true, null);
    }

    public static SendResult failure(String errorMessage) {
        return new SendResult(false, errorMessage);
    }
}