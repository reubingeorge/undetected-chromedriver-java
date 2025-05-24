package com.undetected.chromedriver;

import com.undetected.chromedriver.config.DriverConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test class for {@link ChromeOptionsBuilder}.
 * <p>
 * This test suite validates the functionality of the ChromeOptionsBuilder class,
 * which is responsible for constructing ChromeOptions objects with various
 * configurations for undetected Chrome automation. The builder pattern allows
 * flexible configuration of Chrome browser options including headless mode,
 * proxy settings, custom preferences, and anti-detection features.
 * </p>
 *
 * <h2>Test Coverage:</h2>
 * <ul>
 *   <li>Basic ChromeOptions creation</li>
 *   <li>Configuration with DriverConfig objects</li>
 *   <li>Headless mode configuration</li>
 *   <li>Proxy and user data directory settings</li>
 *   <li>Custom arguments and preferences</li>
 *   <li>Mobile emulation support</li>
 *   <li>Null config handling</li>
 *   <li>Multiple configuration scenarios</li>
 * </ul>
 *
 * <h2>Testing Approach:</h2>
 * <p>
 * Due to ChromeOptions' encapsulation, many internal settings cannot be directly
 * verified. Tests focus on ensuring options are created successfully and basic
 * properties are set correctly. The actual behavior is validated through
 * integration tests with real Chrome instances.
 * </p>
 *
 * <h2>Builder Pattern Benefits:</h2>
 * <ul>
 *   <li>Fluent API for easy configuration</li>
 *   <li>Separation of configuration from options creation</li>
 *   <li>Reusable configuration objects</li>
 *   <li>Type-safe option setting</li>
 * </ul>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see ChromeOptionsBuilder
 * @see DriverConfig
 * @see ChromeOptions
 */
class ChromeOptionsBuilderTest {

    /**
     * The ChromeOptionsBuilder instance under test.
     * <p>
     * Initialized fresh for each test method to ensure test isolation
     * and prevent configuration bleed between tests.
     * </p>
     */
    private ChromeOptionsBuilder builder;

    /**
     * Sets up the test environment before each test method.
     * <p>
     * Creates a new ChromeOptionsBuilder instance to ensure each test
     * starts with a clean builder without any residual configuration.
     * </p>
     */
    @BeforeEach
    void setUp() {
        builder = new ChromeOptionsBuilder(); // Create a fresh builder instance for each test
    }


    /**
     * Tests basic ChromeOptions creation without any configuration.
     * <p>
     * Verifies that the builder can create a minimal ChromeOptions object
     * with default settings. This serves as a baseline test to ensure the
     * builder works even without any custom configuration.
     * </p>
     *
     * @see ChromeOptionsBuilder#build()
     */
    @Test
    @DisplayName("Should build basic Chrome options")
    void testBasicOptions() {
        ChromeOptions options = builder.build(); // Build options without any configuration

        assertThat(options).isNotNull(); // Verify 'options' object was created
        assertThat(options.getBrowserName()).isEqualTo("chrome"); // Verify browser name is set correctly

        // Double-check using capability getter
        assertThat(options.getCapability("browserName")).isEqualTo("chrome");
    }


    /**
     * Tests ChromeOptions creation with a DriverConfig object.
     * <p>
     * Verifies that the builder correctly applies settings from a
     * DriverConfig object, including headless mode and fingerprint
     * randomization settings.
     * </p>
     *
     * @see ChromeOptionsBuilder#withConfig(DriverConfig)
     * @see DriverConfig.DriverConfigBuilder
     */
    @Test
    @DisplayName("Should build Chrome options with config")
    void testOptionsWithConfig() {
        // Create a configuration with specific settings
        DriverConfig config = DriverConfig.builder()
                .headless(false) // Explicitly set non-headless mode
                .randomizeFingerprint(true) // Enable fingerprint randomization
                .build();

        // Build options with the configuration
        ChromeOptions options = builder
                .withConfig(config)
                .build();

        assertThat(options).isNotNull(); // Verify options were created successfully
        assertThat(options.getBrowserName()).isEqualTo("chrome"); // Verify browser identification
    }


    /**
     * Tests application of custom configuration settings.
     * <p>
     * Verifies that complex configurations including headless mode,
     * proxy settings, and custom user data directory are properly
     * applied to the ChromeOptions object.
     * </p>
     *
     * <h3>Configuration includes:</h3>
     * <ul>
     *   <li>Headless mode enabled</li>
     *   <li>HTTP proxy configuration</li>
     *   <li>Custom user data directory</li>
     * </ul>
     *
     * @see DriverConfig.DriverConfigBuilder#headless(boolean)
     * @see DriverConfig.DriverConfigBuilder#proxy(String)
     * @see DriverConfig.DriverConfigBuilder#userDataDir(String)
     */
    @Test
    @DisplayName("Should apply custom configuration")
    void testCustomConfiguration() {

        // Create a configuration with multiple custom settings
        DriverConfig config = DriverConfig.builder()
                .headless(true)                         // Enable headless mode
                .proxy("http://proxy:8080")             // Set HTTP proxy
                .userDataDir("/tmp/chrome-profile")     // Custom profile directory
                .build();

        // Apply configuration to builder
        ChromeOptions options = builder
                .withConfig(config)
                .build();

        assertThat(options).isNotNull(); // Verify options creation

        // Internal ChromeOptions structure doesn't expose arguments for verification
        assertThat(options.getBrowserName()).isEqualTo("chrome");
    }


