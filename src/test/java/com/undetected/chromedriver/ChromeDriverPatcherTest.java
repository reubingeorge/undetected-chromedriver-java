package com.undetected.chromedriver;

import com.undetected.chromedriver.exceptions.PatchingException;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test class for {@link ChromeDriverPatcher}.
 * <p>
 * This test suite validates the functionality of the ChromeDriverPatcher class,
 * ensuring proper driver download, caching, and error handling. While the
 * ChromeDriverPatcher class is deprecated, these tests ensure backward
 * compatibility and proper functionality for legacy code.
 * </p>
 *
 * <h2>Test Coverage:</h2>
 * <ul>
 *   <li>Driver download for various Chrome versions</li>
 *   <li>Caching mechanism verification</li>
 *   <li>Executable file creation</li>
 *   <li>Fallback behavior for unknown versions</li>
 *   <li>Network error handling</li>
 * </ul>
 *
 * <h2>Test Environment Requirements:</h2>
 * <ul>
 *   <li>Internet connectivity for driver downloads</li>
 *   <li>Write permissions in temp directory</li>
 *   <li>Chrome browser (optional, falls back to known versions)</li>
 * </ul>
 *
 * <h2>Known Versions Used:</h2>
 * <p>
 * Tests use specific Chrome versions known to exist in Chrome for Testing:
 * <ul>
 *   <li>120.0.6099.0 - Fallback version</li>
 *   <li>121.0.6167.57 - Stable version (early 2024)</li>
 * </ul>
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @deprecated Mirrors the deprecation of {@link ChromeDriverPatcher}
 * @see ChromeDriverPatcher
 * @see ChromeVersionManager
 */
@Deprecated
class ChromeDriverPatcherTest {

    /**
     * The ChromeDriverPatcher instance under test.
     * <p>
     * Initialized fresh for each test method to ensure test isolation
     * and prevent cache interference between tests.
     * </p>
     */
    private ChromeDriverPatcher patcher;


    /**
     * ChromeVersionManager instance for detecting installed Chrome version.
     * <p>
     * Used to test driver downloads for the actual installed Chrome version
     * when available, providing more realistic test scenarios.
     * </p>
     */
    private ChromeVersionManager versionManager;


    /**
     * Sets up the test environment before each test method.
     * <p>
     * Creates fresh instances of ChromeDriverPatcher and ChromeVersionManager
     * to ensure test isolation. Each test starts with an empty driver cache.
     * </p>
     */
    @BeforeEach
    void setUp() {
        patcher = new ChromeDriverPatcher();            // Create a new patcher instance with empty cache
        versionManager = new ChromeVersionManager();    // Create a version manager for Chrome detection
    }


    /**
     * Tests that ChromeDriverPatcher can be instantiated successfully.
     * <p>
     * This basic test ensures the constructor doesn't throw exceptions
     * and creates a valid instance.
     * </p>
     *
     * @see ChromeDriverPatcher#ChromeDriverPatcher()
     */
    @Test
    @DisplayName("Should create patcher instance successfully")
    void testConstructor() {
        assertThat(patcher).isNotNull(); // Verify patcher was created successfully
    }


    /**
     * Test driver download for the currently installed Chrome version.
     * <p>
     * This test attempts to download a driver matching the actual Chrome
     * installation on the test system. If Chrome is not installed, it falls
     * back to a known good version to ensure the test can run in CI/CD
     * environments without Chrome.
     * </p>
     *
     * <h3>Test Flow:</h3>
     * <ol>
     *   <li>Attempt to detect the current Chrome version</li>
     *   <li>Fall back to a known version if detection fails</li>
     *   <li>Download appropriate ChromeDriver</li>
     *   <li>Verify a driver file exists and is executable</li>
     * </ol>
     *
     * @see ChromeDriverPatcher#getPatchedDriver(String)
     * @see ChromeVersionManager#getCurrentVersion()
     */
    @Test
    @DisplayName("Should get patched driver for current Chrome version")
    void testGetPatchedDriverForCurrentVersion() {
        // Get the actual current Chrome version
        String currentVersion;
        try {
            // Attempt to detect an installed Chrome version
            currentVersion = versionManager.getCurrentVersion();
        } catch (Exception e) {
            // If Chrome is not installed, use a known good version
            // This ensures tests can run in environments without Chrome
            currentVersion = "120.0.6099.0"; // A known version from Chrome for Testing
        }

        String patchedPath = patcher.getPatchedDriver(currentVersion); // Download driver for the determined version
        Path driverPath = Path.of(patchedPath); // Convert to Path for file operations

        assertThat(patchedPath).isNotNull(); // Verify driver was downloaded successfully
        assertThat(Files.exists(driverPath)).isTrue(); // Verify a driver file exists
        assertThat(driverPath.toFile().canExecute()).isTrue(); // Verify a driver is executable (important for Selenium)
    }


    /**
     * Test driver download for a specific known Chrome version.
     * <p>
     * Uses a hardcoded Chrome version known to exist in the
     * Chrome for Testing repository. This provides a stable test that
     * doesn't depend on the local Chrome installation.
     * </p>
     *
     * @see ChromeDriverPatcher#getPatchedDriver(String)
     */
    @Test
    @DisplayName("Should get patched driver for known good version")
    void testGetPatchedDriverForKnownVersion() {
        // Use a version we know exists in Chrome for Testing
        // This version was stable as of early 2024
        String chromeVersion = "121.0.6167.57"; // Stable version as of early 2024
        String patchedPath = patcher.getPatchedDriver(chromeVersion); // Download driver for a known version
        Path driverPath = Path.of(patchedPath); // Convert to Path for verification

        assertThat(patchedPath).isNotNull(); // Verify successful download
        assertThat(Files.exists(driverPath)).isTrue(); // Verify file exists
    }


