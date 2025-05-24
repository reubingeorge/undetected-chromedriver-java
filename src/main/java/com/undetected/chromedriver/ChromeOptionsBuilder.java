package com.undetected.chromedriver;

import com.undetected.chromedriver.config.DriverConfig;
import com.undetected.chromedriver.utils.RandomUtils;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Builder for creating ChromeOptions with anti-detection features.
 * <p>
 * This builder creates ChromeOptions instances configured with various
 * anti-detection measures to help evade bot detection systems. It applies
 * stealth techniques, randomization, and custom configurations to make the
 * automated browser appear more like a regular user's browser.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Disables automation indicators and flags</li>
 *   <li>Randomizes browser fingerprint elements</li>
 *   <li>Applies stealth preferences and arguments</li>
 *   <li>Supports custom configurations via DriverConfig</li>
 *   <li>Implements window size and user preference randomization</li>
 * </ul>
 *
 * <h2>Anti-Detection Strategies:</h2>
 * <ul>
 *   <li><b>Automation Flag Removal:</b> Removes "enable-automation" and related flags</li>
 *   <li><b>Fingerprint Randomization:</b> Varies user agent, language, window size</li>
 *   <li><b>Feature Disabling:</b> Disables features that reveal automation</li>
 *   <li><b>WebRTC Protection:</b> Prevents IP leaks</li>
 *   <li><b>Preference Randomization:</b> Mimics diverse user settings</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * DriverConfig config = DriverConfig.builder()
 *     .headless(false)
 *     .proxy("http://proxy:8080")
 *     .build();
 *
 * ChromeOptions options = new ChromeOptionsBuilder()
 *     .withConfig(config)
 *     .build();
 *
 * WebDriver driver = new ChromeDriver(options);
 * }</pre>
 *
 * <h2>Builder Pattern Benefits:</h2>
 * <p>
 * The builder pattern allows for fluent configuration and ensures all options
 * are applied in the correct order. Randomization is applied consistently,
 * and conflicts between options are resolved automatically.
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see ChromeOptions
 * @see DriverConfig
 * @see RandomUtils
 */
public class ChromeOptionsBuilder {

    /**
     * The ChromeOptions instance being built.
     * <p>
     * This is the main options object that will be returned after all
     * configurations are applied. It stores Chrome startup arguments and
     * capabilities.
     * </p>
     */
    private final ChromeOptions options;

    /**
     * Experimental options map for Chrome-specific features.
     * <p>
     * Stores experimental Chrome options that are not part of the standard
     * WebDriver protocol. These options control Chrome-specific behavior
     * and are applied via setExperimentalOption().
     * </p>
     *
     * <h3>Common Experimental Options:</h3>
     * <ul>
     *   <li>excludeSwitches: List of Chrome switches to exclude</li>
     *   <li>useAutomationExtension: Controls automation extension</li>
     *   <li>prefs: Chrome preferences</li>
     *   <li>mobileEmulation: Mobile device emulation settings</li>
     * </ul>
     */
    private final Map<String, Object> experimentalOptions;

    /**
     * Chrome preferences map.
     * <p>
     * Stores Chrome user preferences that control browser behavior.
     * These are applied as part of the experimental options under the
     * "prefs" key.
     * </p>
     *
     * <h3>Common Preferences:</h3>
     * <ul>
     *   <li>profile.*: User profile settings</li>
     *   <li>intl.*: Internationalization settings</li>
     *   <li>webrtc.*: WebRTC settings</li>
     * </ul>
     */
    private final Map<String, Object> prefs;

    /**
     * Optional driver configuration.
     * <p>
     * When provided, contains custom settings like headless mode,
     * proxy configuration, and additional arguments. Can be null for
     * default configuration.
     * </p>
     */
    private DriverConfig config;


    /**
     * Constructs a new ChromeOptionsBuilder with default settings.
     * <p>
     * Initializes empty collections for options, experimental options,
     * and preferences. The builder starts with no configuration and
     * builds up options through method calls.
     * </p>
     */
    public ChromeOptionsBuilder() {
        this.options = new ChromeOptions();         // Initialize ChromeOptions instance
        this.experimentalOptions = new HashMap<>(); // Initialize experimental options map
        this.prefs = new HashMap<>();               // Initialize preferences map
    }


    /**
     * Sets the driver configuration for this builder.
     * <p>
     * The provided configuration will be applied when build() is called,
     * allowing customization of headless mode, proxy settings, and other options.
     * </p>
     *
     * @param config the driver configuration to apply, can be null
     * @return this builder instance for method chaining
     */
    public ChromeOptionsBuilder withConfig(DriverConfig config) {
        this.config = config;
        return this;
    }


