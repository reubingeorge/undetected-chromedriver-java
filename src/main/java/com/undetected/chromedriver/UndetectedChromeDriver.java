package com.undetected.chromedriver;

import com.undetected.chromedriver.config.DriverConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.http.ClientConfig;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Thread-safe Undetected ChromeDriver implementation that bypasses bot detection systems.
 * Multiple instances can be created and used concurrently without conflicts.
 * Now includes human behavior simulation to avoid detection from behavioral analysis.
 * <p>
 * This is the main class of the Undetected ChromeDriver library. It extends the standard
 * Selenium ChromeDriver with multiple anti-detection features including stealth JavaScript
 * injection, fingerprint randomization, human behavior simulation, and bot detection
 * handling. The implementation is fully thread-safe and supports concurrent instances.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Automatic ChromeDriver management via WebDriverManager</li>
 *   <li>Stealth JavaScript injection to hide automation indicators</li>
 *   <li>Browser fingerprint randomization</li>
 *   <li>Human-like behavior simulation (mouse, keyboard, scrolling)</li>
 *   <li>Bot detection monitoring and handling</li>
 *   <li>Thread-safe concurrent instance support</li>
 *   <li>Configurable via DriverConfig</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Default configuration
 * UndetectedChromeDriver driver = new UndetectedChromeDriver();
 *
 * // Custom configuration
 * DriverConfig config = DriverConfig.builder()
 *     .headless(false)
 *     .humanBehavior(true)
 *     .randomizeFingerprint(true)
 *     .build();
 * UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
 *
 * // Use like regular WebDriver with human-like methods
 * driver.get("https://example.com");
 * WebElement button = driver.findElement(By.id("submit"));
 * driver.humanClick(button);
 *
 * driver.quit();
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * The class uses atomic operations, synchronized blocks, and concurrent collections
 * to ensure thread safety. Multiple instances can be created and used from different
 * threads without conflicts.
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see ChromeDriver
 * @see DriverConfig
 */
@Slf4j
public class UndetectedChromeDriver extends ChromeDriver {

    /**
     * Thread-safe counter for unique instance IDs.
     * <p>
     * Generates sequential instance IDs for logging and identification.
     * Uses AtomicInteger to ensure thread-safe increment operations.
     * </p>
     */
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger(0);

