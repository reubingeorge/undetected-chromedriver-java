package com.undetected.chromedriver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Unit test class for {@link BotDetectionHandler}.
 * <p>
 * This comprehensive test suite validates the bot detection and evasion capabilities
 * of the BotDetectionHandler class. It tests detection of various anti-bot systems
 * including Cloudflare, CAPTCHA services, rate limiting, and the handler's ability
 * to apply anti-detection strategies.
 * </p>
 *
 * <h2>Test Coverage:</h2>
 * <ul>
 *   <li>Cloudflare challenge detection (including Turnstile)</li>
 *   <li>CAPTCHA detection (reCAPTCHA, hCaptcha)</li>
 *   <li>Rate limiting detection and handling</li>
 *   <li>Anti-detection strategy application</li>
 *   <li>JavaScript injection and monitoring</li>
 *   <li>Natural interaction simulation</li>
 *   <li>Error handling and edge cases</li>
 * </ul>
 *
 * <h2>Testing Approach:</h2>
 * <p>
 * Uses Mockito to mock WebDriver interactions, allowing isolated testing of
 * bot detection logic without requiring a real browser instance. The tests
 * simulate various bot detection scenarios and verify the handler's responses.
 * </p>
 *
 * <h2>Testing Framework:</h2>
 * <p>
 * Uses JUnit 5 (Jupiter) for test execution, Mockito for mocking WebDriver
 * components, and AssertJ for fluent assertions.
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see BotDetectionHandler
 */
class BotDetectionHandlerTest {

    /**
     * Mock WebDriver instance used for testing.
     * <p>
     * Annotated with {@link Mock} for automatic initialization by Mockito.
     * This mock is configured to also implement {@link JavascriptExecutor}
     * to support JavaScript execution tests.
     * </p>
     */
    @Mock
    private WebDriver mockDriver;

    /**
     * The BotDetectionHandler instance under test.
     * <p>
     * Initialized in {@link #setUp()} with test mode enabled to avoid
     * real Actions API usage which would require a real browser.
     * </p>
     */
    private BotDetectionHandler handler;

    /**
     * AutoCloseable reference for Mockito annotations.
     * <p>
     * Used to properly close Mockito resources in {@link #tearDown()}.
     * </p>
     */
    private AutoCloseable mocks;

    /**
     * Test instance ID used for handler initialization.
     * <p>
     * Provides a consistent instance identifier across all tests.
     * </p>
     */
    private static final int TEST_INSTANCE_ID = 1;