    /**
     * Builds and returns the configured ChromeOptions instance.
     * <p>
     * This method applies all configurations in a specific order to ensure
     * proper option precedence and avoid conflicts:
     * <ol>
     *   <li>Basic anti-automation options</li>
     *   <li>Stealth options for detection evasion</li>
     *   <li>Randomization for fingerprint diversity</li>
     *   <li>User preferences simulation</li>
     *   <li>Custom configuration (if provided)</li>
     * </ol>
     * </p>
     *
     * <h3>Final Assembly:</h3>
     * <p>
     * After all individual configurations are applied, preferences are
     * packaged into experimental options, and all experimental options
     * are applied to the ChromeOptions instance.
     * </p>
     *
     * @return configured ChromeOptions instance ready for WebDriver creation
     */
    public ChromeOptions build() {
        applyBasicOptions();        // Foundation options
        applyStealthOptions();      // Anti-detection measures
        applyRandomization();       // Fingerprint randomization
        applyUserPreferences();     // User behavior simulation

        // Apply custom configuration if provided
        if (config != null) {
            applyCustomOptions();
        }

        // Apply all experimental options and preferences at once
        // Package preferences into experimental options
        if (!prefs.isEmpty()) {
            experimentalOptions.put("prefs", prefs);
        }

        // Iterate through all experimental options and apply them
        for (Map.Entry<String, Object> entry : experimentalOptions.entrySet()) {
            options.setExperimentalOption(entry.getKey(), entry.getValue());
        }

        return options;
    }


    /**
     * Applies basic anti-automation options.
     * <p>
     * Sets fundamental options to disable automation indicators and
     * configure basic browser behavior. These options form the foundation
     * of the anti-detection strategy.
     * </p>
     *
     * <h3>Key Configurations:</h3>
     * <ul>
     *   <li>Disables automation extension and flags</li>
     *   <li>Configures sandbox and security settings</li>
     *   <li>Sets random window dimensions</li>
     *   <li>Configures alert handling behavior</li>
     * </ul>
     */
    private void applyBasicOptions() {
        // Remove the "Chrome is being controlled by automated software" banner
        experimentalOptions.put("excludeSwitches",
                List.of("enable-automation"));

        // Disable Chrome's automation extension
        experimentalOptions.put("useAutomationExtension", false);

        // Add basic arguments
        options.addArguments(
                "--disable-blink-features=AutomationControlled",        // Hide automation in the Blink engine
                "--disable-dev-shm-usage",                              // Overcome limited resource problems
                "--no-sandbox",                                         // Bypass OS security model
                "--disable-web-security",                               // Disable same-origin policy
                "--disable-features=IsolateOrigins,site-per-process"    // Disable site isolation
        );

        // Prevents automated handling of JavaScript alerts
        options.setCapability("unhandledPromptBehavior", "ignore");

        // Base size: 1200x800, randomized by ±100-200 pixels
        int width = 1200 + ThreadLocalRandom.current().nextInt(-100, 200);
        int height = 800 + ThreadLocalRandom.current().nextInt(-100, 200);
        options.addArguments(String.format("--window-size=%d,%d", width, height));
    }


    /**
     * Applies stealth options for enhanced detection evasion.
     * <p>
     * Configures advanced options to make the browser appear more like
     * a regular user's browser. These options disable various automated
     * behaviors and telemetry features.
     * </p>
     *
     * <h3>Stealth Features:</h3>
     * <ul>
     *   <li>Disables password manager and credentials service</li>
     *   <li>Blocks notification prompts</li>
     *   <li>Disables background throttling</li>
     *   <li>Configures plugin settings</li>
     * </ul>
     */
    private void applyStealthOptions() {
        // Advanced stealth preferences
        prefs.put("credentials_enable_service", false);     // Disable Chrome's password and credential features
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.default_content_setting_values.notifications", 2); // Block notification requests (2 = block)
        prefs.put("profile.default_content_settings.popups", 0);    // Allow popups (0 = allow)

        // Additional anti-detection arguments
        options.addArguments(
                "--disable-background-timer-throttling",        // Prevent timer throttling
                "--disable-backgrounding-occluded-windows",     // Keep background windows active
                "--disable-renderer-backgrounding",             // Prevent renderer backgrounding
                "--disable-features=TranslateUI",               // Disable translation prompts
                "--disable-ipc-flooding-protection"             // Disable IPC flood protection
        );

        // Configure plugin-related preferences
        Map<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("profile.default_content_setting_values.plugins", 1);
        chromePrefs.put("profile.content_settings.plugin_whitelist.adobe-flash-player", 1);
        chromePrefs.put("profile.content_settings.exceptions.plugins.*,*.per_resource.adobe-flash-player", 1);

        // Add plugin preferences to main preferences map
        prefs.putAll(chromePrefs);
    }


