package com.undetected.chromedriver.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit test class for {@link SystemUtils}.
 * <p>
 * This test class validates the system detection capabilities of the SystemUtils
 * utility class, ensuring accurate identification of operating systems, architectures,
 * and platform strings across different environments.
 * </p>
 *
 * <h2>Test Coverage:</h2>
 * <ul>
 *   <li>Operating system detection (Windows, macOS, Linux)</li>
 *   <li>Architecture detection (ARM vs x86/x64)</li>
 *   <li>Platform string generation</li>
 *   <li>OS-specific conditional tests</li>
 * </ul>
 *
 * <h2>Conditional Testing:</h2>
 * <p>
 * Several tests use JUnit 5's {@link EnabledOnOs} annotation to run only on
 * specific operating systems, ensuring accurate validation of OS-specific behavior.
 * </p>
 *
 * <h2>Testing Framework:</h2>
 * <p>
 * Uses JUnit 5 (Jupiter) for test execution with conditional test support
 * and AssertJ for fluent assertions.
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see SystemUtils
 */
class SystemUtilsTest {
    /**
     * Tests that at least one operating system is detected.
     * <p>
     * This test serves as a sanity check to ensure that the system detection
     * logic identifies the current operating system as one of the supported types.
     * It verifies that exactly one of Windows, macOS, or Linux is detected.
     * </p>
     * <p>
     * This test runs on all platforms and ensures the detection methods don't
     * all return false, which would indicate a detection failure.
     * </p>
     *
     * @see SystemUtils#isWindows()
     * @see SystemUtils#isMac()
     * @see SystemUtils#isLinux()
     */
    @Test
    @DisplayName("Should detect OS correctly")
    void testOsDetection() {
        // At least one should be true
        // Combine all OS detection results with logical OR
        boolean isAnyOs = SystemUtils.isWindows() ||
                SystemUtils.isMac() ||
                SystemUtils.isLinux();

        assertThat(isAnyOs).isTrue();       // Verify that at least one OS was detected
    }


    /**
     * Tests Windows detection when running on a Windows system.
     * <p>
     * This test is conditionally executed only on Windows platforms using
     * the {@link EnabledOnOs} annotation. It verifies that:
     * <ul>
     *   <li>{@link SystemUtils#isWindows()} returns {@code true}</li>
     *   <li>{@link SystemUtils#isMac()} returns {@code false}</li>
     *   <li>{@link SystemUtils#isLinux()} returns {@code false}</li>
     * </ul>
     * </p>
     * <p>
     * This ensures the OS detection is mutually exclusive and accurate
     * when running on Windows.
     * </p>
     *
     * @see SystemUtils#isWindows()
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    @DisplayName("Should detect Windows correctly")
    void testWindowsDetection() {
        assertThat(SystemUtils.isWindows()).isTrue();   // Verify Windows is detected
        assertThat(SystemUtils.isMac()).isFalse();      // Verify other OS types are not detected
        assertThat(SystemUtils.isLinux()).isFalse();    // Verify other OS types are not detected
    }


    /**
     * Tests macOS detection when running on a macOS system.
     * <p>
     * This test is conditionally executed only on macOS platforms using
     * the {@link EnabledOnOs} annotation. It verifies that:
     * <ul>
     *   <li>{@link SystemUtils#isWindows()} returns {@code false}</li>
     *   <li>{@link SystemUtils#isMac()} returns {@code true}</li>
     *   <li>{@link SystemUtils#isLinux()} returns {@code false}</li>
     * </ul>
     * </p>
     * <p>
     * This ensures the OS detection is mutually exclusive and accurate
     * when running on macOS.
     * </p>
     *
     * @see SystemUtils#isMac()
     */
    @Test
    @EnabledOnOs(OS.MAC)
    @DisplayName("Should detect macOS correctly")
    void testMacDetection() {
        assertThat(SystemUtils.isWindows()).isFalse();  // Verify other OS types are not detected
        assertThat(SystemUtils.isMac()).isTrue();       // Verify macOS is detected
        assertThat(SystemUtils.isLinux()).isFalse();    // Verify Linux is not detected
    }


