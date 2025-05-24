package com.undetected.chromedriver;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.*;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * Simulates human-like behavior to avoid bot detection.
 * Handles mouse movements, scrolling, typing, and page navigation timing.
 * <p>
 * This class implements sophisticated human behavior simulation to make automated
 * browser interactions appear natural and avoid detection by anti-bot systems.
 * It introduces realistic delays, movement patterns, and interaction behaviors
 * that mimic how real users interact with web pages.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Natural mouse movements using Bezier curves</li>
 *   <li>Smooth scrolling with easing functions</li>
 *   <li>Realistic typing with variable speed and typos</li>
 *   <li>Human-like page navigation timing</li>
 *   <li>Random idle behaviors and page scanning</li>
 *   <li>Configurable behavior profiles (FAST, NORMAL, CAREFUL)</li>
 * </ul>
 *
 * <h2>Behavior Patterns:</h2>
 * <ul>
 *   <li><b>Mouse Movement:</b> Curved paths with variable speed</li>
 *   <li><b>Scrolling:</b> Smooth with occasional pauses for reading</li>
 *   <li><b>Typing:</b> Variable speed with occasional typos and corrections</li>
 *   <li><b>Navigation:</b> Realistic delays between page loads</li>
 *   <li><b>Idle Behavior:</b> Random scrolling and mouse movements</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * WebDriver driver = new ChromeDriver();
 * HumanBehaviorSimulator simulator = new HumanBehaviorSimulator(driver, 1);
 *
 * // Set behavior profile
 * simulator.setBehaviorProfile(BehaviorProfile.NORMAL);
 *
 * // Navigate with human timing
 * simulator.humanNavigate("https://example.com");
 *
 * // Find and click element naturally
 * WebElement button = driver.findElement(By.id("submit"));
 * simulator.humanClick(button);
 *
 * // Type with human speed
 * WebElement input = driver.findElement(By.name("email"));
 * simulator.humanType(input, "user@example.com");
 * }</pre>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see Actions
 * @see BehaviorProfile
 */
@Slf4j
public class HumanBehaviorSimulator {

    /**
     * The WebDriver instance to control.
     * <p>
     * Used for all browser interactions including navigation, element finding,
     * and JavaScript execution.
     * </p>
     */
    protected final WebDriver driver;

    /**
     * Unique identifier for this simulator instance.
     * <p>
     * Used in logging to distinguish between multiple simulator instances
     * when running parallel browser sessions.
     * </p>
     */
    protected final int instanceId;

    /**
     * Selenium Actions instance for complex interactions.
     * <p>
     * Used for mouse movements and advanced user interactions. May be null
     * if the driver doesn't support the Interactive interface or in test mode.
     * </p>
     */
    private Actions actions;

    /**
     * Thread-local random number generator.
     * <p>
     * Provides thread-safe random number generation for all randomized
     * behaviors and timings.
     * </p>
     */
    private final ThreadLocalRandom random;

    /**
     * Minimum delay between scroll steps in milliseconds.
     * <p>
     * Lower bound for smooth scrolling animation delays.
     * Default: 50ms
     * </p>
     */
    private int minScrollDelay = 50;

    /**
     * Maximum delay between scroll steps in milliseconds.
     * <p>
     * Upper bound for smooth scrolling animation delays.
     * Default: 200ms
     * </p>
     */
    private int maxScrollDelay = 200;

    /**
     * Minimum delay between typed characters in milliseconds.
     * <p>
     * Lower bound for typing speed simulation.
     * Default: 50ms
     * </p>
     */
    private int minTypingDelay = 50;

    /**
     * Maximum delay between typed characters in milliseconds.
     * <p>
     * Upper bound for typing speed simulation.
     * Default: 250ms
     * </p>
     */
    private int maxTypingDelay = 250;

    /**
     * Minimum number of steps for mouse movement animation.
     * <p>
     * Lower bound for mouse movement smoothness.
     * Default: 10 steps
     * </p>
     */
    private int minMouseMoveSteps = 10;

