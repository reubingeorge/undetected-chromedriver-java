package com.undetected.chromedriver.config;

import com.undetected.chromedriver.HumanBehaviorSimulator;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Configuration for UndetectedChromeDriver.
 */
@Data
@Builder
public class DriverConfig {

    @Builder.Default
    private boolean headless = false;

    @Builder.Default
    private boolean requireLatestChrome = true;

    @Builder.Default
    private boolean strictVersionCheck = false;

    @Builder.Default
    private boolean randomizeFingerprint = true;

    @Builder.Default
    private boolean mobileEmulation = false;

    @Builder.Default
    private boolean humanBehavior = true;  // Enable human behavior by default

    @Builder.Default
    private int implicitWaitMs = 10000;

    @Builder.Default
    private int driverPort = 0; // 0 means random port

    @Builder.Default
    private boolean verboseLogging = false;

    @Builder.Default
    private boolean forceDriverDownload = false;

    @Builder.Default
    private HumanBehaviorSimulator.BehaviorProfile behaviorProfile =
            HumanBehaviorSimulator.BehaviorProfile.NORMAL;

    private String proxy;

    private String userDataDir;

    private String driverCachePath;

    private String logFile;

    private String buildPath;

    private List<String> additionalArguments;

    private Map<String, Object> additionalPreferences;

    private Map<String, String> environmentVariables;

    @Builder.Default
    private StealthConfig stealthConfig = StealthConfig.builder().build();
}