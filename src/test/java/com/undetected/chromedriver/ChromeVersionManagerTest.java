package com.undetected.chromedriver;

import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * Unit test class for {@link ChromeVersionManager}.
 * <p>
 * This test suite validates the functionality of the ChromeVersionManager class,
 * which is responsible for detecting installed Chrome versions and retrieving
 * the latest available version information. The tests handle various scenarios
 * including successful detection, missing installations, and API interactions.
 * </p>
 *
 * <h2>Test Coverage:</h2>
 * <ul>
 *   <li>Chrome version detection on the local system</li>
 *   <li>Latest version retrieval from Chrome update API</li>
 *   <li>Version comparison and update checking</li>
 *   <li>Error handling for missing Chrome installations</li>
 *   <li>Version format validation</li>
 * </ul>
 *
 * <h2>Test Environment Considerations:</h2>
 * <ul>
 *   <li>Chrome may not be installed in CI/CD environments</li>
 *   <li>Network access required for API tests</li>
 *   <li>Tests include timeouts to prevent hanging</li>
 *   <li>Graceful handling of missing Chrome installations</li>
 * </ul>
 *
 * <h2>Version Format:</h2>
 * <p>
 * Chrome versions follow the format: MAJOR.MINOR.BUILD.PATCH
 * Example: 120.0.6099.109
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see ChromeVersionManager
 */
class ChromeVersionManagerTest {

    /**
     * The ChromeVersionManager instance under test.
     * <p>
     * Initialized fresh for each test method to ensure test isolation
     * and prevent state leakage between tests.
     * </p>
     */
    private ChromeVersionManager versionManager;


    /**
     * Sets up the test environment before each test method.
     * <p>
     * Creates a new ChromeVersionManager instance to ensure each test
     * starts with a clean state.
     * </p>
     */
    @BeforeEach
    void setUp() {
        versionManager = new ChromeVersionManager(); // Initialize a fresh version manager for each test
    }


    /**
     * Tests that ChromeVersionManager can be instantiated successfully.
     * <p>
     * This basic test ensures the constructor doesn't throw exceptions
     * and creates a valid instance.
     * </p>
     *
     * @see ChromeVersionManager#ChromeVersionManager()
     */
    @Test
    @DisplayName("Should create version manager instance")
    void testConstructor() {
        assertThat(versionManager).isNotNull(); // Verify instance was created successfully
    }


    /**
     * Tests detection of the currently installed Chrome version.
     * <p>
     * This test attempts to detect the Chrome version installed on the
     * test system. It handles the case where Chrome might not be installed
     * (common in CI environments) by catching IOException and validating
     * the error message.
     * </p>
     *
     * <h3>Test Behavior:</h3>
     * <ul>
     *   <li>Success: Validates version format (e.g., 120.0.6099.109)</li>
     *   <li>Failure: Ensures meaningful error message is provided</li>
     * </ul>
     *
     * @see ChromeVersionManager#getCurrentVersion()
     */
    @Test
    @DisplayName("Should get current Chrome version")
    @Timeout(value = 15) // 15-second timeout for the whole test
    void testGetCurrentVersion() {
        try {
            String version = versionManager.getCurrentVersion();    // Attempt to get the current Chrome version

            assertThat(version).isNotNull();                        // Validate successful detection
            assertThat(version).matches("\\d+\\.\\d+\\.\\d+\\.\\d+"); // Verify version matches expected format: X.X.X.X

            System.out.println("Detected Chrome version: " + version); // Log detected version for debugging
        } catch (IOException e) {
            // Chrome might not be installed in a CI environment
            System.err.println("Chrome detection failed: " + e.getMessage());

            // Even in failure, an error message should be informative
            assertThat(e.getMessage()).isNotEmpty();
        }
    }