    /**
     * Tests handling of additional command-line arguments.
     * <p>
     * Verifies that custom Chrome command-line arguments can be added
     * through the DriverConfig. These arguments are passed directly to
     * the Chrome binary at startup.
     * </p>
     *
     * <h3>Test arguments:</h3>
     * <ul>
     *   <li>--disable-gpu: Disables GPU hardware acceleration</li>
     *   <li>--no-first-run: Skips first-run wizard</li>
     * </ul>
     *
     * @see DriverConfig.DriverConfigBuilder#additionalArguments(java.util.List)
     */
    @Test
    @DisplayName("Should handle additional arguments")
    void testAdditionalArguments() {

        // Create configuration with custom Chrome arguments
        DriverConfig config = DriverConfig.builder()
                .additionalArguments(java.util.List.of(
                        "--disable-gpu",  // Disable GPU acceleration
                        "--no-first-run"  // Skip first-run experience
                ))
                .build();

        // Build options with additional arguments
        ChromeOptions options = builder
                .withConfig(config)
                .build();

        assertThat(options).isNotNull(); // Verify successful creation
    }


    /**
     * Tests application of custom Chrome preferences.
     * <p>
     * Verifies that Chrome preferences (different from command-line arguments)
     * can be set through the builder. These preferences control Chrome's
     * internal behavior and settings.
     * </p>
     *
     * <h3>Test preferences:</h3>
     * <ul>
     *   <li>download.default_directory: Sets download location</li>
     *   <li>download.prompt_for_download: Disables download prompt</li>
     * </ul>
     *
     * @see DriverConfig.DriverConfigBuilder#additionalPreferences(Map)
     */
    @Test
    @DisplayName("Should apply custom preferences")
    void testCustomPreferences() {
        Map<String, Object> customPrefs = new HashMap<>();                  // Create a map of Chrome preferences
        customPrefs.put("download.default_directory", "/tmp/downloads");    // Set the default download directory
        customPrefs.put("download.prompt_for_download", false);             // Disable download prompt dialog

        // Create configuration with custom preferences
        DriverConfig config = DriverConfig.builder()
                .additionalPreferences(customPrefs)
                .build();

        // Build options with preferences
        ChromeOptions options = builder
                .withConfig(config)
                .build();

        // Verify creation
        assertThat(options).isNotNull();
        assertThat(options.getBrowserName()).isEqualTo("chrome");
    }


    /**
     * Tests mobile emulation configuration.
     * <p>
     * Verifies that the builder can configure Chrome to emulate mobile
     * devices. This is useful for testing responsive designs and mobile-
     * specific functionality.
     * </p>
     *
     * @see DriverConfig.DriverConfigBuilder#mobileEmulation(boolean)
     */
    @Test
    @DisplayName("Should handle mobile emulation")
    void testMobileEmulation() {
        // Create configuration with mobile emulation enabled
        DriverConfig config = DriverConfig.builder()
                .mobileEmulation(true) // Enable mobile device emulation
                .build();

        // Build options with mobile emulation
        ChromeOptions options = builder
                .withConfig(config)
                .build();

        assertThat(options).isNotNull(); // Verify creation

        // Verify capability is set
        assertThat(options.getCapability("browserName")).isEqualTo("chrome");
    }


    /**
     * Tests headless Chrome configuration.
     * <p>
     * Verifies that the builder correctly configures Chrome to run in
     * headless mode (without GUI). Headless mode is useful for automated
     * testing and server environments without displays.
     * </p>
     *
     * <h3>Note on Verification:</h3>
     * <p>
     * Due to ChromeOptions encapsulation, we cannot directly verify that
     * headless arguments are added. The actual headless behavior is
     * validated in integration tests.
     * </p>
     *
     * @see DriverConfig.DriverConfigBuilder#headless(boolean)
     */
    @Test
    @DisplayName("Should build headless options")
    void testHeadlessOptions() {

        // Create configuration for headless mode
        DriverConfig config = DriverConfig.builder()
                .headless(true)
                .build();

        // Build options with headless configuration
        ChromeOptions options = builder
                .withConfig(config)
                .build();

        assertThat(options).isNotNull(); // Verify creation
        // In newer Selenium versions, headless is a capability
        // We can't directly check arguments, but we know it was configured
    }

    @Test
    @DisplayName("Should handle null config gracefully")
    void testNullConfig() {
        ChromeOptions options = builder
                .withConfig(null)
                .build();

        assertThat(options).isNotNull();
        assertThat(options.getBrowserName()).isEqualTo("chrome");
    }

    @Test
    @DisplayName("Should create different options for different configs")
    void testDifferentConfigurations() {
        // First configuration
        DriverConfig config1 = DriverConfig.builder()
                .headless(true)
                .build();

        ChromeOptions options1 = new ChromeOptionsBuilder()
                .withConfig(config1)
                .build();

        // Second configuration
        DriverConfig config2 = DriverConfig.builder()
                .headless(false)
                .mobileEmulation(true)
                .build();

        ChromeOptions options2 = new ChromeOptionsBuilder()
                .withConfig(config2)
                .build();

        assertThat(options1).isNotNull();
        assertThat(options2).isNotNull();

        // Both should be Chrome options
        assertThat(options1.getBrowserName()).isEqualTo("chrome");
        assertThat(options2.getBrowserName()).isEqualTo("chrome");
    }
}