    /**
     * Maximum number of steps for mouse movement animation.
     * <p>
     * Upper bound for mouse movement smoothness.
     * Default: 30 steps
     * </p>
     */
    private int maxMouseMoveSteps = 30;

    /**
     * Minimum wait time between page navigations in milliseconds.
     * <p>
     * Lower bound for inter-page navigation delays.
     * Default: 2000ms (2 seconds)
     * </p>
     */
    private int minPageWaitTime = 2000;

    /**
     * Maximum wait time between page navigations in milliseconds.
     * <p>
     * Upper bound for inter-page navigation delays.
     * Default: 5000 ms (5 seconds)
     * </p>
     */
    private int maxPageWaitTime = 5000;


    /**
     * Constructs a HumanBehaviorSimulator with Actions support.
     * <p>
     * Creates a simulator with full interaction capabilities including
     * mouse movements via Selenium Actions API.
     * </p>
     *
     * @param driver the WebDriver instance to control
     * @param instanceId unique identifier for this simulator
     */
    public HumanBehaviorSimulator(WebDriver driver, int instanceId) {
        this(driver, instanceId, true);
    }

    /**
     * Constructor with an option to disable Actions (for testing).
     * <p>
     * Allows creation of a simulator without Actions API support, useful
     * for testing or when the driver doesn't support the Interactive interface.
     * </p>
     *
     * @param driver the WebDriver instance to control
     * @param instanceId unique identifier for this simulator
     * @param createActions whether to attempt Actions creation
     */
    protected HumanBehaviorSimulator(WebDriver driver, int instanceId, boolean createActions) {
        this.driver = driver;
        this.instanceId = instanceId;
        this.random = ThreadLocalRandom.current();
        this.actions = null; // Always start with null

        if (createActions) {
            // Try to create Actions but handle cases where a driver doesn't support it
            try {
                // Check if a driver implements Interactive before creating Actions
                if (driver instanceof Interactive) {
                    this.actions = new Actions(driver);
                } else {
                    log.debug("Driver does not implement Interactive interface for instance #{}", instanceId);
                }
            } catch (Exception e) {
                log.debug("Failed to create Actions for instance #{}: {}", instanceId, e.getMessage());
                this.actions = null;
            }
        }
    }


    /**
     * Navigate to URL with human-like timing.
     * <p>
     * Simulates natural navigation behavior including:
     * <ul>
     *   <li>Pre-navigation delay (user decision time)</li>
     *   <li>Page load waiting</li>
     *   <li>Post-load scanning behavior</li>
     * </ul>
     * </p>
     *
     * @param url the URL to navigate to
     */
    public void humanNavigate(String url) {
        log.debug("Instance #{} navigating to {} with human behavior", instanceId, url);

        // Random pre-navigation delay; Simulates user reading/deciding before clicking
        pause(random.nextInt(500, 1500));

        // Actually navigate (not recursive!); Use JavaScript navigation to avoid recursion
        ((JavascriptExecutor) driver).executeScript("window.location.href = arguments[0];", url);


        waitForPageLoad();  // Wait for the page to load with human-like delay
        pause(random.nextInt(minPageWaitTime, maxPageWaitTime));    // Additional delay after page load

        performInitialPageScan(); // Simulate initial page scanning; Users typically scan new pages briefly
    }


    /**
     * Perform human-like scrolling.
     * <p>
     * Implements smooth scrolling with easing function to simulate natural
     * human scrolling patterns. Includes occasional pauses for reading.
     * </p>
     *
     * @param targetY the target vertical scroll position in pixels
     */
    public void humanScroll(int targetY) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Long currentY = (Long) js.executeScript("return window.pageYOffset;"); // Get current scroll position

        if (currentY == null) currentY = 0L;

        int distance = Math.abs(targetY - currentY.intValue()); // Calculate scroll distance and steps
        int steps = Math.max(5, distance / 100); // More steps for longer distances

