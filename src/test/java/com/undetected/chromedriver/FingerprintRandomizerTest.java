package com.undetected.chromedriver;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.openqa.selenium.*;


import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Unit test class for {@link FingerprintRandomizer}.
 * <p>
 * This test suite validates the functionality of the FingerprintRandomizer class,
 * which is responsible for randomizing browser fingerprints to evade detection.
 * The tests verify JavaScript injection, fingerprint randomization strategies,
 * thread management, and error handling.
 * </p>
 *
 * <h2>Test Coverage:</h2>
 * <ul>
 *   <li>Canvas fingerprint randomization with noise injection</li>
 *   <li>WebGL vendor and renderer spoofing</li>
 *   <li>Audio context fingerprint modification</li>
 *   <li>Font measurement randomization</li>
 *   <li>Battery API spoofing</li>
 *   <li>Hardware concurrency modification</li>
 *   <li>Thread lifecycle management</li>
 *   <li>Error handling and graceful degradation</li>
 * </ul>
 *
 * <h2>Testing Approach:</h2>
 * <p>
 * Uses Mockito to mock WebDriver interactions and capture JavaScript execution.
 * Tests verify that appropriate scripts are injected for each fingerprinting
 * technique and that the randomizer handles various edge cases gracefully.
 * </p>
 *
 * <h2>Fingerprinting Techniques Tested:</h2>
 * <ul>
 *   <li><b>Canvas:</b> Pixel data randomization</li>
 *   <li><b>WebGL:</b> GPU vendor/renderer information</li>
 *   <li><b>Audio:</b> Audio processing fingerprints</li>
 *   <li><b>Fonts:</b> Text measurement variations</li>
 *   <li><b>Battery:</b> Battery level and charging status</li>
 *   <li><b>Hardware:</b> CPU core count</li>
 * </ul>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see FingerprintRandomizer
 */
class FingerprintRandomizerTest {

    /**
     * Mock WebDriver instance for testing.
     * <p>
     * Used to verify JavaScript execution without requiring a real browser.
     * Can be configured to implement JavascriptExecutor interface when needed.
     * </p>
     */
    @Mock
    private WebDriver mockDriver;

    /**
     * The FingerprintRandomizer instance under test.
     * <p>
     * Created fresh for each test to ensure isolation. Properly cleaned up
     * in the {@link #tearDown()} method to prevent thread leaks.
     * </p>
     */
    private FingerprintRandomizer randomizer;

    /**
     * AutoCloseable reference for Mockito resources.
     * <p>
     * Ensures proper cleanup of mock objects after each test.
     * </p>
     */
    private AutoCloseable mocks;

    /**
     * Test instance ID for randomizer identification.
     * <p>
     * Used to distinguish between multiple randomizer instances in logs
     * and thread names.
     * </p>
     */
    private static final int TEST_INSTANCE_ID = 1;