    /**
     * Shared executor for background tasks (thread-safe).
     * <p>
     * Single shared thread pool for all driver instances to execute background
     * tasks like stealth application and fingerprint randomization. Uses daemon
     * threads to avoid preventing JVM shutdown.
     * </p>
     *
     * <h3>Thread Pool Configuration:</h3>
     * <ul>
     *   <li>Size: Number of available processors</li>
     *   <li>Type: Scheduled thread pool for delayed/periodic tasks</li>
     *   <li>Threads: Daemon threads with descriptive names</li>
     * </ul>
     */
    private static final ScheduledExecutorService SHARED_EXECUTOR = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "UndetectedChromeDriver-Worker");
                t.setDaemon(true); // Daemon threads won't prevent JVM shutdown
                return t;
            }
    );

    /**
     * Lock for WebDriverManager setup (only one thread should set it up at a time).
     * <p>
     * Ensures thread-safe initialization of WebDriverManager to prevent
     * concurrent download attempts of ChromeDriver binaries.
     * </p>
     */
    private static final Object WDM_SETUP_LOCK = new Object();

    /**
     * Flag indicating if WebDriverManager has been initialized.
     * <p>
     * Uses AtomicBoolean for thread-safe check-and-set operations.
     * </p>
     */
    private static final AtomicBoolean WDM_INITIALIZED = new AtomicBoolean(false);

    /**
     *  Get the unique instance ID for this driver.
     *
     * Unique identifier for this driver instance.
     * <p>
     * Used for logging and distinguishing between multiple concurrent instances.
     * </p>
     *
     * @return the instance ID
     */
    @Getter
    private final int instanceId;

    /**
     * Configuration for this driver instance.
     * <p>
     * Contains all configuration options like headless mode, proxy settings,
     * behavior profiles, etc.
     * </p>
     */
    private final DriverConfig config;

    /**
     * Executor for JavaScript stealth techniques.
     * <p>
     * Applies various JavaScript modifications to hide automation indicators.
     * </p>
     */
    private final StealthExecutor stealthExecutor;

    /**
     * Randomizer for browser fingerprints.
     * <p>
     * Periodically changes browser fingerprints to avoid tracking.
     * </p>
     */
    private final FingerprintRandomizer fingerprintRandomizer;

    /**
     *  Get the human behavior simulator.
     *
     * Simulator for human-like interactions.
     * <p>
     * Provides realistic mouse movements, typing, scrolling, and timing.
     * </p>
     *
     * @return the human behavior simulator
     */
    @Getter
    private final HumanBehaviorSimulator humanBehavior;

    /**
     *  Get the bot detection handler.
     *
     * Handler for bot detection challenges.
     * <p>
     * Monitors for and handles various bot detection systems like Cloudflare.
     * </p>
     *
     * @return the bot detection handler
     */
    private final BotDetectionHandler botDetectionHandler;

    /**
     * Flag indicating if this instance has been initialized.
     * <p>
     * Uses AtomicBoolean to ensure initialization happens only once.
     * </p>
     */
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    /**
     * Flag indicating if this instance is quitting.
     * <p>
     * Prevents operations on a driver that's shutting down.
     * </p>
     */
    private final AtomicBoolean isQuitting = new AtomicBoolean(false);

    /**
     * Static initialization block.
     * <p>
     * Registers a shutdown hook to properly cleanup the shared executor
     * when the JVM exits.
     * </p>
     */
    static {
        // Register shutdown hook to cleanup executor
        Runtime.getRuntime().addShutdownHook(new Thread(SHARED_EXECUTOR::shutdownNow));
    }

    /**
     * Initialize WebDriverManager once in a thread-safe manner.
     * <p>
     * Uses double-checked locking pattern to ensure WebDriverManager is
     * initialized exactly once across all threads. This prevents concurrent
     * download attempts which could cause conflicts.
     * </p>
     */
    private static void initializeWebDriverManager() {
        if (!WDM_INITIALIZED.get()) { // First check without locking (performance optimization)
            synchronized (WDM_SETUP_LOCK) { // Acquire lock for initialization
                if (!WDM_INITIALIZED.get()) {   // Double-check inside a synchronized block
                    log.info("Initializing WebDriverManager");
                    WebDriverManager.chromedriver().setup(); // Setup ChromeDriver management
                    WDM_INITIALIZED.set(true); // Mark as initialized
                }
            }
        }
    }

    /**
     * Creates an UndetectedChromeDriver with default configuration.
     * <p>
     * Uses default DriverConfig settings for quick setup.
     * </p>
     */
    public UndetectedChromeDriver() {
        this(DriverConfig.builder().build());
    }

    /**
     * Creates an UndetectedChromeDriver with custom configuration.
     * <p>
     * This is the main constructor that sets up all components, including
     * the ChromeDriver service, options, and anti-detection features.
     * </p>
     *
     * @param config the driver configuration
     */
    public UndetectedChromeDriver(DriverConfig config) {
        // Call parent constructor with custom service and options
        super(
                createService(config),
                createOptions(config),
                ClientConfig.defaultConfig()
        );

        // Assign unique instance ID
        this.instanceId = INSTANCE_COUNTER.incrementAndGet();
        this.config = config;

        // Initialize anti-detection components
        this.stealthExecutor = new StealthExecutor(this, instanceId);
        this.fingerprintRandomizer = new FingerprintRandomizer(this, instanceId);
        this.humanBehavior = new HumanBehaviorSimulator(this, instanceId);
        this.botDetectionHandler = new BotDetectionHandler(this, instanceId);

        log.info("Created UndetectedChromeDriver instance #{}", instanceId);

        initialize(); // Perform post-construction initialization
    }

    /**
     * Creates a ChromeDriverService with configuration.
     * <p>
     * Sets up the ChromeDriver executable, port, logging, and environment.
     * Ensures WebDriverManager is initialized and ChromeDriver is downloaded.
     * </p>
     *
     * @param config the driver configuration
     * @return configured ChromeDriverService
     * @throws RuntimeException if service creation fails
     */
    private static ChromeDriverService createService(DriverConfig config) {
        try {
            // Ensure WebDriverManager is initialized
            initializeWebDriverManager();

            // Create an instance-specific service
            String currentVersion = null;

            // Detect the current Chrome version if required
            if (config.isRequireLatestChrome()) {
                try {
                    ChromeVersionManager versionManager = new ChromeVersionManager();
                    currentVersion = versionManager.getCurrentVersion();
                    log.info("Detected Chrome version: {}", currentVersion);
                } catch (IOException e) {
                    log.warn("Could not detect Chrome version: {}", e.getMessage());
                }
            }

            // Thread-safe WebDriverManager usage
            String driverPath;
            synchronized (WDM_SETUP_LOCK) {
                WebDriverManager wdm = WebDriverManager.chromedriver(); // Configure WebDriverManager

                if (currentVersion != null) { // Set Chrome version if detected
                    wdm.browserVersion(currentVersion);
                }

                if (config.getProxy() != null && !config.getProxy().isEmpty()) { // Configure proxy if specified
                    wdm.proxy(config.getProxy());
                }

                if (config.getDriverCachePath() != null) { // Set custom cache path if specified
                    wdm.cachePath(config.getDriverCachePath());
                }

                if (config.isForceDriverDownload()) { // Force download if requested
                    wdm.forceDownload();
                }

                wdm.setup(); // Setup and get a driver path
                driverPath = wdm.getDownloadedDriverPath();
            }

            log.info("Using ChromeDriver from: {}", driverPath);

            File driverFile = new File(driverPath); // Verify driver exists
            if (!driverFile.exists()) {
                throw new IOException("ChromeDriver not found at: " + driverPath);
            }

            // Ensure executable permissions on Unix systems
            if (!driverFile.canExecute() && !System.getProperty("os.name").toLowerCase().contains("win")) {
                driverFile.setExecutable(true);
            }

            // Create service with unique port for each instance
            ChromeDriverService.Builder serviceBuilder = new ChromeDriverService.Builder()
                    .usingDriverExecutable(driverFile)
                    .usingPort(config.getDriverPort()) // 0 for random port
                    .withSilent(!config.isVerboseLogging());

            // Instance-specific log file
            if (config.getLogFile() != null) {
                serviceBuilder.withLogFile(new File(config.getLogFile())); // Use configured a log file
            } else if (!config.isVerboseLogging()) {
                File logFile = new File(System.getProperty("java.io.tmpdir"), // Create unique log file in temp directory
                        String.format("chromedriver_%d_%d.log",
                                System.currentTimeMillis(),
                                ThreadLocalRandom.current().nextInt(1000)));
                serviceBuilder.withLogFile(logFile);
            }

            // Set environment variables if specified
            if (config.getEnvironmentVariables() != null && !config.getEnvironmentVariables().isEmpty()) {
                serviceBuilder.withEnvironment(config.getEnvironmentVariables());
            }

            return serviceBuilder.build();

        } catch (Exception e) {
            log.error("Failed to create ChromeDriver service", e);
            throw new RuntimeException("Failed to create ChromeDriver service", e);
        }
    }

    /**
     * Creates ChromeOptions with anti-detection configurations.
     * <p>
     * Delegates to ChromeOptionsBuilder to create options with stealth
     * arguments, preferences, and experimental options.
     * </p>
     *
     * @param config the driver configuration
     * @return configured ChromeOptions
     */
    private static ChromeOptions createOptions(DriverConfig config) {
        // ChromeOptions creation is thread-safe as it creates new instances
        return new ChromeOptionsBuilder()
                .withConfig(config)
                .build();
    }


    /**
     * Initializes the driver with anti-detection features.
     * <p>
     * Performs post-construction initialization including:
     * <ul>
     *   <li>Stealth JavaScript application</li>
     *   <li>Fingerprint randomization startup</li>
     *   <li>Bot detection monitoring</li>
     *   <li>Timeout configuration</li>
     *   <li>Behavior profile setting</li>
     * </ul>
     * </p>
     */
    private void initialize() {
        // Ensure initialization happens only once
        if (isInitialized.compareAndSet(false, true)) {
            log.info("Initializing UndetectedChromeDriver instance #{}", instanceId);

            // Schedule stealth application asynchronously
            SHARED_EXECUTOR.schedule(() -> {
                if (!isQuitting.get()) {
                    try {
                        randomDelay(100, 300);  // Random delay to vary initialization timing
                        stealthExecutor.applyStealthTechniques(); // Apply stealth techniques
                        applyInitialStealthJS(); // Apply initial JavaScript modifications
                        botDetectionHandler.applyAntiDetectionStrategies(); // Apply anti-detection strategies
                    } catch (Exception e) {
                        log.error("Failed to apply initial stealth for instance #{}", instanceId, e);
                    }
                }
            }, 100, TimeUnit.MILLISECONDS);

            // Start fingerprint randomization if enabled
            if (config.isRandomizeFingerprint()) {
                fingerprintRandomizer.start();
            }

            // Start bot detection monitoring
            botDetectionHandler.startMonitoring();

            // Set implicit wait with instance-specific randomization
            manage().timeouts().implicitlyWait(
                    Duration.ofMillis(config.getImplicitWaitMs() +
                            ThreadLocalRandom.current().nextInt(-100, 100))
            );

            // Set behavior profile
            if (config.getBehaviorProfile() != null) {
                humanBehavior.setBehaviorProfile(config.getBehaviorProfile());
            }

            log.info("UndetectedChromeDriver instance #{} initialized successfully", instanceId);
        }
    }


    /**
     * Applies initial stealth JavaScript modifications.
     * <p>
     * Executes JavaScript to modify browser properties that reveal automation.
     * These modifications are applied early to prevent detection on first page load.
     * </p>
     */
    private void applyInitialStealthJS() {
        try {
            executeScript("""
                // Override webdriver detection - check if it's configurable first
                try {
                    const desc = Object.getOwnPropertyDescriptor(navigator, 'webdriver');
                    if (!desc || desc.configurable) {
                        Object.defineProperty(navigator, 'webdriver', {
                            get: () => undefined
                        });
                    }
                } catch (e) {
                    // Property might be non-configurable
                }
                
                // Fix chrome.runtime
                if (!window.chrome) window.chrome = {};
                if (!window.chrome.runtime) window.chrome.runtime = {};
                
                // Mock plugins if empty
                if (navigator.plugins.length === 0) {
                    try {
                        Object.defineProperty(navigator, 'plugins', {
                            get: () => ({
                                0: {name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer'},
                                1: {name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai'},
                                2: {name: 'Native Client', filename: 'internal-nacl-plugin'},
                                length: 3,
                                item: function(i) { return this[i]; },
                                namedItem: function(name) {
                                    for (let i = 0; i < this.length; i++) {
                                        if (this[i].name === name) return this[i];
                                    }
                                    return null;
                                }
                            })
                        });
                    } catch (e) {
                        // Property might already be defined
                    }
                }
                
                // Fix permissions
                const originalQuery = window.navigator.permissions?.query;
                if (originalQuery) {
                    try {
                        window.navigator.permissions.query = function(parameters) {
                            if (parameters.name === 'notifications') {
                                return Promise.resolve({ state: 'prompt' });
                            }
                            return originalQuery.apply(this, arguments);
                        };
                    } catch (e) {
                        // Permissions might not be modifiable
                    }
                }
                
                // Fix languages
                try {
                    Object.defineProperty(navigator, 'languages', {
                        get: () => ['en-US', 'en']
                    });
                } catch (e) {
                    // Languages might already be defined
                }
                """);
        } catch (Exception e) {
            log.debug("Could not apply initial stealth JS for instance #{}: {}", instanceId, e.getMessage());
        }
    }

    /**
     * Navigates to a URL with human-like behavior and bot detection handling.
     * <p>
     * Overrides the standard get() method to add:
     * <ul>
     *   <li>Rate limiting checks</li>
     *   <li>Human-like navigation delays</li>
     *   <li>Initial page scanning</li>
     *   <li>Bot detection monitoring</li>
     *   <li>Post-navigation stealth application</li>
     * </ul>
     * </p>
     *
     * @param url the URL to navigate to
     * @throws IllegalStateException if driver is shutting down
     */
    @Override
    public void get(String url) {
        if (isQuitting.get()) {
            throw new IllegalStateException("Driver instance #" + instanceId + " is shutting down");
        }

        log.debug("Instance #{} navigating to: {}", instanceId, url);

        // Check if we're rate limited
        if (botDetectionHandler.isRateLimited()) {
            int waitTime = botDetectionHandler.getRecommendedWaitTime();
            log.warn("Instance #{} is rate limited, waiting {} seconds", instanceId, waitTime);
            try {
                TimeUnit.SECONDS.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Use human behavior for navigation
        if (config.isHumanBehavior()) {
            // Random pre-navigation delay
            randomDelay(500, 1500);

            // Navigate using parent method to avoid recursion
            super.get(url);

            // Apply human behavior after navigation
            humanBehavior.waitBetweenPages();
            humanBehavior.performInitialPageScan();
        } else {
            randomDelay(50, 200);
            super.get(url);
        }

        // Check for bot detection
        if (botDetectionHandler.isBotDetectionActive()) {
            log.warn("Bot detection active for instance #{}", instanceId);
            if (!botDetectionHandler.handleBotDetection()) {
                log.error("Failed to bypass bot detection for instance #{}", instanceId);
            }
        }

        // Re-apply stealth after navigation
        SHARED_EXECUTOR.execute(() -> {
            if (!isQuitting.get()) {
                try {
                    stealthExecutor.applyStealthTechniques();
                } catch (Exception e) {
                    log.debug("Could not apply stealth after navigation for instance #{}: {}",
                            instanceId, e.getMessage());
                }
            }
        });
    }

    /**
     * Find an element with human-like behavior.
     * <p>
     * Adds realistic delays to element finding operations when human
     * behavior is enabled.
     * </p>
     *
     * @param by the locator to find an element
     * @return the found WebElement
     */
    @Override
    public WebElement findElement(By by) {
        WebElement element = super.findElement(by);

        // Add slight delay to simulate human element location
        if (config.isHumanBehavior()) {
            randomDelay(50, 150);
        }

        return element;
    }

    /**
     * Find elements with human-like behavior.
     * <p>
     * Adds realistic delays to element finding operations when human
     * behavior is enabled.
     * </p>
     *
     * @param by the locator to find elements
     * @return list of found WebElements
     */
    @Override
    public List<WebElement> findElements(By by) {
        List<WebElement> elements = super.findElements(by);

        // Add slight delay to simulate human element location
        if (config.isHumanBehavior()) {
            randomDelay(50, 150);
        }

        return elements;
    }

    /**
     * Click an element with human-like behavior.
     * <p>
     * Performs realistic click actions including scrolling to element,
     * mouse movement, and appropriate delays.
     * </p>
     *
     * @param element the element to click
     */
    public void humanClick(WebElement element) {
        if (config.isHumanBehavior()) {
            humanBehavior.humanClick(element);
        } else {
            element.click();
        }
    }


    /**
     * Type text with human-like behavior.
     * <p>
     * Types text character by character with realistic delays and
     * occasional typos when human behavior is enabled.
     * </p>
     *
     * @param element the element to type into
     * @param text the text to type
     */
    public void humanType(WebElement element, String text) {
        if (config.isHumanBehavior()) {
            humanBehavior.humanType(element, text);
        } else {
            element.sendKeys(text);
        }
    }


    /**
     * Scroll with human-like behavior.
     * <p>
     * Performs smooth scrolling with easing when human behavior is enabled.
     * </p>
     *
     * @param targetY the target Y coordinate to scroll to
     */
    public void humanScroll(int targetY) {
        if (config.isHumanBehavior()) {
            humanBehavior.humanScroll(targetY);
        } else {
            executeScript("window.scrollTo(0, " + targetY + ");");
        }
    }


    /**
     * Scroll to an element with human-like behavior.
     * <p>
     * Scrolls element into view with natural movement and centering.
     * </p>
     *
     * @param element the element to scroll to
     */
    public void humanScrollToElement(WebElement element) {
        if (config.isHumanBehavior()) {
            humanBehavior.scrollToElement(element);
        } else {
            executeScript("arguments[0].scrollIntoView(true);", element);
        }
    }


    /**
     * Perform random human-like actions.
     * <p>
     * Simulates idle user behavior like random scrolling or mouse movements.
     * </p>
     */
    public void performRandomActions() {
        if (config.isHumanBehavior()) {
            humanBehavior.performRandomActions();
        }
    }


    /**
     * Quits the driver and cleans up resources.
     * <p>
     * Ensures proper cleanup of all anti-detection components before
     * calling parent quit method. Thread-safe with atomic operations.
     * </p>
     */
    @Override
    public void quit() {
        if (isQuitting.compareAndSet(false, true)) {
            log.info("Quitting UndetectedChromeDriver instance #{}", instanceId);

            // Stop bot detection monitoring
            if (botDetectionHandler != null) {
                try {
                    botDetectionHandler.stopMonitoring();
                } catch (Exception e) {
                    log.debug("Error stopping bot detection handler for instance #{}: {}",
                            instanceId, e.getMessage());
                }
            }

            // Stop fingerprint randomization
            if (fingerprintRandomizer != null) {
                try {
                    fingerprintRandomizer.stop();
                } catch (Exception e) {
                    log.debug("Error stopping fingerprint randomizer for instance #{}: {}",
                            instanceId, e.getMessage());
                }
            }

            randomDelay(100, 300);

            try {
                super.quit();
            } catch (Exception e) {
                log.warn("Error during quit for instance #{}: {}", instanceId, e.getMessage());
            }
        }
    }


    /**
     * Closes the current window.
     * <p>
     * Prevents closing if a driver is already quitting.
     * </p>
     */
    @Override
    public void close() {
        if (!isQuitting.get()) {
            log.debug("Closing window for instance #{}", instanceId);
            super.close();
        }
    }

    /**
     * Adds a random delay between min and max milliseconds.
     * <p>
     * Used throughout the driver to add unpredictability to timing.
     * </p>
     *
     * @param minMs minimum delay in milliseconds
     * @param maxMs maximum delay in milliseconds
     */
    private void randomDelay(int minMs, int maxMs) {
        try {
            int delay = ThreadLocalRandom.current().nextInt(minMs, maxMs);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Random delay interrupted for instance #{}", instanceId);
        }
    }


    /**
     * Check if this driver instance is still active.
     * <p>
     * Returns false if the driver is in the process of quitting.
     * </p>
     *
     * @return true if a driver is active, false if quitting
     */
    public boolean isActive() {
        return !isQuitting.get();
    }
}