    /**
     * Sets up the test environment before each test method.
     * <p>
     * This method:
     * <ul>
     *   <li>Initializes Mockito annotations</li>
     *   <li>Creates a mock WebDriver that implements JavascriptExecutor</li>
     *   <li>Instantiates the BotDetectionHandler in test mode</li>
     * </ul>
     * </p>
     *
     * @see MockitoAnnotations#openMocks(Object)
     */
    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this); // Initialize all @Mock annotated fields

        // Make driver implement JavascriptExecutor
        // Create a new mock that implements both WebDriver and JavascriptExecutor interfaces
        mockDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));

        // Create handler with test mode (no Actions in HumanBehaviorSimulator)
        // Test mode = true prevents real mouse/keyboard actions
        handler = new BotDetectionHandler(mockDriver, TEST_INSTANCE_ID, false);
    }


    /**
     * Cleans up test resources after each test method.
     * <p>
     * Ensures proper cleanup of Mockito resources to prevent memory leaks
     * and interference between tests.
     * </p>
     *
     * @throws Exception if cleanup fails
     */
    @AfterEach
    void tearDown() throws Exception {
        // Close Mockito resources if initialized
        if (mocks != null) {
            mocks.close();
        }
    }


    /**
     * Tests that the handler is properly instantiated with an instance ID.
     * <p>
     * This basic test ensures the constructor works correctly and the
     * handler object is created successfully.
     * </p>
     *
     * @see BotDetectionHandler#BotDetectionHandler(WebDriver, int, boolean)
     */
    @Test
    @DisplayName("Should create handler with instance ID")
    void testConstructor() {
        assertThat(handler).isNotNull(); // Simple assertion that handler was created successfully
    }


    /**
     * Tests detection of Cloudflare browser challenges.
     * <p>
     * Simulates a Cloudflare "checking your browser" page by mocking:
     * <ul>
     *   <li>Page source containing challenge text</li>
     *   <li>Page title "Just a moment..."</li>
     * </ul>
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should detect Cloudflare challenge")
    void testDetectCloudflareChallenge() {

        // Mock page source with Cloudflare challenge text
        when(mockDriver.getPageSource()).thenReturn(
                "<html><body>Checking your browser before accessing...</body></html>"
        );

        // Mock typical Cloudflare challenge title
        when(mockDriver.getTitle()).thenReturn("Just a moment...");

        // Test detection
        boolean detected = handler.isBotDetectionActive();

        // Should detect the Cloudflare challenge
        assertThat(detected).isTrue();
    }


    /**
     * Tests detection of CAPTCHA challenges.
     * <p>
     * Simulates a page with Google reCAPTCHA by mocking:
     * <ul>
     *   <li>Page source containing g-recaptcha div</li>
     *   <li>Page title indicating human verification</li>
     * </ul>
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should detect CAPTCHA")
    void testDetectCaptcha() {
        // Mock page source with reCAPTCHA element
        when(mockDriver.getPageSource()).thenReturn(
                "<html><body><div class='g-recaptcha'></div></body></html>"
        );

        // Mock title suggesting human verification
        when(mockDriver.getTitle()).thenReturn("Verify you're human");

        // Test detection
        boolean detected = handler.isBotDetectionActive();

        // Should detect the CAPTCHA
        assertThat(detected).isTrue();
    }


    /**
     * Tests detection of rate limiting based on HTTP status codes.
     * <p>
     * Simulates a rate-limited response by mocking JavaScript execution
     * that returns HTTP 429 (Too Many Requests) status code.
     * </p>
     *
     * @see BotDetectionHandler#isRateLimited()
     */
    @Test
    @DisplayName("Should detect rate limiting")
    void testDetectRateLimiting() {
        // Mock empty page source and title
        when(mockDriver.getPageSource()).thenReturn("");
        when(mockDriver.getTitle()).thenReturn("");

        // Mock JavaScript execution returning 429 status code
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn(429L); // HTTP 429 status

        boolean isRateLimited = handler.isRateLimited();    // Test rate limit detection

        assertThat(isRateLimited).isTrue();                 // Should detect rate limiting
    }


    /**
     * Tests that detection returns false when no bot challenges are present.
     * <p>
     * Simulates a normal webpage without any bot detection elements by:
     * <ul>
     *   <li>Providing normal page content</li>
     *   <li>Returning HTTP 200 status</li>
     *   <li>Throwing NoSuchElementException for challenge elements</li>
     * </ul>
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should return false when no detection active")
    void testNoDetectionActive() {

        // Mock normal page content
        when(mockDriver.getPageSource()).thenReturn(
                "<html><body>Normal page content</body></html>"
        );

        // Mock normal page title
        when(mockDriver.getTitle()).thenReturn("Example Page");

        // Mock JavaScript returning 200 OK status
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn(200L);

        // Mock no challenge elements found
        // Simulate element not found when looking for challenge indicators
        when(mockDriver.findElement(any(By.class)))
                .thenThrow(new org.openqa.selenium.NoSuchElementException("Not found"));

        boolean detected = handler.isBotDetectionActive();          // Test detection

        assertThat(detected).isFalse();                             // Should not detect any bot challenges
    }


    /**
     * Tests the bot detection handling mechanism.
     * <p>
     * Simulates a challenge that automatically resolves by changing the
     * page source from challenge content to normal content between calls.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     * @see BotDetectionHandler#handleBotDetection()
     */
    @Test
    @DisplayName("Should handle bot detection")
    void testHandleBotDetection() throws InterruptedException {
        // Simulate challenge that auto-resolves
        // Mock current URL
        when(mockDriver.getCurrentUrl()).thenReturn("https://example.com");

        // First call returns challenge, second returns normal content
        when(mockDriver.getPageSource())
                .thenReturn("Checking your browser...")
                .thenReturn("Normal page content");

        // Attempt to handle bot detection
        boolean handled = handler.handleBotDetection();

        // Should attempt to handle
        // Verify page source was checked multiple times (polling behavior)
        verify(mockDriver, atLeast(2)).getPageSource();
    }


    /**
     * Tests bot detection handling with a custom timeout value.
     * <p>
     * Verifies that the handler accepts and uses custom timeout values
     * for challenge resolution.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     * @see BotDetectionHandler#handleBotDetection(int)
     */
    @Test
    @DisplayName("Should handle bot detection with custom timeout")
    void testHandleBotDetectionWithTimeout() throws InterruptedException {
        // Simulate a challenge that auto-resolves
        when(mockDriver.getCurrentUrl()).thenReturn("https://example.com");
        when(mockDriver.getPageSource())
                .thenReturn("Checking your browser...")
                .thenReturn("Normal page content");

        // Handle with a custom 60-second timeout
        boolean handled = handler.handleBotDetection(60);

        // Should attempt to handle
        verify(mockDriver, atLeast(2)).getPageSource();
    }


    /**
     * Tests retrieval of recommended wait time when rate limited.
     * <p>
     * Simulates a rate-limited response without a retry-after header,
     * expecting the default wait time of 60 seconds.
     * </p>
     *
     * @see BotDetectionHandler#getRecommendedWaitTime()
     */
    @Test
    @DisplayName("Should get recommended wait time")
    void testGetRecommendedWaitTime() {
        // First call checks if rate limited (returns true)
        // Mock status check returning 429 (rate-limited)
        when(((JavascriptExecutor) mockDriver).executeScript(contains("responseStatus")))
                .thenReturn(429L);

        // The second call gets retry-after value
        // Mock no retry-after header present
        when(((JavascriptExecutor) mockDriver).executeScript(contains("retry-after")))
                .thenReturn(null);

        // Mock rate limited page content
        when(mockDriver.getPageSource()).thenReturn("Rate limited");

        // Get the recommended wait time
        int waitTime = handler.getRecommendedWaitTime();

        // Should return default 60 seconds when no retry-after header
        assertThat(waitTime).isEqualTo(60);
    }


    /**
     * Tests the monitoring script injection functionality.
     * <p>
     * Verifies that starting monitoring injects the bot detection
     * monitoring JavaScript into the page.
     * </p>
     *
     * @see BotDetectionHandler#startMonitoring()
     */
    @Test
    @DisplayName("Should start monitoring")
    void testStartMonitoring() {
        handler.startMonitoring(); // Start bot detection monitoring

        // Verify a script containing a monitoring function was injected
        verify((JavascriptExecutor) mockDriver).executeScript(contains("__botDetectionMonitor"));
    }


    /**
     * Tests application of anti-detection strategies.
     * <p>
     * Verifies that anti-detection JavaScript is injected to override
     * browser APIs that might reveal automation.
     * </p>
     *
     * @see BotDetectionHandler#applyAntiDetectionStrategies()
     */
    @Test
    @DisplayName("Should apply anti-detection strategies")
    void testApplyAntiDetectionStrategies() {
        handler.applyAntiDetectionStrategies();     // Apply anti-detection measures

        // Should override fetch and XMLHttpRequest
        // Verify script overriding fetch API was injected
        verify((JavascriptExecutor) mockDriver).executeScript(
                contains("window.fetch")
        );
    }


    /**
     * Tests graceful error handling during detection checks.
     * <p>
     * Simulates a page source retrieval error and verifies the handler
     * doesn't crash and returns false for detection.
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should handle detection check errors gracefully")
    void testDetectionCheckError() {
        // Mock page source throwing exception
        when(mockDriver.getPageSource()).thenThrow(new RuntimeException("Page error"));
        when(mockDriver.getTitle()).thenReturn("Normal title"); // Mock normal title

        // Mock no challenge elements found
        when(mockDriver.findElement(any(By.class)))
                .thenThrow(new org.openqa.selenium.NoSuchElementException("Not found"));

        boolean detected = handler.isBotDetectionActive(); // Test detection with an error condition

        assertThat(detected).isFalse(); // Should handle error gracefully and return false
    }


    /**
     * Tests detection of rate limiting through page content.
     * <p>
     * Verifies rate limit detection based on page content even when
     * HTTP status is 200, simulating rate limit messages in HTML.
     * </p>
     *
     * @see BotDetectionHandler#isRateLimited()
     */
    @Test
    @DisplayName("Should detect multiple rate limit indicators")
    void testMultipleRateLimitIndicators() {
        // Mock page with a rate limit message in content
        when(mockDriver.getPageSource()).thenReturn(
                "<html><body>Error: Too many requests. Please slow down.</body></html>"
        );
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn(200L); // Mock 200 status (not 429)

        boolean isRateLimited = handler.isRateLimited(); // Test rate limit detection

        assertThat(isRateLimited).isTrue(); // Should detect rate limiting from content
    }


    /**
     * Tests handling of challenges that don't resolve within timeout.
     * <p>
     * Simulates a persistent challenge page that doesn't resolve,
     * testing timeout behavior.
     * </p>
     *
     * @see BotDetectionHandler#handleBotDetection()
     */
    @Test
    @DisplayName("Should handle challenge resolution timeout")
    void testChallengeResolutionTimeout() {
        when(mockDriver.getCurrentUrl()).thenReturn("https://example.com/challenge"); // Mock challenge URL
        when(mockDriver.getPageSource()).thenReturn("Checking your browser..."); // Mock persistent challenge content

        // Mock element isn't found (challenge still active)
        when(mockDriver.findElement(any(By.class)))
                .thenThrow(new org.openqa.selenium.NoSuchElementException("Element not found"));

        // Attempt to handle (will time out)
        boolean handled = handler.handleBotDetection();

        // Should attempt to handle but may fail
        verify(mockDriver, atLeastOnce()).getPageSource();
    }


    /**
     * Tests the monitoring stop functionality.
     * <p>
     * Verifies that monitoring can be started and stopped without errors.
     * </p>
     *
     * @see BotDetectionHandler#stopMonitoring()
     */
    @Test
    @DisplayName("Should stop monitoring")
    void testStopMonitoring() {
        handler.startMonitoring(); // Start monitoring first
        handler.stopMonitoring(); // Then stop monitoring

        // Simple verification that no exception was thrown
        assertThat(handler).isNotNull();
    }


    /**
     * Tests detection based on HTTP blocking status codes.
     * <p>
     * Verifies that HTTP 403 (Forbidden) status triggers bot detection.
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should detect blocking status codes")
    void testDetectBlockingStatus() {
        when(mockDriver.getPageSource()).thenReturn("Normal page"); // Mock normal page content
        when(mockDriver.getTitle()).thenReturn("Normal title");
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn(403L); // Mock 403 Forbidden status

        boolean detected = handler.isBotDetectionActive(); // Test detection

        assertThat(detected).isTrue(); // Should detect blocking based on status code
    }


    /**
     * Tests natural interaction simulation with page elements.
     * <p>
     * Verifies that the handler attempts to find and interact with
     * clickable elements like buttons during challenge handling.
     * </p>
     *
     * @see BotDetectionHandler#handleBotDetection()
     */
    @Test
    @DisplayName("Should handle natural interaction")
    void testNaturalInteraction() {
        WebElement mockButton = mock(WebElement.class);     // Mock a button element
        when(mockButton.isDisplayed()).thenReturn(true);
        when(mockButton.isEnabled()).thenReturn(true);
        when(mockDriver.findElement(By.cssSelector("button"))).thenReturn(mockButton); // Mock finding the button

        handler.handleBotDetection(); // Trigger bot detection handling

        // Should attempt to find interactive elements
        verify(mockDriver, atLeastOnce()).findElement(any(By.class));
    }


    /**
     * Tests detection of Cloudflare Turnstile challenges.
     * <p>
     * Turnstile is Cloudflare's privacy-preserving CAPTCHA alternative.
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should detect Cloudflare turnstile")
    void testDetectCloudflareTurnstile() {
        when(mockDriver.getPageSource()).thenReturn(
                "<html><body><div class='cf-turnstile'></div></body></html>"
        ); // Mock page with Turnstile element
        when(mockDriver.getTitle()).thenReturn("Security Check");

        boolean detected = handler.isBotDetectionActive(); // Test detection

        assertThat(detected).isTrue(); // Should detect Turnstile challenge
    }


    /**
     * Tests detection of hCaptcha challenges.
     * <p>
     * hCaptcha is an alternative to Google reCAPTCHA.
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should detect hCaptcha")
    void testDetectHCaptcha() {
        when(mockDriver.getPageSource()).thenReturn(
                "<html><body><div class='h-captcha'></div></body></html>"
        ); // Mock page with hCaptcha element
        when(mockDriver.getTitle()).thenReturn("Verify");

        boolean detected = handler.isBotDetectionActive(); // Test detection

        assertThat(detected).isTrue(); // Should detect hCaptcha
    }


    /**
     * Tests detection of challenge elements by their ID attributes.
     * <p>
     * Some challenges use specific element IDs rather than classes.
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should check for challenge elements by ID")
    void testDetectChallengeById() {
        when(mockDriver.getPageSource()).thenReturn("Normal page"); // Mock normal page content
        when(mockDriver.getTitle()).thenReturn("Normal title");

        WebElement challengeElement = mock(WebElement.class); // Mock finding a challenge element by ID
        when(mockDriver.findElement(By.id("cf-challenge-running"))).thenReturn(challengeElement);

        boolean detected = handler.isBotDetectionActive(); // Test detection

        assertThat(detected).isTrue(); // Should detect a challenge element
    }


    /**
     * Tests that a monitoring script is only injected once.
     * <p>
     * Verifies idempotency of startMonitoring() calls to prevent
     * duplicate script injection.
     * </p>
     *
     * @see BotDetectionHandler#startMonitoring()
     */
    @Test
    @DisplayName("Should handle multiple start monitoring calls")
    void testMultipleStartMonitoringCalls() {
        // Call start monitoring multiple times
        handler.startMonitoring();
        handler.startMonitoring();
        handler.startMonitoring();

        // Verify script was injected exactly once despite multiple calls
        verify((JavascriptExecutor) mockDriver, times(1))
                .executeScript(contains("__botDetectionMonitor"));
    }


    /**
     * Tests wait time calculation when not rate limited.
     * <p>
     * Verifies that the recommended wait time is 0 when no rate limiting
     * is detected.
     * </p>
     *
     * @see BotDetectionHandler#getRecommendedWaitTime()
     */
    @Test
    @DisplayName("Should return default wait time when not rate limited")
    void testWaitTimeWhenNotRateLimited() {
        when(mockDriver.getPageSource()).thenReturn("Normal page"); // Mock normal page without rate limiting
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn(200L); // Mock 200 OK status

        int waitTime = handler.getRecommendedWaitTime(); // Get wait time

        assertThat(waitTime).isEqualTo(0); // Should return 0 (no wait needed)
    }


    /**
     * Tests error handling during JavaScript injection.
     * <p>
     * Verifies that script injection errors don't crash the handler
     * and are handled gracefully.
     * </p>
     *
     * @see BotDetectionHandler#startMonitoring()
     * @see BotDetectionHandler#applyAntiDetectionStrategies()
     */
    @Test
    @DisplayName("Should handle script injection errors")
    void testScriptInjectionError() {
        // Mock script execution throwing exception
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenThrow(new RuntimeException("Script error"));

        // Should not throw when starting monitoring
        handler.startMonitoring();

        // Should not throw when applying strategies
        handler.applyAntiDetectionStrategies();
    }


    /**
     * Tests detection based on Cloudflare Ray ID presence.
     * <p>
     * Ray IDs are unique identifiers in Cloudflare error pages.
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should detect ray ID indicator")
    void testDetectRayId() {
        when(mockDriver.getPageSource()).thenReturn(
                "<html><body>Ray ID: 1234567890abcdef</body></html>"
        ); // Mock page with Ray ID (Cloudflare indicator)
        when(mockDriver.getTitle()).thenReturn("Access Denied");

        boolean detected = handler.isBotDetectionActive(); // Test detection

        assertThat(detected).isTrue(); // Should detect based on Ray ID presence
    }


    /**
     * Tests waiting for automatic challenge resolution.
     * <p>
     * Simulates a Cloudflare challenge that resolves automatically,
     * with URL changing from challenge path to normal path.
     * </p>
     *
     * @see BotDetectionHandler#handleBotDetection()
     */
    @Test
    @DisplayName("Should wait for automatic challenge resolution")
    void testWaitForChallengeResolution() {
        // The first call returns a challenge, second returns normal page
        when(mockDriver.getCurrentUrl())
                .thenReturn("https://example.com/cdn-cgi/challenge")
                .thenReturn("https://example.com");

        // Mock page content changing from challenge to normal
        when(mockDriver.getPageSource())
                .thenReturn("<html><body>Just a moment...</body></html>")
                .thenReturn("<html><body>Welcome to Example.com</body></html>");

        handler.handleBotDetection(); // Handle bot detection

        // Should check a page source multiple times
        verify(mockDriver, atLeast(2)).getPageSource();
        // Should check URL for cdn-cgi path
        verify(mockDriver, atLeast(1)).getCurrentUrl();
    }


    /**
     * Tests handling of null JavaScript execution results.
     * <p>
     * Verifies that null returns from JavaScript execution don't
     * cause NullPointerExceptions.
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should handle null JavaScript execution results")
    void testNullJavaScriptResults() {
        when(mockDriver.getPageSource()).thenReturn("Normal page"); // Mock normal page
        when(mockDriver.getTitle()).thenReturn("Normal title");
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn(null); // Mock JavaScript returning null

        // Mock no challenge elements found
        when(mockDriver.findElement(any(By.class)))
                .thenThrow(new org.openqa.selenium.NoSuchElementException("Not found"));

        boolean detected = handler.isBotDetectionActive(); // Test detection

        assertThat(detected).isFalse(); // Should handle null gracefully
    }


    /**
     * Tests parsing of retry-after header values.
     * <p>
     * Verifies correct parsing of retry-after header when rate limited,
     * expecting the header value to be returned as wait time.
     * </p>
     *
     * @see BotDetectionHandler#getRecommendedWaitTime()
     */
    @Test
    @DisplayName("Should parse retry after header correctly")
    void testParseRetryAfterHeader() {
        // First call checks if rate limited (returns true)
        when(((JavascriptExecutor) mockDriver).executeScript(contains("responseStatus")))
                .thenReturn(429L);

        // The second call gets retry-after value
        // Mock retry-after header with 120 seconds
        when(((JavascriptExecutor) mockDriver).executeScript(contains("retry-after")))
                .thenReturn("120");

        // Mock rate limited content
        when(mockDriver.getPageSource()).thenReturn("Rate limited");

        int waitTime = handler.getRecommendedWaitTime(); // Get wait time

        assertThat(waitTime).isEqualTo(120); // Should return parsed retry-after value
    }


    /**
     * Tests handling of element not found exceptions.
     * <p>
     * Verifies that NoSuchElementException when searching for challenge
     * elements is handled gracefully.
     * </p>
     *
     * @see BotDetectionHandler#isBotDetectionActive()
     */
    @Test
    @DisplayName("Should handle element not found when checking by class")
    void testElementNotFoundByClass() {
        when(mockDriver.getPageSource()).thenReturn("Normal page"); // Mock normal page
        when(mockDriver.getTitle()).thenReturn("Normal title");

        // Mock element isn't found for any selector
        when(mockDriver.findElement(any(By.class)))
                .thenThrow(new org.openqa.selenium.NoSuchElementException("Not found"));

        boolean detected = handler.isBotDetectionActive(); // Test detection

        assertThat(detected).isFalse(); // Should return false when elements not found
    }


    /**
     * Tests natural interaction with multiple button types.
     * <p>
     * Verifies that the handler tries multiple selectors to find
     * interactive elements, falling back from buttons to submit inputs.
     * </p>
     *
     * @see BotDetectionHandler#handleBotDetection()
     */
    @Test
    @DisplayName("Should perform natural interaction with multiple button types")
    void testNaturalInteractionMultipleButtons() {
        // Mock button not found
        when(mockDriver.findElement(By.cssSelector("button")))
                .thenThrow(new org.openqa.selenium.NoSuchElementException("No button"));

        // Mock submit input found
        WebElement submitInput = mock(WebElement.class);
        when(submitInput.isDisplayed()).thenReturn(true);
        when(submitInput.isEnabled()).thenReturn(true);
        when(mockDriver.findElement(By.cssSelector("input[type='submit']")))
                .thenReturn(submitInput); // Mock finding submits input as fallback

        handler.handleBotDetection(); // Handle bot detection

        // Should try multiple selectors,
        // Verify attempted to find the button first
        verify(mockDriver).findElement(By.cssSelector("button"));
        verify(mockDriver).findElement(By.cssSelector("input[type='submit']")); // Verify fell back to submit input
    }
}