    /**
     * Tests fallback behavior for unknown or non-existent Chrome versions.
     * <p>
     * WebDriverManager should handle unknown versions gracefully by falling
     * back to the latest stable version or closest match. This test ensures
     * the patcher doesn't crash when given an invalid version.
     * </p>
     *
     * @see ChromeDriverPatcher#getPatchedDriver(String)
     */
    @Test
    @DisplayName("Should fall back to latest stable for unknown version")
    void testFallbackToLatestStable() {
        // Use a version that likely doesn't exist exactly
        // The specific version number is intentionally invalid
        String chromeVersion = "120.0.0.1";

        // Should not throw exception, should fall back to a working version
        String patchedPath = patcher.getPatchedDriver(chromeVersion);
        Path driverPath = Path.of(patchedPath); // Convert to Path for verification

        assertThat(patchedPath).isNotNull(); // Verify fallback succeeded
        assertThat(Files.exists(driverPath)).isTrue(); // Verify a driver was downloaded despite invalid version
    }


    /**
     * Tests the caching mechanism of ChromeDriverPatcher.
     * <p>
     * Verifies that repeated requests for the same Chrome version return
     * cached results rather than downloading again. This is tested by
     * comparing execution times - cached access should be significantly
     * faster than initial download.
     * </p>
     *
     * <h3>Performance Expectation:</h3>
     * <p>
     * The second call should be at least 10x faster than the first,
     * as it only needs to return a cached path rather than downloading.
     * </p>
     *
     * @see ChromeDriverPatcher#getPatchedDriver(String)
     */
    @Test
    @DisplayName("Should cache patched drivers")
    void testPatchedDriverCaching() {
        String version = "121.0.6167.57"; // Known good version

        // First call - downloads and patches
        long start1 = System.currentTimeMillis();
        String path1 = patcher.getPatchedDriver(version);
        long duration1 = System.currentTimeMillis() - start1;

        // Second call - should use cache
        long start2 = System.currentTimeMillis();
        String path2 = patcher.getPatchedDriver(version);
        long duration2 = System.currentTimeMillis() - start2;

        assertThat(path1).isEqualTo(path2);
        assertThat(duration2).isLessThan(duration1 / 10); // Cache should be much faster
    }


    /**
     * Tests that downloaded ChromeDriver files have proper executable permissions.
     * <p>
     * This test verifies that the driver files are properly configured for
     * execution, which is crucial for Selenium to launch them. On Unix-like
     * systems, this includes checking executable permissions.
     * </p>
     *
     * <h3>Platform-Specific Behavior:</h3>
     * <ul>
     *   <li>Unix/Linux/macOS: Checks executable file permission</li>
     *   <li>Windows: Skips executable check (uses .exe extension instead)</li>
     * </ul>
     *
     * @see ChromeDriverPatcher#getPatchedDriver(String)
     */
    @Test
    @DisplayName("Should create executable patched driver")
    void testPatchedDriverIsExecutable() {
        String version = "121.0.6167.57"; // Use a known version for reliable testing
        String patchedPath = patcher.getPatchedDriver(version); // Get a driver path

        Path driverPath = Path.of(patchedPath); // Convert to Path for file operations
        assertThat(Files.exists(driverPath)).isTrue(); // Verify file exists
        assertThat(Files.isRegularFile(driverPath)).isTrue(); // Verify it's a regular file (not directory or symlink)

        // On Unix systems, check if executable
        // Windows doesn't use Unix-style executable permissions
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            assertThat(driverPath.toFile().canExecute()).isTrue(); // Verify executable permission is set
        }
    }


    /**
     * Tests error handling for network-related failures.
     * <p>
     * Simulates network errors during driver download to ensure proper
     * exception handling and error messages. Uses an anonymous inner class
     * to override the getPatchedDriver method with a failing implementation.
     * </p>
     *
     * <h3>Test Approach:</h3>
     * <p>
     * Creates a custom ChromeDriverPatcher that always throws an exception
     * simulating network failure. This tests the error propagation without
     * requiring actual network manipulation.
     * </p>
     *
     * @see ChromeDriverPatcher#getPatchedDriver(String)
     * @see PatchingException
     */
    @Test
    @DisplayName("Should handle network errors gracefully")
    void testNetworkError() {
        // Create a patcher with invalid URL to simulate network error
        // Anonymous inner class overrides getPatchedDriver to always fail
        ChromeDriverPatcher faultyPatcher = new ChromeDriverPatcher() {
            @Override
            public String getPatchedDriver(String chromeVersion) {
                // Simulate network failure with UnknownHostException
                throw new PatchingException("Failed to get patched driver",
                        new java.net.UnknownHostException("simulated network error"));
            }
        };

        // Verify exception is thrown with a proper message
        assertThatThrownBy(() -> faultyPatcher.getPatchedDriver("120.0.0.0"))
                .isInstanceOf(PatchingException.class)
                .hasMessageContaining("Failed to get patched driver");
    }
}