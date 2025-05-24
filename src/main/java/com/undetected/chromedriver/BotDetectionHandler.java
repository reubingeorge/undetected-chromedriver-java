package com.undetected.chromedriver;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles bot detection challenges and monitoring.
 * <p>
 * This class provides sophisticated mechanisms to detect and handle various
 * anti-bot systems including Cloudflare challenges, CAPTCHAs, and rate limiting.
 * It employs human behavior simulation and monitoring strategies to evade
 * detection and resolve challenges automatically when possible.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Detection of Cloudflare, reCAPTCHA, hCaptcha, and Turnstile challenges</li>
 *   <li>Automatic challenge resolution strategies</li>
 *   <li>Rate limiting detection and handling</li>
 *   <li>Human behavior simulation integration</li>
 *   <li>JavaScript-based monitoring and anti-detection</li>
 *   <li>Configurable timeout and retry mechanisms</li>
 * </ul>
 *
 * <h2>Supported Bot Detection Systems:</h2>
 * <ul>
 *   <li><b>Cloudflare:</b> Browser verification, JavaScript challenges</li>
 *   <li><b>Google reCAPTCHA:</b> v2 and v3 detection</li>
 *   <li><b>hCaptcha:</b> Privacy-focused CAPTCHA alternative</li>
 *   <li><b>Cloudflare Turnstile:</b> Modern challenge system</li>
 *   <li><b>Rate Limiting:</b> HTTP 429 and custom rate limit detection</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * WebDriver driver = new ChromeDriver();
 * BotDetectionHandler handler = new BotDetectionHandler(driver, 1);
 *
 * // Start monitoring for bot detection
 * handler.startMonitoring();
 *
 * // Navigate to protected site
 * driver.get("https://protected-site.com");
 *
 * // Check and handle bot detection
 * if (handler.isBotDetectionActive()) {
 *     boolean resolved = handler.handleBotDetection(60); // 60 second timeout
 *     if (!resolved) {
 *         System.out.println("Could not resolve bot challenge");
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * This class is thread-safe for monitoring operations through the use of
 * {@link AtomicBoolean}. However, WebDriver operations should be synchronized
 * externally if used from multiple threads.
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see HumanBehaviorSimulator
 */
@Slf4j
public class BotDetectionHandler {

    /**
     * The WebDriver instance used for browser automation.
     * <p>
     * This driver is used for all detection and interaction operations
     * including page source analysis, element finding, and JavaScript execution.
     * </p>
     */
    private final WebDriver driver;

    /**
     * Unique identifier for this handler instance.
     * <p>
     * Used in logging to distinguish between multiple concurrent instances
     * when running parallel browser sessions.
     * </p>
     */
    private final int instanceId;

    /**
     * Human behavior simulator for natural interactions.
     * <p>
     * Provides realistic mouse movements, clicks, and other behaviors
     * to avoid detection by bot protection systems.
     * </p>
     */
    private final HumanBehaviorSimulator behaviorSimulator;

    /**
     * Thread-safe flag indicating if monitoring is active.
     * <p>
     * Uses {@link AtomicBoolean} to ensure thread-safe state management
     * for the monitoring feature.
     * </p>
     */
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);


    /**
     * Known indicators of Cloudflare bot protection.
     * <p>
     * These strings are searched in page source and title to detect
     * Cloudflare's various challenge types including:
     * <ul>
     *   <li>Browser verification checks</li>
     *   <li>JavaScript challenges</li>
     *   <li>Ray ID error pages</li>
     * </ul>
     * </p>
     */
    private static final String[] CLOUDFLARE_INDICATORS = {
            "Checking your browser",        // Initial challenge message
            "Just a moment",                // Common challenge page title
            "ray ID",                       // Cloudflare error identifier
            "cf-browser-verification",      // Verification class name
            "cf-challenge-running"          // Active challenge indicator
    };


    /**
     * Known indicators of CAPTCHA systems.
     * <p>
     * These strings identify various CAPTCHA implementations:
     * <ul>
     *   <li>g-recaptcha: Google reCAPTCHA</li>
     *   <li>h-captcha: hCaptcha service</li>
     *   <li>cf-turnstile: Cloudflare Turnstile</li>
     *   <li>challenge-form: Generic challenge forms</li>
     * </ul>
     * </p>
     */
    private static final String[] CAPTCHA_INDICATORS = {
            "g-recaptcha",      // Google reCAPTCHA class
            "h-captcha",        // hCaptcha class
            "cf-turnstile",     // Cloudflare Turnstile
            "challenge-form"    // Generic challenge form
    };


    /**
     * Constructs a BotDetectionHandler with default settings.
     * <p>
     * Creates a handler with full HumanBehaviorSimulator capabilities
     * including Actions API for realistic interactions.
     * </p>
     *
     * @param driver the WebDriver instance to monitor and control
     * @param instanceId unique identifier for this handler instance
     */
    public BotDetectionHandler(WebDriver driver, int instanceId) {
        this(driver, instanceId, true); // Delegate to full constructor with Actions enabled
    }

    /**
     * Constructor with an option to create HumanBehaviorSimulator with Actions.
     * <p>
     * Protected constructor allowing control over HumanBehaviorSimulator
     * initialization. Useful for testing where Actions API might not be
     * available.
     * </p>
     *
     * @param driver the WebDriver instance to monitor and control
     * @param instanceId unique identifier for this handler instance
     * @param createActionsInSimulator whether to create Actions in HumanBehaviorSimulator
     */
    protected BotDetectionHandler(WebDriver driver, int instanceId, boolean createActionsInSimulator) {
        this.driver = driver;
        this.instanceId = instanceId;

        // Initialize the behavior simulator with configurable Actions support
        this.behaviorSimulator = new HumanBehaviorSimulator(driver, instanceId, createActionsInSimulator);
    }

    /**
     * Check if bot detection is active on the current page.
     * <p>
     * This method performs comprehensive checks to detect various bot
     * protection systems. It analyzes page source, title, specific elements,
     * and HTTP status codes to identify challenges.
     * </p>
     *
     * <h3>Detection Strategy:</h3>
     * <ol>
     *   <li>Verify driver readiness</li>
     *   <li>Search for known text indicators</li>
     *   <li>Check for specific DOM elements</li>
     *   <li>Analyze HTTP response status</li>
     * </ol>
     *
     * <h3>Detected Systems:</h3>
     * <ul>
     *   <li>Cloudflare challenges and errors</li>
     *   <li>Various CAPTCHA implementations</li>
     *   <li>HTTP 403/429 blocking statuses</li>
     * </ul>
     *
     * @return {@code true} if bot detection is active, {@code false} otherwise
     */
    public boolean isBotDetectionActive() {
        try {
            // First check if a driver is ready
            // Attempt to get the current URL to verify a driver is responsive
            try {
                driver.getCurrentUrl();
            } catch (Exception e) {
                // Driver doesn't ready, can't perform detection
                log.debug("Driver not ready for bot detection check on instance #{}", instanceId);
                return false;
            }

            // Get page source and title for analysis
            String pageSource = driver.getPageSource();
            String title = driver.getTitle();

            // Handle null returns
            // Ensure we have non-null strings for comparison
            if (pageSource == null) pageSource = "";
            if (title == null) title = "";

            // Convert to lowercase for case-insensitive comparison
            pageSource = pageSource.toLowerCase();
            title = title.toLowerCase();

            // Check for Cloudflare challenge
            // Iterate through known Cloudflare indicators
            for (String indicator : CLOUDFLARE_INDICATORS) {
                if (pageSource.contains(indicator.toLowerCase()) ||
                        title.contains(indicator.toLowerCase())) {
                    log.warn("Instance #{} detected Cloudflare challenge: {}", instanceId, indicator);
                    return true;
                }
            }

            // Check for CAPTCHA
            // Search for CAPTCHA system indicators
            for (String indicator : CAPTCHA_INDICATORS) {
                if (pageSource.contains(indicator)) {
                    log.warn("Instance #{} detected CAPTCHA: {}", instanceId, indicator);
                    return true;
                }
            }

            // Check for specific elements
            // Look for challenge-specific DOM elements
            if (isElementPresent(By.id("cf-challenge-running")) ||
                    isElementPresent(By.className("cf-browser-verification"))) {
                log.warn("Instance #{} detected challenge element", instanceId);
                return true;
            }

            // Check HTTP status via JavaScript
            // Use Navigation Timing API to get response status
            try {
                Long status = (Long) ((JavascriptExecutor) driver).executeScript(
                        "return window.performance.getEntriesByType('navigation')[0]?.responseStatus || 0;"
                );

                // Check for blocking status codes
                if (status != null && (status == 403 || status == 429)) {
                    log.warn("Instance #{} detected blocking status: {}", instanceId, status);
                    return true;
                }
            } catch (Exception e) {
                // Ignore JavaScript errors during bot detection check
                // Some pages might not support Navigation Timing API
                log.trace("Could not check HTTP status for instance #{}", instanceId);
            }

            return false;

        } catch (Exception e) {
            // Catch-all for any detection failures
            log.debug("Bot detection check failed for instance #{}: {}", instanceId, e.getMessage());
            return false;
        }
    }

    /**
     * Handle detected a bot challenge.
     * <p>
     * Attempts to resolve bot detection challenges using various strategies
     * including waiting for auto-resolution and simulating human behavior.
     * Uses a default timeout of 30 seconds.
     * </p>
     *
     * @return {@code true} if a challenge was resolved, {@code false} otherwise
     * @see #handleBotDetection(int)
     */
    public boolean handleBotDetection() {
        return handleBotDetection(30); // Use default 30-second timeout
    }

    /**
     * Handle detected a bot challenge with custom timeout.
     * <p>
     * This method implements a multi-strategy approach to resolve bot challenges:
     * <ol>
     *   <li>Switch to a careful behavior profile</li>
     *   <li>Wait for automatic challenge resolution</li>
     *   <li>Perform natural interactions if needed</li>
     *   <li>Verify challenge resolution</li>
     * </ol>
     * </p>
     *
     * <h3>Resolution Strategies:</h3>
     * <ul>
     *   <li><b>Auto-resolution:</b> Many challenges resolve automatically</li>
     *   <li><b>Natural interaction:</b> Simulates human behavior</li>
     *   <li><b>Button clicking:</b> Attempts to click challenge buttons</li>
     * </ul>
     *
     * @param timeoutSeconds maximum time to wait for resolution in seconds
     * @return {@code true} if a challenge was resolved, {@code false} otherwise
     */
    public boolean handleBotDetection(int timeoutSeconds) {
        log.info("Instance #{} attempting to handle bot detection with {} second timeout",
                instanceId, timeoutSeconds);

        try {
            // First, slow down behavior
            // Switch to a careful profile to avoid triggering more protection
            behaviorSimulator.setBehaviorProfile(HumanBehaviorSimulator.BehaviorProfile.CAREFUL);

            // Wait for a challenge to load
            // Give time for challenge JavaScript to initialize
            TimeUnit.SECONDS.sleep(2);

            // If it's a Cloudflare challenge, it might auto-solve
            // Many modern challenges resolve without user interaction
            if (waitForChallengeResolution(timeoutSeconds)) {
                log.info("Instance #{} challenge resolved automatically", instanceId);
                return true;
            }

            // Try to interact naturally with the page
            // Simulate human-like behavior if auto-resolution fails
            performNaturalInteraction();

            // Check if resolved
            // Verify challenge is no longer active
            return !isBotDetectionActive();

        } catch (Exception e) {
            log.error("Failed to handle bot detection for instance #{}", instanceId, e);
            return false;
        }
    }


    /**
     * Wait for automatic challenge resolution.
     * <p>
     * Many bot protection systems, especially Cloudflare, resolve automatically
     * after verifying the browser environment. This method waits for such
     * resolution by monitoring page changes.
     * </p>
     *
     * <h3>Resolution Detection:</h3>
     * <ul>
     *   <li>Challenge indicators disappear from a page source</li>
     *   <li>URL changes (often redirects after a challenge)</li>
     *   <li>Absence of /cdn-cgi/ in URL (Cloudflare specific)</li>
     * </ul>
     *
     * @param timeoutSeconds maximum time to wait for resolution
     * @return {@code true} if a challenge resolved, {@code false} if timeout
     */
    private boolean waitForChallengeResolution(int timeoutSeconds) {
        try {
            // Create WebDriverWait with specified timeout
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

            // Wait for a challenge to disappear
            return wait.until(webDriver -> {
                // Check if we're no longer on the challenge page
                String pageSource = webDriver.getPageSource().toLowerCase();

                // Check for presence of challenge indicators
                boolean stillChallenged = false;
                for (String indicator : CLOUDFLARE_INDICATORS) {
                    if (pageSource.contains(indicator.toLowerCase())) {
                        stillChallenged = true;
                        break;
                    }
                }

                // Also check if the URL has changed (often happens after a challenge)
                String currentUrl = webDriver.getCurrentUrl();

                // Check for Cloudflare-specific URL pattern
                if (!stillChallenged && currentUrl != null && !currentUrl.contains("/cdn-cgi/")) {
                    log.debug("Challenge resolved, URL is now: {}", currentUrl);
                    return true;
                }

                return !stillChallenged; // Return true if no longer challenged
            });

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Perform natural interaction to appear human.
     * <p>
     * Simulates human-like behavior on the page, including random mouse
     * movements and attempting to click visible buttons. This can help
     * pass behavior analysis in some bot detection systems.
     * </p>
     *
     * <h3>Interaction Strategy:</h3>
     * <ol>
     *   <li>Perform random mouse movements</li>
     *   <li>Add realistic delays between actions</li>
     *   <li>Search for and click interactive elements</li>
     * </ol>
     */
    private void performNaturalInteraction() {
        try {
            // Random mouse movements
            // Perform multiple random actions with increasing delays
            for (int i = 0; i < 3; i++) {
                behaviorSimulator.performRandomActions();
                TimeUnit.MILLISECONDS.sleep(500 + (i * 200)); // Increasing delay pattern: 500 ms, 700 ms, 900 ms
            }

            // Try to find and click any visible buttons
            // Check common button selectors
            for (String selector : new String[]{"button", "input[type='submit']", ".challenge-button"}) {
                try {
                    WebElement button = driver.findElement(By.cssSelector(selector)); // Find element by CSS selector

                    // Only click if the button is interactive
                    if (button.isDisplayed() && button.isEnabled()) {
                        behaviorSimulator.humanClick(button); // Use human-like click

                        // Random sleep between 1.5 and 2.5 seconds
                        int randomMillis = 1500 + ThreadLocalRandom.current().nextInt(1000);
                        TimeUnit.SECONDS.sleep(randomMillis); // Wait for potential page changes
                        break;
                    }
                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            log.debug("Natural interaction failed for instance #{}: {}", instanceId, e.getMessage());
        }
    }


    /**
     * Start monitoring for bot detection.
     * <p>
     * Injects JavaScript code that continuously monitors for bot detection
     * challenges. The monitor runs in the background and sets flags when
     * challenges are detected.
     * </p>
     *
     * <h3>Monitoring Features:</h3>
     * <ul>
     *   <li>Periodic checking for challenge indicators</li>
     *   <li>HTTP status monitoring via Navigation API</li>
     *   <li>Cloudflare-specific detection</li>
     * </ul>
     *
     * <h3>Note:</h3>
     * <p>
     * This method is idempotent - multiple calls will not create multiple
     * monitors.
     * </p>
     */
    public void startMonitoring() {
        // Use atomic compare-and-set to ensure a single initialization
        if (isMonitoring.compareAndSet(false, true)) {
            log.debug("Starting bot detection monitoring for instance #{}", instanceId);

            // Add JavaScript to monitor for challenges
            try {
                ((JavascriptExecutor) driver).executeScript("""
                    window.__botDetectionMonitor = {
                        detected: false,
                        check: function() {
                            // Monitor for Cloudflare challenge
                            if (document.title.toLowerCase().includes('just a moment') ||
                                document.body.innerHTML.includes('cf-challenge')) {
                                this.detected = true;
                            }
                            
                            // Monitor for rate limiting
                            var entries = window.performance.getEntriesByType('navigation');
                            if (entries.length > 0 && entries[0].responseStatus === 429) {
                                this.detected = true;
                            }
                        }
                    };
                    
                    // Check periodically
                    setInterval(function() {
                        window.__botDetectionMonitor.check();
                    }, 1000);
                    """);
            } catch (Exception e) {
                log.debug("Failed to inject monitoring script for instance #{}", instanceId);
            }
        }
    }

    /**
     * Stop monitoring.
     * <p>
     * Disables the monitoring flag. Note that injected JavaScript continues
     * to run until page navigation or refresh.
     * </p>
     */
    public void stopMonitoring() {
        isMonitoring.set(false); // Set a flag to false
    }


    /**
     * Check if we're being rate limited.
     * <p>
     * Detects rate limiting through multiple methods:
     * <ul>
     *   <li>HTTP 429 status code detection</li>
     *   <li>Common rate limit message patterns</li>
     * </ul>
     * </p>
     *
     * @return {@code true} if rate limited, {@code false} otherwise
     */
    public boolean isRateLimited() {
        try {
            // Check for status 429
            // Use Navigation Timing API to get HTTP status
            Long status = (Long) ((JavascriptExecutor) driver).executeScript(
                    "return window.performance.getEntriesByType('navigation')[0]?.responseStatus || 0;"
            );

            // HTTP 429 = Too Many Requests
            if (status != null && status == 429) {
                return true;
            }

            // Check for rate limit messages
            String pageSource = driver.getPageSource().toLowerCase();
            return pageSource.contains("rate limit") ||
                    pageSource.contains("too many requests") ||
                    pageSource.contains("please slow down");

        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Get recommended wait time if rate limited.
     * <p>
     * Attempts to extract the recommended wait time from rate limiting
     * responses. Falls back to a default of 60 seconds if no specific
     * time is provided.
     * </p>
     *
     * <h3>Wait Time Sources:</h3>
     * <ul>
     *   <li>Retry-After header (if available)</li>
     *   <li>Server timing information</li>
     *   <li>Default: 60 seconds</li>
     * </ul>
     *
     * @return recommended wait time in seconds, or 0 if not rate limited
     */
    public int getRecommendedWaitTime() {
        if (isRateLimited()) {
            // Check for Retry-After header
            try {
                // Attempt to get retry-after from server timing
                Object retryAfter = ((JavascriptExecutor) driver).executeScript(
                        "return window.performance.getEntriesByType('navigation')[0]?.serverTiming?" +
                                ".find(t => t.name === 'retry-after')?.duration || null;"
                );
                if (retryAfter != null) {
                    return Integer.parseInt(retryAfter.toString()); // Parse and return the wait time
                }
            } catch (Exception e) {
                log.debug("Failed to parse retry-after header for instance #{}: {}", instanceId, e.getMessage());
            }
            // Default wait time
            return 60;
        }
        return 0; // Not rate limited
    }


    /**
     * Apply anti-detection strategies.
     * <p>
     * Modifies browser behavior to appear more human-like by adding
     * random delays to network requests. This helps evade detection
     * systems that analyze request timing patterns.
     * </p>
     *
     * <h3>Strategies Applied:</h3>
     * <ul>
     *   <li>Random delays for fetch() calls (50-150ms)</li>
     *   <li>Random delays for XMLHttpRequest (50-150ms)</li>
     *   <li>Request timing randomization</li>
     * </ul>
     *
     * <h3>Note:</h3>
     * <p>
     * These modifications persist until page navigation or refresh.
     * </p>
     */
    public void applyAntiDetectionStrategies() {
        try {
            // Randomize request timing
            ((JavascriptExecutor) driver).executeScript("""
                // Override fetch to add random delays
                const originalFetch = window.fetch;
                window.fetch = function(...args) {
                    return new Promise((resolve) => {
                        setTimeout(() => {
                            resolve(originalFetch.apply(this, args));
                        }, Math.random() * 100 + 50);
                    });
                };
                
                // Override XMLHttpRequest
                const XHR = XMLHttpRequest.prototype;
                const originalOpen = XHR.open;
                const originalSend = XHR.send;
                
                XHR.open = function() {
                    this._requestStart = Date.now();
                    return originalOpen.apply(this, arguments);
                };
                
                XHR.send = function() {
                    const delay = Math.random() * 100 + 50;
                    setTimeout(() => {
                        originalSend.apply(this, arguments);
                    }, delay);
                };
                """);

        } catch (Exception e) {
            log.debug("Failed to apply anti-detection strategies for instance #{}", instanceId);
        }
    }


    /**
     * Helper method to check if an element is present on the page.
     * <p>
     * Attempts to find an element without throwing exceptions. Used for
     * non-critical element detection where absence is expected.
     * </p>
     *
     * @param by the locator strategy to find the element
     * @return {@code true} if element exists, {@code false} otherwise
     */
    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by); // Attempt to find an element
            return true;
        } catch (Exception e) {
            return false; // Element isn't found or error
        }
    }
}