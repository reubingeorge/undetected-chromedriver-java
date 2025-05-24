package com.undetected.chromedriver;

import com.undetected.chromedriver.exceptions.PatchingException;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified ChromeDriverPatcher that uses WebDriverManager.
 * No longer patches binaries to avoid corruption.
 * <p>
 * This class serves as a wrapper around WebDriverManager to automatically download
 * and manage ChromeDriver executables for different Chrome versions. Unlike previous
 * implementations that attempted to patch ChromeDriver binaries to evade detection,
 * this simplified version only handles driver acquisition and caching.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Automatic ChromeDriver download via WebDriverManager</li>
 *   <li>Thread-safe caching of driver paths</li>
 *   <li>Version-specific driver management</li>
 *   <li>No binary patching (prevents driver corruption)</li>
 * </ul>
 *
 * <h2>Deprecation Notice:</h2>
 * <p>
 * This class is marked as {@link Deprecated} because binary patching has been
 * abandoned in favor of runtime evasion techniques. The functionality has been
 * reduced to simple driver management, which can be handled directly by
 * WebDriverManager in most cases.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * ChromeDriverPatcher patcher = new ChromeDriverPatcher();
 * String driverPath = patcher.getPatchedDriver("120.0.6099.109");
 * System.setProperty("webdriver.chrome.driver", driverPath);
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * This class is thread-safe. Multiple threads can safely request drivers for
 * different Chrome versions simultaneously.
 * </p>
 *
 * @author Reubin George
 * @version 2.0
 * @since 1.0
 * @deprecated Binary patching has been replaced with runtime evasion techniques.
 *             Consider using WebDriverManager directly or UndetectedChromeDriver
 *             for anti-detection capabilities.
 * @see WebDriverManager
 * @see UndetectedChromeDriver
 */
@Slf4j
@Deprecated
public class ChromeDriverPatcher {

    /**
     * Thread-safe cache for storing downloaded ChromeDriver paths.
     * <p>
     * Uses {@link ConcurrentHashMap} to ensure thread-safe access when multiple
     * threads request drivers simultaneously. The map uses Chrome version strings
     * as keys and the corresponding ChromeDriver executable paths as values.
     * </p>
     * <p>
     * This caching mechanism prevents redundant downloads of the same ChromeDriver
     * version and improves performance in applications that create multiple
     * driver instances.
     * </p>
     *
     * <h3>Cache Structure:</h3>
     * <ul>
     *   <li>Key: Chrome version string (e.g., "120.0.6099.109")</li>
     *   <li>Value: Absolute path to ChromeDriver executable</li>
     * </ul>
     */
    private final ConcurrentHashMap<String, String> cachedDrivers;


    /**
     * Constructs a new ChromeDriverPatcher instance.
     * <p>
     * Initializes the internal cache for storing ChromeDriver paths.
     * The cache starts empty and is populated lazily as drivers are requested.
     * </p>
     *
     * <h3>Post-construction state:</h3>
     * <ul>
     *   <li>Empty driver cache</li>
     *   <li>Ready to handle driver requests</li>
     * </ul>
     */
    public ChromeDriverPatcher() {
        this.cachedDrivers = new ConcurrentHashMap<>(); // Initialize the concurrent cache for driver paths
    }

    /**
     * Get ChromeDriver for the specified version using WebDriverManager.
     * <p>
     * This method retrieves the appropriate ChromeDriver executable for the given
     * Chrome version. If the driver has been previously downloaded, it returns
     * the cached path. Otherwise, it uses WebDriverManager to download the driver
     * and caches the result for future use.
     * </p>
     *
     * <h3>Process Flow:</h3>
     * <ol>
     *   <li>Check if driver path exists in cache</li>
     *   <li>If not cached, use WebDriverManager to download</li>
     *   <li>Cache the downloaded driver path</li>
     *   <li>Return the driver path</li>
     * </ol>
     *
     * <h3>WebDriverManager Configuration:</h3>
     * <p>
     * The method configures WebDriverManager to:
     * <ul>
     *   <li>Download ChromeDriver (not other browser drivers)</li>
     *   <li>Match the specified Chrome browser version</li>
     *   <li>Handle version resolution automatically</li>
     * </ul>
     * </p>
     *
     * <h3>Error Handling:</h3>
     * <p>
     * Any exceptions during driver download or setup are wrapped in a
     * {@link PatchingException} with the original exception as the cause.
     * Common failure scenarios include:
     * <ul>
     *   <li>Network connectivity issues</li>
     *   <li>Invalid Chrome version format</li>
     *   <li>No matching ChromeDriver available</li>
     *   <li>File system permissions issues</li>
     * </ul>
     * </p>
     *
     * @param chromeVersion Chrome version string (e.g., "120.0.6099.109", "120", or "stable")
     * @return absolute path to the ChromeDriver executable suitable for the specified Chrome version
     * @throws PatchingException if driver download or setup fails for any reason
     * @see WebDriverManager#chromedriver()
     * @see ConcurrentHashMap#computeIfAbsent(Object, java.util.function.Function)
     */
    public String getPatchedDriver(String chromeVersion) {
        // Use computeIfAbsent for atomic cache check and update
        // This ensures thread-safe caching without explicit synchronization
        return cachedDrivers.computeIfAbsent(chromeVersion, version -> {
            try {
                // Log the driver request for debugging
                log.info("Getting ChromeDriver for Chrome version: {}", version);

                // Create a WebDriverManager instance specifically for ChromeDriver
                WebDriverManager wdm = WebDriverManager.chromedriver()
                        .browserVersion(version);

                // Trigger driver download and setup
                // This will download the driver if not present or return a cached path
                wdm.setup();

                // Get the absolute path to the downloaded driver executable
                String driverPath = wdm.getDownloadedDriverPath();

                // Log successful driver acquisition
                log.info("ChromeDriver path: {}", driverPath);
                return driverPath; // Return path for caching and use

            } catch (Exception e) {

                // Wrap any exception in PatchingException for consistent error handling
                // This maintains API compatibility while providing exception chaining
                throw new PatchingException("Failed to get driver", e);
            }
        });
    }
}