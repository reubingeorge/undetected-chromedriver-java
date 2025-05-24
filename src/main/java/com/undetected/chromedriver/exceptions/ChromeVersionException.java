package com.undetected.chromedriver.exceptions;

public class ChromeVersionException extends RuntimeException {
    public ChromeVersionException(String message) {
        super(message);
    }

    public ChromeVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
