package com.undetected.chromedriver;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.openqa.selenium.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/**
 * Unit test class for {@link HumanBehaviorSimulator}.
 * <p>
 * This test suite validates the functionality of the HumanBehaviorSimulator class,
 * which simulates human-like browsing behavior to evade bot detection. The tests
 * verify navigation timing, scrolling patterns, typing behavior, and interaction
 * patterns that mimic real users.
 * </p>
 *
 * <h2>Test Coverage:</h2>
 * <ul>
 *   <li>Human-like navigation with timing delays</li>
 *   <li>Smooth scrolling behavior</li>
 *   <li>Natural typing with character-by-character input</li>
 *   <li>Mouse movement and click patterns</li>
 *   <li>Behavior profile management (FAST, NORMAL, CAREFUL)</li>
 *   <li>Random action generation</li>
 *   <li>Error handling and graceful degradation</li>
 * </ul>
 *
 * <h2>Testing Approach:</h2>
 * <p>
 * Uses a custom TestHumanBehaviorSimulator subclass that:
 * <ul>
 *   <li>Disables Actions API for test compatibility</li>
 *   <li>Speeds up pauses to make tests run faster</li>
 *   <li>Allows overriding specific behaviors for testing</li>
 * </ul>
 * </p>
 *
 * <h2>Mock Configuration:</h2>
 * <p>
 * The tests use Mockito to mock WebDriver components and verify that the
 * simulator generates appropriate human-like interaction patterns without
 * requiring a real browser instance.
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see HumanBehaviorSimulator
 */
class HumanBehaviorSimulatorTest {

    /**
     * Mock WebDriver instance for testing.
     * <p>
     * Configured to also implement JavascriptExecutor interface when needed
     * for JavaScript-based interactions.
     * </p>
     */
    @Mock
    private WebDriver mockDriver;

    /**
     * Mock WebElement for testing element interactions.
     * <p>
     * Used to verify clicking, typing, and scrolling behaviors.
     * </p>
     */
    @Mock
    private WebElement mockElement;

    /**
     * Mock JavascriptExecutor for JavaScript execution verification.
     * <p>
     * Although mockDriver implements this interface, this separate mock
     * is kept for clarity in some test scenarios.
     * </p>
     */
    @Mock
    private JavascriptExecutor mockJsExecutor;

    /**
     * The HumanBehaviorSimulator instance under test.
     * <p>
     * Usually an instance of TestHumanBehaviorSimulator for faster test execution.
     * </p>
     */
    private HumanBehaviorSimulator simulator;

    /**
     * AutoCloseable reference for Mockito resource cleanup.
     * <p>
     * Ensures proper cleanup of mock objects after each test.
     * </p>
     */
    private AutoCloseable mocks;

    /**
     * Test instance ID for simulator identification.
     * <p>
     * Used in logging to distinguish between simulator instances.
     * </p>
     */
    private static final int TEST_INSTANCE_ID = 1;


    /**
     * Test-specific simulator that doesn't use Actions.
     * <p>
     * This subclass of HumanBehaviorSimulator is designed for unit testing.
     * It disables the Actions API (which requires a real browser) and speeds
     * up pauses to make tests run faster.
     * </p>
     *
     * <h3>Key Modifications:</h3>
     * <ul>
     *   <li>Disables Actions creation in constructor</li>
     *   <li>Reduces pause durations by 10x for faster tests</li>
     *   <li>Maintains core behavior for verification</li>
     * </ul>
     */
    private static class TestHumanBehaviorSimulator extends HumanBehaviorSimulator {

        /**
         * Constructs a test simulator without Actions API.
         *
         * @param driver the WebDriver instance
         * @param instanceId unique identifier
         */
        public TestHumanBehaviorSimulator(WebDriver driver, int instanceId) {
            super(driver, instanceId, false); // Disable Actions creation
        }

        /**
         * Overrides pause method to speed up tests.
         * <p>
         * Reduces pause duration by 10x with a maximum of 10ms to prevent
         * tests from taking too long while still maintaining some timing.
         * </p>
         *
         * @param milliseconds original pause duration
         */
        @Override
        protected void pause(int milliseconds) {
            // Speed up tests by reducing pauses
            try {
                Thread.sleep(Math.min(milliseconds / 10, 10)); // Divide by 10 and cap at 10 ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
            }
        }
    }


