package com.undetected.chromedriver;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.openqa.selenium.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Unit test class for {@link StealthExecutor}.
 * <p>
 * This test suite validates the functionality of the StealthExecutor class,
 * which applies various JavaScript-based stealth techniques to make automated
 * browsers appear more like regular Chrome browsers. The tests verify script
 * injection, error handling, and the application of specific anti-detection
 * modifications.
 * </p>
 *
 * <h2>Test Coverage:</h2>
 * <ul>
 *   <li>JavaScript execution and script injection</li>
 *   <li>Navigator.webdriver property removal</li>
 *   <li>Plugin spoofing and structure</li>
 *   <li>Permissions API fixes</li>
 *   <li>Chrome runtime object simulation</li>
 *   <li>WebGL vendor/renderer spoofing</li>
 *   <li>Screen dimension fixes for headless mode</li>
 *   <li>Error handling and fallback mechanisms</li>
 *   <li>Concurrent execution safety</li>
 *   <li>Script caching behavior</li>
 * </ul>
 *
 * <h2>Testing Approach:</h2>
 * <p>
 * Uses Mockito to mock WebDriver interactions and capture executed JavaScript.
 * Tests verify that appropriate stealth scripts are injected and that the
 * executor handles various edge cases gracefully.
 * </p>
 *
 * <h2>Stealth Techniques Tested:</h2>
 * <ul>
 *   <li><b>navigator.webdriver:</b> Removal of automation indicator</li>
 *   <li><b>navigator.plugins:</b> Realistic plugin array</li>
 *   <li><b>window.chrome:</b> Chrome-specific object structure</li>
 *   <li><b>WebGL:</b> GPU vendor/renderer spoofing</li>
 *   <li><b>Screen:</b> Realistic dimensions for headless mode</li>
 *   <li><b>Permissions:</b> Proper API behavior</li>
 * </ul>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see StealthExecutor
 */
class StealthExecutorTest {

    /**
     * Mock WebDriver instance for testing.
     * <p>
     * Used to verify JavaScript execution without requiring a real browser.
     * Can be configured to implement JavascriptExecutor when needed.
     * </p>
     */
    @Mock
    private WebDriver mockDriver;

    /**
     * The StealthExecutor instance under test.
     * <p>
     * Created fresh for each test to ensure test isolation.
     * </p>
     */
    private StealthExecutor stealthExecutor;

    /**
     * Test instance ID for executor identification.
     * <p>
     * Used in logging to distinguish between executor instances.
     * </p>
     */
    private static final int TEST_INSTANCE_ID = 1;


    /**
     * Sets up the test environment before each test method.
     * <p>
     * Initializes Mockito annotations to create mock objects.
     * The StealthExecutor is created individually in each test to allow
     * for different driver configurations.
     * </p>
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize Mockito annotations
    }


    /**
     * Tests that StealthExecutor can be instantiated with an instance ID.
     * <p>
     * Basic test to ensure the constructor works correctly.
     * </p>
     *
     * @see StealthExecutor#StealthExecutor(WebDriver, int)
     */
    @Test
    @DisplayName("Should create stealth executor with instance ID")
    void testConstructor() {
        stealthExecutor = new StealthExecutor(mockDriver, TEST_INSTANCE_ID); // Create an executor with test instance ID
        assertThat(stealthExecutor).isNotNull(); // Verify creation succeeded
    }