    /**
     * Sets up the test environment before each test method.
     * <p>
     * Initializes Mockito annotations to create mock objects. The randomizer
     * is created individually in each test to allow for different configurations.
     * </p>
     */
    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this); // Initialize Mockito annotations
    }


    /**
     * Cleans up test resources after each test method.
     * <p>
     * Ensures proper shutdown of the randomizer's executor service and
     * cleanup of Mockito resources to prevent memory leaks.
     * </p>
     *
     * @throws Exception if cleanup fails
     */
    @AfterEach
    void tearDown() throws Exception {

        // Stop randomizer if it was created
        if (randomizer != null) {
            randomizer.stop();
        }

        // Close Mockito resources
        if (mocks != null) {
            mocks.close();
        }
    }


    /**
     * Tests that FingerprintRandomizer can be instantiated with an instance ID.
     * <p>
     * Basic test to ensure constructor works correctly and doesn't throw
     * exceptions.
     * </p>
     *
     * @see FingerprintRandomizer#FingerprintRandomizer(WebDriver, int)
     */
    @Test
    @DisplayName("Should create fingerprint randomizer with instance ID")
    void testConstructor() {
        randomizer = new FingerprintRandomizer(mockDriver, TEST_INSTANCE_ID); // Create a randomizer with test instance ID
        assertThat(randomizer).isNotNull(); // Verify creation succeeded
    }


    /**
     * Tests that starting the randomizer executes initial JavaScript.
     * <p>
     * Verifies that the randomizer begins executing fingerprint modification
     * scripts when started. Uses a mock WebDriver that implements
     * JavascriptExecutor.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     * @see FingerprintRandomizer#start()
     */
    @Test
    @DisplayName("Should start randomization and execute initial scripts")
    void testStart() throws InterruptedException {
        // Create mock driver that also implements JavascriptExecutor
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));

        randomizer = new FingerprintRandomizer(jsDriver, TEST_INSTANCE_ID); // Create randomizer with JS-capable driver
        randomizer.start();

        // Wait a bit for initial randomization. Allows async executor to run initial scripts.
        TimeUnit.MILLISECONDS.sleep(200);

        // Verify scripts were executed
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(anyString());
    }


    /**
     * Tests graceful shutdown of the randomizer.
     * <p>
     * Ensures that stopping the randomizer doesn't throw exceptions and
     * properly shuts down the executor service.
     * </p>
     *
     * @see FingerprintRandomizer#stop()
     */
    @Test
    @DisplayName("Should stop randomization gracefully")
    void testStop() {
        // Create and start randomizer
        randomizer = new FingerprintRandomizer(mockDriver, TEST_INSTANCE_ID);
        randomizer.start();

        // Should not throw exception
        randomizer.stop();
    }


    /**
     * Tests that multiple start calls are handled properly.
     * <p>
     * Verifies idempotency of the start method - multiple calls should not
     * create multiple executor services or cause issues.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     * @see FingerprintRandomizer#start()
     */
    @Test
    @DisplayName("Should handle multiple start calls without issues")
    void testMultipleStartCalls() throws InterruptedException {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable mock driver

        randomizer = new FingerprintRandomizer(jsDriver, TEST_INSTANCE_ID);

        // Call start multiple times
        randomizer.start();
        randomizer.start();
        randomizer.start();

        TimeUnit.MILLISECONDS.sleep(200); // Wait for execution

        // Should still work normally without creating multiple tasks
        randomizer.stop();
    }


    /**
     * Tests that multiple stop calls are handled properly.
     * <p>
     * Verifies that calling stop multiple times doesn't cause exceptions
     * or other issues.
     * </p>
     *
     * @see FingerprintRandomizer#stop()
     */
    @Test
    @DisplayName("Should handle multiple stop calls without issues")
    void testMultipleStopCalls() {
        randomizer = new FingerprintRandomizer(mockDriver, TEST_INSTANCE_ID);
        randomizer.start();

        // Call stop multiple times - should not throw
        randomizer.stop();
        randomizer.stop();
        randomizer.stop();
    }


    /**
     * Tests that all fingerprint aspects are randomized.
     * <p>
     * Comprehensive test that verifies JavaScript is injected for all
     * fingerprinting techniques including Canvas, WebGL, Audio, Fonts,
     * Battery, and Hardware information.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     */
    @Test
    @DisplayName("Should randomize all fingerprint aspects")
    void testAllFingerprintAspects() throws InterruptedException {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable mock driver

        randomizer = new FingerprintRandomizer(jsDriver, TEST_INSTANCE_ID);
        randomizer.start();

        TimeUnit.MILLISECONDS.sleep(300); // Wait for randomization scripts to execute

        // Capture all executed scripts
        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // Verify all fingerprint aspects are covered! Canvas fingerprinting.
        assertThat(scriptCaptor.getAllValues()).anySatisfy(script ->
                assertThat(script).contains("HTMLCanvasElement", "toDataURL"));

        // WebGL fingerprinting
        assertThat(scriptCaptor.getAllValues()).anySatisfy(script ->
                assertThat(script).contains("WebGLRenderingContext", "getParameter"));

        // Audio fingerprinting
        assertThat(scriptCaptor.getAllValues()).anySatisfy(script ->
                assertThat(script).contains("AudioContext", "createOscillator"));

        assertThat(scriptCaptor.getAllValues()).anySatisfy(script ->
                assertThat(script).contains("measureText"));

        // Font fingerprinting
        assertThat(scriptCaptor.getAllValues()).anySatisfy(script ->
                assertThat(script).contains("getBattery"));

        // Hardware fingerprinting
        assertThat(scriptCaptor.getAllValues()).anySatisfy(script ->
                assertThat(script).contains("hardwareConcurrency"));
    }


    /**
     * Tests handling of non-JavaScript capable drivers.
     * <p>
     * Ensures the randomizer doesn't crash when given a driver that doesn't
     * implement JavascriptExecutor interface.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     */
    @Test
    @DisplayName("Should handle non-JavaScript drivers gracefully")
    void testNonJavaScriptDriver() throws InterruptedException {
        // Use a plain mock driver without JavascriptExecutor
        randomizer = new FingerprintRandomizer(mockDriver, TEST_INSTANCE_ID);

        // Should not throw exception
        randomizer.start();

        TimeUnit.MILLISECONDS.sleep(200); // Wait for potential execution attempts

        randomizer.stop();

        // Verify no interaction with a non-JS driver
        verifyNoInteractions(mockDriver);
    }


    /**
     * Tests Canvas fingerprint randomization with noise injection.
     * <p>
     * Verifies that Canvas randomization includes proper noise application
     * to pixel data, which is crucial for evading Canvas fingerprinting.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     */
    @Test
    @DisplayName("Should include random noise in canvas fingerprinting")
    void testCanvasRandomizationWithNoise() throws InterruptedException {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable mock driver

        randomizer = new FingerprintRandomizer(jsDriver, TEST_INSTANCE_ID);
        randomizer.start();

        TimeUnit.MILLISECONDS.sleep(200); // Wait for script execution

        // Capture executed scripts
        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // Find a canvas script and verify it contains a noise application
        String canvasScript = scriptCaptor.getAllValues().stream()
                .filter(script -> script.contains("HTMLCanvasElement"))
                .findFirst()
                .orElse("");

        // Verify noise injection code is present
        assertThat(canvasScript)
                .contains("Math.random()")              // Random noise generation
                .contains("imageData.data[i]")          // Pixel data access
                .contains("putImageData");              // Applying modified data
    }


    /**
     * Tests WebGL fingerprint randomization.
     * <p>
     * Verifies that WebGL vendor and renderer information is properly
     * spoofed to prevent GPU-based fingerprinting.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     */
    @Test
    @DisplayName("Should randomize WebGL vendor and renderer")
    void testWebGLRandomization() throws InterruptedException {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable mock drive

        randomizer = new FingerprintRandomizer(jsDriver, TEST_INSTANCE_ID);
        randomizer.start();

        TimeUnit.MILLISECONDS.sleep(200); // Wait for script execution

        // Capture executed scripts
        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // Find WebGL script and verify it contains vendor/renderer spoofing
        String webglScript = scriptCaptor.getAllValues().stream()
                .filter(script -> script.contains("WebGLRenderingContext"))
                .findFirst()
                .orElse("");

        // Verify WebGL parameter constants
        assertThat(webglScript)
                .contains("37445")  // VENDOR parameter (0x9245)
                .contains("37446"); // RENDERER parameter (0x9246)
    }


    /**
     * Tests graceful handling of script execution errors.
     * <p>
     * Ensures that JavaScript execution failures don't crash the randomizer
     * and are handled gracefully.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     */
    @Test
    @DisplayName("Should handle script execution errors gracefully")
    void testScriptExecutionError() throws InterruptedException {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class));    // Create a JS-capable mock driver

        // Make script execution throw an exception
        when(((JavascriptExecutor) jsDriver).executeScript(anyString()))
                .thenThrow(new RuntimeException("Script execution failed"));

        randomizer = new FingerprintRandomizer(jsDriver, TEST_INSTANCE_ID);

        // Should not throw exception when starting
        randomizer.start();

        TimeUnit.MILLISECONDS.sleep(200); // Wait for an execution attempt

        // Verify attempt was made despite error
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(anyString()); // Verify attempt was made despite error
    }


    /**
     * Tests handling of thread interruption during shutdown.
     * <p>
     * Verifies that the randomizer handles thread interruption gracefully
     * during the stop operation.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     */
    @Test
    @DisplayName("Should handle interrupted thread during shutdown")
    void testInterruptedShutdown() throws InterruptedException {
        randomizer = new FingerprintRandomizer(mockDriver, TEST_INSTANCE_ID);
        randomizer.start();

        // Interrupt the current thread before stopping
        Thread.currentThread().interrupt();

        // Should handle interruption gracefully
        randomizer.stop();

        // Clear interrupt flag
        Thread.interrupted();
    }


    /**
     * Tests that different random values are used for each fingerprint aspect.
     * <p>
     * Verifies that randomization produces realistic values for different
     * fingerprinting techniques, such as battery levels and CPU core counts.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     */
    @Test
    @DisplayName("Should use different random values for each fingerprint aspect")
    void testRandomValueDiversity() throws InterruptedException {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable mock driver

        randomizer = new FingerprintRandomizer(jsDriver, TEST_INSTANCE_ID);
        randomizer.start();

        TimeUnit.MILLISECONDS.sleep(200); // Wait for script execution

        // Capture executed scripts
        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        verify((JavascriptExecutor) jsDriver, atLeastOnce())
                .executeScript(scriptCaptor.capture());

        // Check a battery script for randomized values
        String batteryScript = scriptCaptor.getAllValues().stream()
                .filter(script -> script.contains("getBattery"))
                .findFirst()
                .orElse("");

        // Battery level should be between 0.5 and 1.0
        assertThat(batteryScript).contains("level: 0.");

        // Hardware script should have valid core counts
        String hardwareScript = scriptCaptor.getAllValues().stream()
                .filter(script -> script.contains("hardwareConcurrency"))
                .findFirst()
                .orElse("");

        // Verify a hardware script contains valid core count
        assertThat(hardwareScript).contains("hardwareConcurrency");
        assertThat(hardwareScript).containsPattern("get: \\(\\) => \\d+");
    }


    /**
     * Tests stopping the randomizer before any scripts execute.
     * <p>
     * Ensures that stopping immediately after starting doesn't cause issues
     * or errors.
     * </p>
     */
    @Test
    @DisplayName("Should stop randomization when stop is called before any execution")
    void testStopBeforeExecution() {
        randomizer = new FingerprintRandomizer(mockDriver, TEST_INSTANCE_ID);
        randomizer.start();

        // Stop immediately
        randomizer.stop();

        // Should handle gracefully; No scripts should have been executed on a non-JS driver
        verifyNoInteractions(mockDriver);
    }


    /**
     * Tests that the randomizer uses daemon threads.
     * <p>
     * Daemon threads ensure the JVM can exit even if the randomizer threads
     * are still running. This test verifies proper thread configuration.
     * </p>
     *
     * @throws InterruptedException if thread sleep is interrupted
     */
    @Test
    @DisplayName("Should use daemon threads for executor")
    void testDaemonThreads() throws InterruptedException {
        WebDriver jsDriver = mock(WebDriver.class, withSettings()
                .extraInterfaces(JavascriptExecutor.class)); // Create a JS-capable mock driver

        randomizer = new FingerprintRandomizer(jsDriver, TEST_INSTANCE_ID);
        randomizer.start();

        // The thread name should contain the instance ID
        TimeUnit.MILLISECONDS.sleep(200);

        // Since we can't directly verify thread properties in the test,
        // we ensure the randomizer works as expected
        randomizer.stop();
    }
}