    /**
     * Tests Linux detection when running on a Linux system.
     * <p>
     * This test is conditionally executed only on Linux platforms using
     * the {@link EnabledOnOs} annotation. It verifies that:
     * <ul>
     *   <li>{@link SystemUtils#isWindows()} returns {@code false}</li>
     *   <li>{@link SystemUtils#isMac()} returns {@code false}</li>
     *   <li>{@link SystemUtils#isLinux()} returns {@code true}</li>
     * </ul>
     * </p>
     * <p>
     * This ensures the OS detection is mutually exclusive and accurate
     * when running on Linux.
     * </p>
     *
     * @see SystemUtils#isLinux()
     */
    @Test
    @EnabledOnOs(OS.LINUX)
    @DisplayName("Should detect Linux correctly")
    void testLinuxDetection() {
        assertThat(SystemUtils.isWindows()).isFalse();  // Verify other OS types are not detected
        assertThat(SystemUtils.isMac()).isFalse();      // Verify other OS types are not detected
        assertThat(SystemUtils.isLinux()).isTrue();     // Verify Linux is detected
    }


    /**
     * Tests that architecture detection returns a valid boolean value.
     * <p>
     * This test verifies that {@link SystemUtils#isArm()} executes without
     * throwing exceptions and returns a valid boolean value. The test doesn't
     * assert a specific value since the result depends on the actual hardware
     * architecture where the test runs.
     * </p>
     * <p>
     * The assertion using {@code isIn(true, false)} serves as a safety check
     * to ensure the method returns a proper boolean rather than null or
     * throwing an exception.
     * </p>
     *
     * @see SystemUtils#isArm()
     */
    @Test
    @DisplayName("Should detect architecture")
    void testArchDetection() {
        // Should return true or false, not throw
        // Call the ARM detection method
        boolean isArm = SystemUtils.isArm();

        // Verify it returns a valid boolean (redundant but explicit)
        // This ensures the method doesn't throw an exception
        assertThat(isArm).isIn(true, false);
    }


    /**
     * Tests that a valid platform string is returned.
     * <p>
     * This test verifies that {@link SystemUtils#getPlatform()} returns
     * one of the expected platform identifiers used by Chrome/Chromium:
     * <ul>
     *   <li>{@code "win32"} - Windows (both 32 and 64-bit)</li>
     *   <li>{@code "mac64"} - macOS x64 (Intel)</li>
     *   <li>{@code "mac_arm64"} - macOS ARM64 (Apple Silicon)</li>
     *   <li>{@code "linux64"} - Linux 64-bit</li>
     * </ul>
     * </p>
     * <p>
     * These platform strings match the naming convention used by Chrome
     * for downloading platform-specific binaries.
     * </p>
     *
     * @see SystemUtils#getPlatform()
     */
    @Test
    @DisplayName("Should return valid platform string")
    void testGetPlatform() {
        String platform = SystemUtils.getPlatform();    // Get the platform string

        assertThat(platform).isNotNull();               // Verify the platform string is not null

        // Verify it's one of the expected values
        // These match Chrome's platform naming convention
        assertThat(platform).isIn(
                "win32", "mac64", "mac_arm64", "linux64"
        );
    }


    /**
     * Tests that Windows systems return the correct platform string.
     * <p>
     * This test is conditionally executed only on Windows platforms.
     * It verifies that {@link SystemUtils#getPlatform()} returns
     * {@code "win32"} when running on Windows, regardless of whether
     * the system is 32-bit or 64-bit.
     * </p>
     * <p>
     * Note: Chrome uses "win32" for all Windows platforms, including
     * 64-bit Windows systems, for historical compatibility reasons.
     * </p>
     *
     * @see SystemUtils#getPlatform()
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    @DisplayName("Should return Windows platform")
    void testWindowsPlatform() {
        // Verify Windows returns "win32" (used for both 32 and 64-bit)
        assertThat(SystemUtils.getPlatform()).isEqualTo("win32");
    }


    /**
     * Tests that Linux systems return the correct platform string.
     * <p>
     * This test is conditionally executed only on Linux platforms.
     * It verifies that {@link SystemUtils#getPlatform()} returns
     * {@code "linux64"} when running on Linux.
     * </p>
     * <p>
     * Note: This test assumes 64-bit Linux, as 32-bit Linux support
     * has been deprecated in modern Chrome versions.
     * </p>
     *
     * @see SystemUtils#getPlatform()
     */
    @Test
    @EnabledOnOs(OS.LINUX)
    @DisplayName("Should return Linux platform")
    void testLinuxPlatform() {
        assertThat(SystemUtils.getPlatform()).isEqualTo("linux64"); // Verify Linux returns "linux64"
    }
}