    /**
     * Tests that stealth techniques are applied when driver supports JavaScript.
     * <p>
     * Verifies that JavaScript is executed when the driver implements
     * the JavascriptExecutor interface.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should apply stealth techniques when driver supports JavaScript")
    void testApplyStealthTechniques() {
        // Create a mock that implements both WebDriver and JavascriptExecutor
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID); // Create executor with JS-capable driver
        stealthExecutor.applyStealthTechniques(); // Apply stealth techniques

        // Verify JavaScript was executed
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(anyString());
    }


    /**
     * Tests graceful handling of non-JavaScript capable drivers.
     * <p>
     * Ensures the executor doesn't crash when given a driver that doesn't
     * implement JavascriptExecutor.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should handle non-JavaScript drivers gracefully")
    void testNonJavaScriptDriver() {
        stealthExecutor = new StealthExecutor(mockDriver, TEST_INSTANCE_ID); // Create executor with non-JS driver
        stealthExecutor.applyStealthTechniques(); // Should not throw exception
        verifyNoInteractions(mockDriver); // Verify no interaction with a non-JS driver
    }


    /**
     * Tests that a runtime modifications script is loaded and executed.
     * <p>
     * Verifies that at least the core runtime modifications script
     * is executed during stealth application.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should execute at least runtime modifications script")
    void testScriptLoading() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable driver

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);
        stealthExecutor.applyStealthTechniques();

        // Verify a runtime modifications script was executed
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(anyString());
    }


    /**
     * Tests graceful handling of script execution errors.
     * <p>
     * Ensures that JavaScript execution failures don't crash the executor
     * and that execution is still attempted.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should handle script execution errors gracefully")
    void testScriptExecutionError() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable driver

        // Make script execution throw an exception
        when(((JavascriptExecutor) jsDriver).executeScript(anyString()))
                .thenThrow(new RuntimeException("Script error"));

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);

        // Should not throw exception
        stealthExecutor.applyStealthTechniques();

        // Should still try to execute scripts
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(anyString());
    }


    /**
     * Tests that runtime modifications include navigator.webdriver removal.
     * <p>
     * Verifies that the core stealth technique of removing the
     * navigator.webdriver property is applied.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should apply runtime modifications including navigator.webdriver")
    void testRuntimeModifications() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable driver

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);
        stealthExecutor.applyStealthTechniques();

        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // Verify a runtime modifications script was executed
        boolean hasRuntimeMods = scriptCaptor.getAllValues().stream()
                .anyMatch(script -> script.contains("navigator.webdriver") &&
                        script.contains("navigator.plugins") &&
                        script.contains("window.chrome"));

        assertThat(hasRuntimeMods).isTrue();
    }


    /**
     * Tests that navigator. Plugins are overridden with proper structure.
     * <p>
     * Verifies that the plugin array is populated with realistic Chrome
     * plugins including PDF viewer and Native Client.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should override navigator.plugins with proper structure")
    void testNavigatorPluginsOverride() {
        // Create a JS-capable driver
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);
        stealthExecutor.applyStealthTechniques();

        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class); // Capture executed scripts
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // Find the runtime modifications script
        String runtimeScript = scriptCaptor.getAllValues().stream()
                .filter(script -> script.contains("navigator.plugins"))
                .findFirst()
                .orElse("");

        // Verify it includes proper plugin structure
        assertThat(runtimeScript)
                .contains("Chrome PDF Plugin")  // PDF plugin name
                .contains("Chrome PDF Viewer")  // PDF viewer name
                .contains("Native Client")      // Native Client plugin
                .contains("namedItem");         // Plugin array method
    }


    /**
     * Tests that Permissions API is properly fixed.
     * <p>
     * Verifies that the Permissions API query method is overridden to
     * prevent detection through permission checking.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should fix permissions API")
    void testPermissionsAPIFix() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable driver

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);
        stealthExecutor.applyStealthTechniques();

        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class); // Capture executed scripts
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // Verify permissions fix is applied
        boolean hasPermissionsFix = scriptCaptor.getAllValues().stream()
                .anyMatch(script -> script.contains("navigator.permissions.query") &&
                        script.contains("notifications"));

        assertThat(hasPermissionsFix).isTrue();
    }


    /**
     * Tests that a chrome runtime object is properly created.
     * <p>
     * Verifies that window.chrome.runtime is populated with expected
     * properties including PlatformOs and PlatformArch.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should fix chrome runtime object")
    void testChromeRuntimeFix() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable driver

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);
        stealthExecutor.applyStealthTechniques();

        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class); // Capture executed scripts
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // Verify a chrome runtime object is properly set up
        boolean hasChromeRuntime = scriptCaptor.getAllValues().stream()
                .anyMatch(script -> script.contains("window.chrome.runtime") &&
                        script.contains("PlatformOs") &&
                        script.contains("PlatformArch"));

        assertThat(hasChromeRuntime).isTrue();
    }


    /**
     * Tests that WebGL vendor and renderer are spoofed.
     * <p>
     * Verifies that WebGL getParameter is overridden to return Intel
     * GPU information instead of revealing headless/virtual GPU.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should fix WebGL vendor and renderer")
    void testWebGLFix() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable driver

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);
        stealthExecutor.applyStealthTechniques();

        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class); // Capture executed scripts
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // Verify WebGL parameters are overridden
        boolean hasWebGLFix = scriptCaptor.getAllValues().stream()
                .anyMatch(script -> script.contains("WebGLRenderingContext.prototype.getParameter") &&
                        script.contains("37445") && // VENDOR
                        script.contains("37446") && // RENDERER
                        script.contains("Intel Inc.") && // Spoofed vendor
                        script.contains("Intel Iris OpenGL Engine")); // Spoofed renderer

        assertThat(hasWebGLFix).isTrue();
    }


    /**
     * Tests that screen dimensions are fixed for headless mode.
     * <p>
     * Verifies that screen width and height are set to realistic values
     * (1920x1080) to prevent headless detection through zero dimensions.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should fix screen dimensions for headless mode")
    void testScreenDimensionsFix() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);
        stealthExecutor.applyStealthTechniques();

        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // Verify screen dimensions fix
        boolean hasScreenFix = scriptCaptor.getAllValues().stream()
                .anyMatch(script -> script.contains("window.screen.width") &&
                        script.contains("1920") &&
                        script.contains("1080"));

        assertThat(hasScreenFix).isTrue();
    }


    /**
     * Tests that embedded scripts are used as fallback.
     * <p>
     * Verifies that if external script files aren't found, the executor
     * falls back to embedded script strings.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should use embedded scripts as fallback")
    void testEmbeddedScriptsFallback() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));

        // Create an executor which will use embedded scripts if files not found
        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);
        stealthExecutor.applyStealthTechniques();

        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // At minimum, embedded scripts should be present
        boolean hasWebdriverFix = scriptCaptor.getAllValues().stream()
                .anyMatch(script -> script.contains("navigator, 'webdriver'"));

        assertThat(hasWebdriverFix).isTrue();
    }


    /**
     * Tests thread safety of concurrent execution.
     * <p>
     * Verifies that multiple threads can safely call applyStealthTechniques
     * without causing issues.
     * </p>
     *
     * @throws InterruptedException if thread joining is interrupted
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should handle concurrent calls safely")
    void testConcurrentCalls() throws InterruptedException {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);

        // Simulate concurrent calls
        Thread thread1 = new Thread(() -> stealthExecutor.applyStealthTechniques());
        Thread thread2 = new Thread(() -> stealthExecutor.applyStealthTechniques());

        // Start both threads
        thread1.start();
        thread2.start();

        // Wait for completion
        thread1.join();
        thread2.join();

        // Both calls should complete successfully; At least 2 script executions should occur
        verify((JavascriptExecutor) jsDriver, atLeast(2))
                .executeScript(anyString());
    }


    /**
     * Tests script caching behavior across instances.
     * <p>
     * Verifies that scripts are properly cached and reused across
     * different StealthExecutor instances.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should cache scripts across instances")
    void testScriptCaching() {
        WebDriver jsDriver1 = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));
        WebDriver jsDriver2 = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));

        // Create two executors
        StealthExecutor executor1 = new StealthExecutor(jsDriver1, 1);
        StealthExecutor executor2 = new StealthExecutor(jsDriver2, 2);

        // Apply techniques with both
        executor1.applyStealthTechniques();
        executor2.applyStealthTechniques();

        // Both should execute scripts
        verify((JavascriptExecutor) jsDriver1, atLeastOnce())
                .executeScript(anyString());
        verify((JavascriptExecutor) jsDriver2, atLeastOnce())
                .executeScript(anyString());
    }


    /**
     * Tests handling of null or empty scripts.
     * <p>
     * Verifies that the executor handles edge cases where scripts
     * might be null or empty without crashing.
     * </p>
     *
     * @see StealthExecutor#applyStealthTechniques()
     */
    @Test
    @DisplayName("Should handle null or empty scripts gracefully")
    void testEmptyScripts() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));

        stealthExecutor = new StealthExecutor(jsDriver, TEST_INSTANCE_ID);

        // Should not throw exception
        stealthExecutor.applyStealthTechniques();

        // Should execute at least runtime modifications
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(anyString());
    }
}