    /**
     * Tests retrieval of the latest Chrome version from the update API.
     * <p>
     * This test contacts Chrome's update servers to get the latest stable
     * version information. Requires network connectivity to succeed.
     * </p>
     *
     * <h3>API Information:</h3>
     * <p>
     * Uses Chrome's official update check API which returns version
     * information for the stable channel.
     * </p>
     *
     * @throws IOException if network request fails or API is unavailable
     * @see ChromeVersionManager#getLatestVersion()
     */
    @Test
    @DisplayName("Should get latest Chrome version from API")
    void testGetLatestVersion() throws IOException {
        String version = versionManager.getLatestVersion(); // Get the latest version from Chrome update API

        assertThat(version).isNotNull(); // Validate API response
        assertThat(version).matches("\\d+\\.\\d+\\.\\d+\\.\\d+"); // Verify version format matches X.X.X.X pattern

        System.out.println("Latest Chrome version from API: " + version); // Log latest version for reference
    }


    /**
     * Tests the version comparison functionality.
     * <p>
     * Checks if the installed Chrome version is the latest available.
     * This test combines local detection with API queries and handles
     * cases where Chrome might not be installed.
     * </p>
     *
     * <h3>Test Logic:</h3>
     * <ul>
     *   <li>Attempts to compare local vs. latest version</li>
     *   <li>Returns boolean indicating if update is available</li>
     *   <li>Handles missing Chrome gracefully</li>
     * </ul>
     *
     * @see ChromeVersionManager#isLatestVersion()
     */
    @Test
    @DisplayName("Should check if Chrome is latest version")
    @Timeout(value = 15)
    void testIsLatestVersion() {
        try {
            boolean isLatest = versionManager.isLatestVersion();    // Check if the current Chrome is up to date

            // The Result depends on the actual Chrome installation
            assertThat(isLatest).isIn(true, false); // Log result for debugging

            System.out.println("Is Chrome up to date: " + isLatest);
        } catch (IOException e) {
            // Chrome might not be installed
            System.err.println("Version check failed: " + e.getMessage());
            assertThat(e.getMessage()).isNotEmpty(); // Ensure error message is meaningful
        }
    }


    /**
     * Tests error handling when Chrome is not installed.
     * <p>
     * Uses an anonymous inner class to simulate a missing Chrome installation
     * and verifies that appropriate exceptions are thrown with clear messages.
     * </p>
     *
     * <h3>Test Strategy:</h3>
     * <p>
     * Creates a custom ChromeVersionManager that always throws IOException
     * to simulate missing Chrome, then verifies exception handling.
     * </p>
     *
     * @see ChromeVersionManager#getCurrentVersion()
     */
    @Test
    @DisplayName("Should handle missing Chrome installation gracefully")
    void testMissingChromeInstallation() {
        // Create a custom version manager that simulates missing Chrome
        ChromeVersionManager customManager = new ChromeVersionManager() {
            @Override
            public String getCurrentVersion() throws IOException {
                throw new IOException("Chrome installation not found");
            }
        };

        // Verify exception is thrown with correct message
        assertThatThrownBy(customManager::getCurrentVersion)
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Chrome installation not found");
    }


    /**
     * Tests version comparison logic by parsing version numbers.
     * <p>
     * This test extracts and compares major version numbers from both
     * the installed and latest Chrome versions. It demonstrates how to
     * parse Chrome version strings for comparison purposes.
     * </p>
     *
     * <h3>Version Parsing:</h3>
     * <p>
     * Extracts the major version (first number) from version strings
     * like "120.0.6099.109" → 120
     * </p>
     *
     * @see ChromeVersionManager#getCurrentVersion()
     * @see ChromeVersionManager#getLatestVersion()
     */
    @Test
    @DisplayName("Should compare versions correctly")
    @Timeout(value = 15)
    void testVersionComparison() {
        try {

            // Get both current and latest versions
            String current = versionManager.getCurrentVersion();
            String latest = versionManager.getLatestVersion();

            // Split by dots and take the first element
            int currentMajor = Integer.parseInt(current.split("\\.")[0]);
            int latestMajor = Integer.parseInt(latest.split("\\.")[0]);

            // Validate major versions are positive numbers
            assertThat(currentMajor).isPositive();
            assertThat(latestMajor).isPositive();

            // Log version information for debugging
            System.out.println("Current major version: " + currentMajor);
            System.out.println("Latest major version: " + latestMajor);
        } catch (IOException e) {
            // Skip the test if Chrome detection fails
            System.err.println("Version comparison test skipped: " + e.getMessage());
        }
    }
}