        log.debug("Instance #{} scrolling from {} to {} in {} steps",
                instanceId, currentY, targetY, steps);

        // Perform smooth scrolling animation
        for (int i = 0; i <= steps; i++) {
            // Calculate intermediate position with easing
            int intermediateY = currentY.intValue() +
                    (int) ((targetY - currentY) * easeInOutCubic((double) i / steps));

            js.executeScript("window.scrollTo(0, " + intermediateY + ");"); // Execute scroll

            // Random delay between scroll steps
            pause(random.nextInt(minScrollDelay, maxScrollDelay));

            // Occasionally pause scrolling (like reading)
            if (random.nextDouble() < 0.1) {
                pause(random.nextInt(500, 1500));
            }
        }
    }


    /**
     * Smooth scrolling to an element with human-like behavior.
     * <p>
     * Scrolls an element into view with natural behavior including:
     * <ul>
     *   <li>Centering element in viewport</li>
     *   <li>Occasional overshoot and correction</li>
     *   <li>Fallback to simple scroll if complex scroll fails</li>
     * </ul>
     * </p>
     *
     * @param element the WebElement to scroll to
     */
    public void scrollToElement(WebElement element) {
        try {
            // Get element location and viewport size
            Point location = element.getLocation();
            Dimension viewportSize = driver.manage().window().getSize();

            // Scroll to bring an element to the middle of viewport
            int targetY = location.getY() - (viewportSize.getHeight() / 2);
            humanScroll(Math.max(0, targetY));

            // Small adjustment scroll
            pause(random.nextInt(200, 500));

            // Sometimes overshoot and correct
            if (random.nextDouble() < 0.3) {
                int overshoot = random.nextInt(50, 150); // Overshoot by 50-150 pixels
                humanScroll(targetY + overshoot);
                pause(random.nextInt(300, 600));
                humanScroll(targetY); // Correct back to target
            }
        } catch (Exception e) {
            log.debug("Failed to scroll to element for instance #{}: {}", instanceId, e.getMessage());
            // Fallback to a simple scroll
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            } catch (Exception ex) {
                log.debug("Fallback scroll also failed for instance #{}", instanceId);
            }
        }
    }


    /**
     * Move the mouse with a human-like curve.
     * <p>
     * Implements natural mouse movement using Bezier curves to create
     * smooth, curved paths that mimic human hand movements.
     * </p>
     *
     * @param target the WebElement to move mouse to
     */
    public void humanMouseMove(WebElement target) {
        if (actions == null) {
            log.debug("Actions not available for instance #{}, skipping mouse movement", instanceId);
            return;
        }

        try {
            // Get current and target positions
            Point currentLocation = getCurrentMousePosition();
            Point targetLocation = target.getLocation();
            Dimension targetSize = target.getSize();

            // Add some randomness to the target point (not always center)
            int targetX = targetLocation.getX() + random.nextInt(5, targetSize.getWidth() - 5);
            int targetY = targetLocation.getY() + random.nextInt(5, targetSize.getHeight() - 5);

            // Calculate control points for a Bézier curve
            int steps = random.nextInt(minMouseMoveSteps, maxMouseMoveSteps);

            for (int i = 0; i <= steps; i++) {              // Animate mouse movement along curve
                double t = (double) i / steps;
                Point intermediate = calculateBezierPoint(  // Calculate next position on curve
                        currentLocation,
                        new Point(targetX, targetY),
                        t
                );

                // Move to an intermediate position
                actions.moveByOffset(
                        intermediate.getX() - currentLocation.getX(),
                        intermediate.getY() - currentLocation.getY()
                ).perform();

                currentLocation = intermediate;

                // Variable speed (slower near target)
                int delay = (int) (random.nextInt(5, 15) * (1 + t));
                pause(delay);
            }

            // Small correction movement
            if (random.nextDouble() < 0.4) {
                pause(100);
                actions.moveByOffset(
                        random.nextInt(-3, 4),
                        random.nextInt(-3, 4)
                ).perform();
            }

        } catch (Exception e) {
            log.debug("Mouse movement failed for instance #{}: {}", instanceId, e.getMessage());
            // Fallback to direct move
            if (actions != null) {
                try {
                    actions.moveToElement(target).perform();
                } catch (Exception ex) {
                    log.debug("Fallback mouse move also failed for instance #{}", instanceId);
                }
            }
        }
    }


    /**
     * Click element with human-like behavior.
     * <p>
     * Performs a natural click sequence:
     * <ol>
     *   <li>Scroll element into view</li>
     *   <li>Move mouse to element</li>
     *   <li>Optional hover pause</li>
     *   <li>Click with appropriate timing</li>
     * </ol>
     * </p>
     *
     * @param element the WebElement to click
     */
    public void humanClick(WebElement element) {
        // Scroll element into view
        scrollToElement(element);
        pause(random.nextInt(300, 700));

        // Move the mouse to an element
        humanMouseMove(element);
        pause(random.nextInt(100, 300));

        // Sometimes hover before clicking
        if (random.nextDouble() < 0.3) {
            pause(random.nextInt(200, 600));
        }

        // Click with a slight delay
        if (actions != null) {
            try {
                actions.click().perform();
            } catch (Exception e) {
                log.debug("Actions click failed for instance #{}, using element click", instanceId);
                element.click();
            }
        } else {
            element.click(); // Fallback to direct click
        }

        // Post-click pause
        pause(random.nextInt(200, 500));
    }


    /**
     * Type text with human-like speed and rhythm.
     * <p>
     * Simulates realistic typing including:
     * <ul>
     *   <li>Variable typing speed</li>
     *   <li>Longer pauses after spaces and punctuation</li>
     *   <li>Occasional thinking pauses</li>
     *   <li>Rare typos with corrections</li>
     * </ul>
     * </p>
     *
     * @param element the WebElement to type into
     * @param text the text to type
     */
    public void humanType(WebElement element, String text) {
        element.click();
        pause(random.nextInt(200, 500));

        for (char c : text.toCharArray()) {
            element.sendKeys(String.valueOf(c));

            // Variable typing speed
            int baseDelay = random.nextInt(minTypingDelay, maxTypingDelay);

            // Longer pause after spaces or punctuation
            if (c == ' ' || c == '.' || c == ',') {
                baseDelay += random.nextInt(50, 150);
            }

            // Occasionally longer pause (thinking)
            if (random.nextDouble() < 0.05) {
                baseDelay += random.nextInt(500, 1500);
            }

            pause(baseDelay);

            // Simulate typos and corrections
            if (random.nextDouble() < 0.02 && text.length() > 5) {
                // Make typo
                element.sendKeys(getRandomChar());
                pause(random.nextInt(100, 300));
                // Correct it
                element.sendKeys(Keys.BACK_SPACE);
                pause(random.nextInt(100, 200));
            }
        }
    }


    /**
     * Wait between page navigations with human-like timing.
     * <p>
     * Simulates the time users spend between navigating pages, including
     * occasional longer breaks.
     * </p>
     */
    public void waitBetweenPages() {
        int waitTime = random.nextInt(minPageWaitTime, maxPageWaitTime);

        // Sometimes take longer breaks
        if (random.nextDouble() < 0.1) {
            waitTime += random.nextInt(5000, 15000);
        }

        log.debug("Instance #{} waiting {} ms between pages", instanceId, waitTime);
        pause(waitTime);
    }


    /**
     * Perform random human-like actions on page.
     * <p>
     * Simulates idle user behavior including:
     * <ul>
     *   <li>Random mouse movements</li>
     *   <li>Random scrolling</li>
     *   <li>Tab key navigation</li>
     * </ul>
     * </p>
     */
    public void performRandomActions() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Random mouse movements (only if actions available)
            if (actions != null && random.nextDouble() < 0.3) {
                try {
                    // Perform 1-3 random movements
                    int moveCount = random.nextInt(1, 4);
                    for (int i = 0; i < moveCount; i++) {
                        actions.moveByOffset(
                                random.nextInt(-100, 100),
                                random.nextInt(-100, 100)
                        ).perform();
                        pause(random.nextInt(500, 1500));
                    }
                } catch (Exception e) {
                    log.trace("Random mouse movement failed for instance #{}", instanceId);
                }
            }

            // Random scrolling
            if (random.nextDouble() < 0.4) {
                Long maxScroll = (Long) js.executeScript(
                        "return Math.max(document.body.scrollHeight, " +
                                "document.documentElement.scrollHeight) - window.innerHeight;"
                );
                if (maxScroll != null && maxScroll > 0) {
                    humanScroll(random.nextInt(0, maxScroll.intValue()));
                }
            }

            // Tab focus changes (only if actions available)
            if (actions != null && random.nextDouble() < 0.1) {
                try {
                    // Send tab key
                    actions.sendKeys(Keys.TAB).perform();
                    pause(random.nextInt(500, 1000));
                } catch (Exception e) {
                    log.trace("Tab action failed for instance #{}", instanceId);
                }
            }

        } catch (Exception e) {
            log.debug("Random action failed for instance #{}: {}", instanceId, e.getMessage());
        }
    }


    /**
     * Configure behavior parameters.
     * <p>
     * Sets predefined behavior profiles that adjust all timing parameters
     * to match different user interaction styles.
     * </p>
     *
     * @param profile the behavior profile to apply
     */
    public void setBehaviorProfile(BehaviorProfile profile) {
        switch (profile) {
            case FAST:                      // Fast but still human-like behavior
                minScrollDelay = 30;
                maxScrollDelay = 100;
                minTypingDelay = 30;
                maxTypingDelay = 150;
                minMouseMoveSteps = 5;
                maxMouseMoveSteps = 15;
                minPageWaitTime = 1000;
                maxPageWaitTime = 3000;
                break;

            case NORMAL:                    // Already initialized with balanced values
                break;

            case CAREFUL:                   // Very cautious, slower behavior
                minScrollDelay = 100;
                maxScrollDelay = 300;
                minTypingDelay = 100;
                maxTypingDelay = 400;
                minMouseMoveSteps = 20;
                maxMouseMoveSteps = 50;
                minPageWaitTime = 3000;
                maxPageWaitTime = 8000;
                break;
        }
    }


    /**
     * Behavior profile enumeration.
     * <p>
     * Defines preset behavior patterns for different interaction styles.
     * </p>
     */
    public enum BehaviorProfile {
        /**
         * Fast but still human-like behavior.
         * <p>
         * Suitable for automated testing where speed is important
         * but human-like behavior is still required.
         * </p>
         */
        FAST,

        /**
         * Default balanced behavior.
         * <p>
         * Mimics average user interaction speed and patterns.
         * </p>
         */
        NORMAL,

        /**
         * Very cautious, slower behavior.
         * <p>
         * Suitable for high-security sites or when maximum
         * human-likeness is required.
         * </p>
         */
        CAREFUL  // Very cautious, slower
    }


    /**
     * Pauses execution for specified milliseconds.
     * <p>
     * Central pause method that handles interruption properly.
     * </p>
     *
     * @param milliseconds the duration to pause in milliseconds
     */
    protected void pause(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            log.warn("Pause interrupted for instance #{}", instanceId);
        }
    }

    /**
     * Gets current mouse position (simulated).
     * <p>
     * Since Selenium doesn't provide actual mouse position, this returns
     * a random starting point within the browser window.
     * </p>
     *
     * @return simulated current mouse position
     */
    private Point getCurrentMousePosition() {
        // Since we can't get an actual position, return a random starting point
        Dimension size = driver.manage().window().getSize();
        return new Point(
                random.nextInt(size.getWidth()),
                random.nextInt(size.getHeight())
        );
    }


    /**
     * Calculates a point on a cubic Bézier curve.
     * <p>
     * Creates natural curved paths for mouse movement using cubic Bezier
     * interpolation with randomized control points.
     * </p>
     *
     * @param start the starting point
     * @param end the ending point
     * @param t the interpolation parameter (0.0 to 1.0)
     * @return the interpolated point on the curve
     */
    private Point calculateBezierPoint(Point start, Point end, double t) {
        // First control point at 25% with a random offset
        double controlX1 = start.getX() + (end.getX() - start.getX()) * 0.25 + random.nextInt(-50, 50);
        double controlY1 = start.getY() + (end.getY() - start.getY()) * 0.25 + random.nextInt(-50, 50);

        // The second control point at 75% with a random offset
        double controlX2 = start.getX() + (end.getX() - start.getX()) * 0.75 + random.nextInt(-50, 50);
        double controlY2 = start.getY() + (end.getY() - start.getY()) * 0.75 + random.nextInt(-50, 50);

        // Cubic bezier formula; B(t) = (1-t)³P₀ + 3(1-t)²tP₁ + 3(1-t)t²P₂ + t³P₃
        double x = Math.pow(1 - t, 3) * start.getX() +
                3 * Math.pow(1 - t, 2) * t * controlX1 +
                3 * (1 - t) * Math.pow(t, 2) * controlX2 +
                Math.pow(t, 3) * end.getX();

        double y = Math.pow(1 - t, 3) * start.getY() +
                3 * Math.pow(1 - t, 2) * t * controlY1 +
                3 * (1 - t) * Math.pow(t, 2) * controlY2 +
                Math.pow(t, 3) * end.getY();

        return new Point((int) x, (int) y);
    }


    /**
     * Ease-in-out cubic easing function.
     * <p>
     * Provides smooth acceleration and deceleration for animations,
     * mimicking natural human movement patterns.
     * </p>
     *
     * @param t the input parameter (0.0 to 1.0)
     * @return the eased output value
     */
    private double easeInOutCubic(double t) {
        // Speed up until halfway, then decelerate
        return t < 0.5
                ? 4 * t * t * t
                : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }


    /**
     * Gets a random lowercase letter for typo simulation.
     *
     * @return a random lowercase letter as a String
     */
    private String getRandomChar() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        return String.valueOf(chars.charAt(random.nextInt(chars.length())));
    }


    /**
     * Waits for page to fully load.
     * <p>
     * Uses WebDriverWait to wait for document ready state to be "complete".
     * Times out after 30 seconds.
     * </p>
     */
    private void waitForPageLoad() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(
                    webDriver -> {
                        try {
                            // Check document ready state
                            return ((JavascriptExecutor) webDriver)
                                    .executeScript("return document.readyState").equals("complete");
                        } catch (Exception e) {
                            // If we can't execute script, assume page is not ready
                            return false;
                        }
                    }
            );
        } catch (Exception e) {
            log.debug("Page load wait timeout for instance #{}", instanceId);
        }
    }


    /**
     * Performs initial page scanning behavior.
     * <p>
     * Simulates how users typically scan a new page by scrolling down
     * in small increments, pausing to "read", and sometimes returning
     * to the top.
     * </p>
     */
    public void performInitialPageScan() {
        try {
            // Simulate reading page from top
            pause(random.nextInt(500, 1500));

            // Small scroll movements like scanning
            for (int i = 0; i < random.nextInt(2, 5); i++) {
                humanScroll(random.nextInt(100, 300) * (i + 1));
                pause(random.nextInt(800, 2000));
            }

            // Return to the top sometimes
            if (random.nextDouble() < 0.3) {
                pause(random.nextInt(500, 1000));
                humanScroll(0);
            }
        } catch (Exception e) {
            log.debug("Initial page scan failed for instance #{}: {}", instanceId, e.getMessage());
        }
    }
}