    /**
     * Applies randomization to create unique browser fingerprints.
     * <p>
     * Randomizes various browser characteristics to avoid fingerprint-based
     * detection. Each browser instance gets a unique combination of properties
     * to appear as a different user.
     * </p>
     *
     * <h3>Randomized Elements:</h3>
     * <ul>
     *   <li>User agent string</li>
     *   <li>Browser language</li>
     *   <li>Feature flags</li>
     *   <li>Plugin discovery settings</li>
     * </ul>
     */
    private void applyRandomization() {
        // Get randomized user agent from utility
        String userAgent = RandomUtils.getRandomUserAgent();
        options.addArguments("--user-agent=" + userAgent);

        // Get randomized language code
        String lang = RandomUtils.getRandomLanguage();
        options.addArguments("--lang=" + lang);

        // Set browser's accepted languages preference
        prefs.put("intl.accept_languages", lang);

        // 50% chance to disable plugin discovery
        if (ThreadLocalRandom.current().nextBoolean()) {
            options.addArguments("--disable-plugins-discovery");
        }

        // List of Chrome features to potentially disable
        List<String> features = Arrays.asList(
                "VizDisplayCompositor",         // Display compositor
                "NetworkService",               // Network service
                "OutOfBlinkCors"                // CORS handling
        );

        Collections.shuffle(features, ThreadLocalRandom.current()); // Shuffle features randomly

        // Select a random number of features to disable (1 to all)
        int numFeatures = ThreadLocalRandom.current().nextInt(1, features.size());

        // Create comma-separated list of features to disable
        String disableFeatures = String.join(",",
                features.subList(0, numFeatures));
        options.addArguments("--disable-features=" + disableFeatures);
    }


    /**
     * Applies randomized user preferences to simulate real user diversity.
     * <p>
     * Sets various browser preferences with random values to create
     * realistic user profiles. This helps avoid detection based on
     * default or uniform settings.
     * </p>
     *
     * <h3>Randomized Preferences:</h3>
     * <ul>
     *   <li>Media device permissions (microphone, camera)</li>
     *   <li>Geolocation permissions</li>
     *   <li>Zoom level</li>
     *   <li>WebRTC configuration</li>
     * </ul>
     */
    private void applyUserPreferences() {
        // Randomize various preferences
        prefs.put("profile.default_content_setting_values.media_stream_mic",
                ThreadLocalRandom.current().nextInt(1, 3)); // Random microphone permission (1=allow, 2=block)

        prefs.put("profile.default_content_setting_values.media_stream_camera",
                ThreadLocalRandom.current().nextInt(1, 3)); // Random camera permission (1=allow, 2=block)

        prefs.put("profile.default_content_setting_values.geolocation",
                ThreadLocalRandom.current().nextInt(1, 3)); // Random geolocation permission (1=allow, 2=block)

        // Base zoom 1.0 (100%), randomized by ±10%
        double zoomLevel = 1.0 + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2;
        prefs.put("profile.default_zoom_level", zoomLevel);

        // Prevent WebRTC IP leaks
        prefs.put("webrtc.ip_handling_policy", "default_public_interface_only");
        prefs.put("webrtc.multiple_routes_enabled", false);
        prefs.put("webrtc.nonproxied_udp_enabled", false);
    }


    /**
     * Applies custom options from the provided DriverConfig.
     * <p>
     * Processes user-provided configuration including headless mode,
     * proxy settings, custom arguments, and mobile emulation. These
     * options override or supplement the default configurations.
     * </p>
     *
     * <h3>Supported Configurations:</h3>
     * <ul>
     *   <li>Headless mode with additional stealth options</li>
     *   <li>HTTP/SOCKS proxy configuration</li>
     *   <li>Custom Chrome arguments</li>
     *   <li>Custom preferences</li>
     *   <li>User data directory</li>
     *   <li>Mobile device emulation</li>
     * </ul>
     */
    private void applyCustomOptions() {
        // Headless mode
        if (config.isHeadless()) {
            options.addArguments("--headless=new");  // Use new headless mode (Chrome 109+)
            options.addArguments("--window-size=1920,1080"); // Set an explicit window size for headless

            // Additional headless stealth
            options.addArguments(
                    "--disable-gpu",                    // Disable GPU acceleration
                    "--disable-software-rasterizer",    // Disable software rasterizer
                    "--disable-dev-shm-usage"           // Overcome /dev/shm size issues
            );
        }

        // Proxy
        if (config.getProxy() != null) {
            options.addArguments("--proxy-server=" + config.getProxy()); // Set proxy server
        }

        // Custom arguments
        if (config.getAdditionalArguments() != null) {
            options.addArguments(config.getAdditionalArguments()); // Add all user-provided arguments
        }

        // Custom preferences
        if (config.getAdditionalPreferences() != null) {
            prefs.putAll(config.getAdditionalPreferences()); // Merge user preferences with existing ones
        }

        // User data directory
        if (config.getUserDataDir() != null) {
            options.addArguments("--user-data-dir=" + config.getUserDataDir()); // Set custom profile directory
        }

        // Additional mobile emulation option
        if (config.isMobileEmulation()) {
            Map<String, Object> mobileEmulation = new HashMap<>();
            mobileEmulation.put("deviceName", "Nexus 5"); // Emulate Nexus 5 device
            experimentalOptions.put("mobileEmulation", mobileEmulation); // Add mobile emulation to experimental options
        }
    }
}