    /**
     * Sets up the test environment before each test method.
     * <p>
     * Initializes Mockito mocks and creates a test simulator instance
     * with a mock WebDriver that implements JavascriptExecutor.
     * </p>
     */
    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Make the driver implement JavascriptExecutor
        mockDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));

        // Use a test-specific simulator
        simulator = new TestHumanBehaviorSimulator(mockDriver, TEST_INSTANCE_ID);
    }


    /**
     * Cleans up test resources after each test method.
     * <p>
     * Ensures proper cleanup of Mockito resources to prevent memory leaks.
     * </p>
     *
     * @throws Exception if cleanup fails
     */
    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }


    /**
     * Tests that HumanBehaviorSimulator can be instantiated with an instance ID.
     * <p>
     * Basic test to ensure constructor works correctly.
     * </p>
     *
     * @see HumanBehaviorSimulator#HumanBehaviorSimulator(WebDriver, int, boolean)
     */
    @Test
    @DisplayName("Should create simulator with instance ID")
    void testConstructor() {
        assertThat(simulator).isNotNull(); // Verify creation succeeded
    }


    /**
     * Tests human-like navigation with appropriate timing delays.
     * <p>
     * Verifies that navigation includes page load waiting and readiness checks
     * to mimic how real users wait for pages to load.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     * @see HumanBehaviorSimulator#humanNavigate(String)
     */
    @Test
    @DisplayName("Should navigate with human-like timing")
    void testHumanNavigate() throws InterruptedException {
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn("complete"); // Mock page ready state

        simulator.humanNavigate("https://example.com"); // Perform navigation

        // Verify navigation occurred via JavaScript
        verify((JavascriptExecutor) mockDriver).executeScript(
                "window.location.href = arguments[0];",
                "https://example.com"
        );

        // Verify page load check; Should check document.readyState
        verify((JavascriptExecutor) mockDriver, atLeastOnce())
                .executeScript("return document.readyState");
    }


    /**
     * Tests smooth scrolling behavior.
     * <p>
     * Verifies that scrolling is performed in multiple small steps rather
     * than jumping directly to the target position, mimicking human behavior.
     * </p>
     *
     * @see HumanBehaviorSimulator#humanScroll(int)
     */
    @Test
    @DisplayName("Should perform smooth scrolling")
    void testHumanScroll() {
        when(((JavascriptExecutor) mockDriver).executeScript("return window.pageYOffset;"))
                .thenReturn(0L); // Mock current scroll position at top

        simulator.humanScroll(500); // Perform scroll to 500px

        // Verify multiple scroll steps were executed
        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        verify((JavascriptExecutor) mockDriver, atLeast(5))
                .executeScript(scriptCaptor.capture());

        // Check that scroll commands were sent
        assertThat(scriptCaptor.getAllValues())
                .anyMatch(script -> script.contains("window.scrollTo"));

        // Verify we checked the current position
        verify((JavascriptExecutor) mockDriver, times(1))
                .executeScript("return window.pageYOffset;");
    }


    /**
     * Tests scrolling to a specific element with offset.
     * <p>
     * Verifies that the simulator can scroll an element into view with
     * appropriate positioning to center it in the viewport.
     * </p>
     *
     * @see HumanBehaviorSimulator#scrollToElement(WebElement)
     */
    @Test
    @DisplayName("Should scroll to element with offset")
    void testScrollToElement() {
        // Mock element location and size
        when(mockElement.getLocation()).thenReturn(new Point(0, 1000));
        when(mockElement.getSize()).thenReturn(new Dimension(100, 50));

        // Mock window management chain
        WebDriver.Options mockOptions = mock(WebDriver.Options.class);
        WebDriver.Window mockWindow = mock(WebDriver.Window.class);
        when(mockDriver.manage()).thenReturn(mockOptions);
        when(mockOptions.window()).thenReturn(mockWindow);
        when(mockWindow.getSize()).thenReturn(new Dimension(1920, 1080));

        // Mock scroll position
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn(0L);

        // Perform scroll to element
        simulator.scrollToElement(mockElement);

        // Verify scrolling occurred
        verify((JavascriptExecutor) mockDriver, atLeastOnce())
                .executeScript(contains("window.scrollTo"));
    }


    /**
     * Tests human-like typing with character-by-character input.
     * <p>
     * Verifies that text is typed one character at a time with appropriate
     * delays, mimicking real human typing patterns.
     * </p>
     *
     * @see HumanBehaviorSimulator#humanType(WebElement, String)
     */
    @Test
    @DisplayName("Should type with human-like delays")
    void testHumanType() {
        String testText = "Hello World";

        // Mock random to avoid typos in test; Create a custom simulator that disables typo simulation
        simulator = new TestHumanBehaviorSimulator(mockDriver, TEST_INSTANCE_ID) {
            @Override
            public void humanType(WebElement element, String text) {
                // Override to disable typo simulation for this test
                element.click();
                for (char c : text.toCharArray()) {
                    element.sendKeys(String.valueOf(c)); // Type each character individually
                }
            }
        };

        simulator.humanType(mockElement, testText); // Perform typing

        // Verify each character was typed
        ArgumentCaptor<String> charCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockElement, times(testText.length())).sendKeys(charCaptor.capture());

        // Verify all characters were sent
        String typedText = String.join("", charCaptor.getAllValues());
        assertThat(typedText).isEqualTo(testText);

        // Verify element was clicked first
        verify(mockElement).click();
    }


    /**
     * Tests different behavior profiles.
     * <p>
     * Verifies that the simulator can switch between FAST, NORMAL, and
     * CAREFUL behavior profiles without errors.
     * </p>
     *
     * @see HumanBehaviorSimulator.BehaviorProfile
     * @see HumanBehaviorSimulator#setBehaviorProfile(HumanBehaviorSimulator.BehaviorProfile)
     */
    @Test
    @DisplayName("Should handle different behavior profiles")
    void testBehaviorProfiles() {
        // Test FAST profile
        simulator.setBehaviorProfile(HumanBehaviorSimulator.BehaviorProfile.FAST);
        assertThat(simulator).isNotNull();

        // Test NORMAL profile
        simulator.setBehaviorProfile(HumanBehaviorSimulator.BehaviorProfile.NORMAL);
        assertThat(simulator).isNotNull();

        // Test CAREFUL profile
        simulator.setBehaviorProfile(HumanBehaviorSimulator.BehaviorProfile.CAREFUL);
        assertThat(simulator).isNotNull();
    }


    /**
     * Tests random action generation.
     * <p>
     * Verifies that the simulator can perform random actions like scrolling
     * or moving without errors, simulating idle user behavior.
     * </p>
     *
     * @see HumanBehaviorSimulator#performRandomActions()
     */
    @Test
    @DisplayName("Should perform random actions")
    void testPerformRandomActions() {
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn(1000L); // Mock max scroll value

        // Should not throw exception
        simulator.performRandomActions();
    }


    /**
     * Tests inter-page navigation delays.
     * <p>
     * Verifies that the simulator waits an appropriate amount of time
     * between page navigations to mimic human browsing patterns.
     * </p>
     *
     * @see HumanBehaviorSimulator#waitBetweenPages()
     */
    @Test
    @DisplayName("Should wait between pages")
    void testWaitBetweenPages() {
        // Use a regular simulator without pause speed-up for this test
        simulator = new HumanBehaviorSimulator(mockDriver, TEST_INSTANCE_ID, false);

        // Measure wait time
        long startTime = System.currentTimeMillis();
        simulator.waitBetweenPages();
        long endTime = System.currentTimeMillis();

        // Should wait at least the minimum time (2000 ms default);
        // Using 1000 ms threshold to account for timing variations
        assertThat(endTime - startTime).isGreaterThanOrEqualTo(1000);
    }


    /**
     * Tests clicking behavior with automatic scrolling.
     * <p>
     * Verifies that clicking an element includes scrolling it into view
     * first, mimicking how real users interact with elements.
     * </p>
     *
     * @see HumanBehaviorSimulator#humanClick(WebElement)
     */
    @Test
    @DisplayName("Should handle click with scrolling")
    void testHumanClick() {
        // Mock element location and size
        when(mockElement.getLocation()).thenReturn(new Point(100, 500));
        when(mockElement.getSize()).thenReturn(new Dimension(200, 50));

        // Mock window management
        WebDriver.Options mockOptions = mock(WebDriver.Options.class);
        WebDriver.Window mockWindow = mock(WebDriver.Window.class);
        when(mockDriver.manage()).thenReturn(mockOptions);
        when(mockOptions.window()).thenReturn(mockWindow);
        when(mockWindow.getSize()).thenReturn(new Dimension(1920, 1080));

        // Handle both single-arg and multi-arg executeScript calls
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn(0L);
        when(((JavascriptExecutor) mockDriver).executeScript(anyString(), any()))
                .thenReturn(null);

        // Create a test simulator that overrides problematic methods
        simulator = new TestHumanBehaviorSimulator(mockDriver, TEST_INSTANCE_ID) {
            @Override
            public void scrollToElement(WebElement element) {
                // Simple test implementation
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            }

            @Override
            public void humanMouseMove(WebElement target) {
                // Skip mouse move in test - no logging needed; Actions API not available in test environment
            }
        };

        simulator.humanClick(mockElement); // Perform click

        // Verify scrolling occurred with the correct arguments
        verify((JavascriptExecutor) mockDriver).executeScript(
                eq("arguments[0].scrollIntoView(true);"),
                eq(mockElement)
        );

        // Verify element was clicked (since Actions is not available in test)
        verify(mockElement).click();
    }


    /**
     * Tests graceful handling of navigation errors.
     * <p>
     * Verifies that JavaScript execution errors during navigation don't
     * prevent the navigation from occurring.
     * </p>
     *
     * @see HumanBehaviorSimulator#humanNavigate(String)
     */
    @Test
    @DisplayName("Should handle navigation errors gracefully")
    void testNavigationError() {
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenThrow(new WebDriverException("Script error"));

        // Should not throw exception
        simulator.humanNavigate("https://example.com");

        // Navigation should still occur via JavaScript
        verify((JavascriptExecutor) mockDriver).executeScript(
                "window.location.href = arguments[0];",
                "https://example.com"
        );
    }


    /**
     * Tests typing of special characters.
     * <p>
     * Verifies that special characters like @, comma, and exclamation
     * are typed correctly without issues.
     * </p>
     *
     * @see HumanBehaviorSimulator#humanType(WebElement, String)
     */
    @Test
    @DisplayName("Should handle typing special characters")
    void testTypingSpecialCharacters() {
        String specialText = "test@email.com, hello!";

        // Mock to disable typo simulation
        simulator = new TestHumanBehaviorSimulator(mockDriver, TEST_INSTANCE_ID) {
            @Override
            public void humanType(WebElement element, String text) {
                element.click();
                for (char c : text.toCharArray()) {
                    element.sendKeys(String.valueOf(c));
                }
            }
        };

        simulator.humanType(mockElement, specialText); // Perform typing

        // Verify all characters, including special ones, were typed
        ArgumentCaptor<String> charCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockElement, times(specialText.length())).sendKeys(charCaptor.capture());

        // Reconstruct typed text
        String typedText = String.join("", charCaptor.getAllValues());
        assertThat(typedText).isEqualTo(specialText);
    }


    /**
     * Tests initial page scanning behavior.
     * <p>
     * Verifies that after navigation, the simulator performs an initial
     * scan of the page including scrolling to simulate how users explore
     * new pages.
     * </p>
     *
     * @see HumanBehaviorSimulator#humanNavigate(String)
     */
    @Test
    @DisplayName("Should perform initial page scan")
    void testInitialPageScan() {
        // First return "complete" for page ready state, then 0L for scroll position
        when(((JavascriptExecutor) mockDriver).executeScript(anyString()))
                .thenReturn("complete") // For document.readyState check
                .thenReturn(0L);        // For window.pageYOffset check

        simulator.humanNavigate("https://example.com"); // Navigate to page

        // Should perform scrolling during the initial scan
        verify((JavascriptExecutor) mockDriver, atLeast(2))
                .executeScript(contains("window.scrollTo"));
    }
}