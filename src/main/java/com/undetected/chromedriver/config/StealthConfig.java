package com.undetected.chromedriver.config;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for stealth features.
 */
@Data
@Builder
public class StealthConfig {

    @Builder.Default
    private boolean randomizeUserAgent = true;

    @Builder.Default
    private boolean randomizeViewport = true;

    @Builder.Default
    private boolean randomizeLanguage = true;

    @Builder.Default
    private boolean randomizeTimezone = true;

    @Builder.Default
    private boolean randomizeCanvas = true;

    @Builder.Default
    private boolean randomizeWebGL = true;

    @Builder.Default
    private boolean randomizeAudio = true;

    @Builder.Default
    private boolean randomizeFonts = true;

    @Builder.Default
    private boolean randomizeBattery = true;

    @Builder.Default
    private boolean randomizeHardware = true;

    @Builder.Default
    private int fingerprintRandomizationIntervalSeconds = 120;
}