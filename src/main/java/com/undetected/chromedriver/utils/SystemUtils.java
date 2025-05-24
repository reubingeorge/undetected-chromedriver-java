package com.undetected.chromedriver.utils;


/**
 * Utility class for detecting system properties and platform information.
 * <p>
 * This class provides static methods to identify the operating system and
 * architecture of the current runtime environment. It's primarily used to
 * determine the correct ChromeDriver binary to download and use for the
 * current platform.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Operating system detection (Windows, macOS, Linux)</li>
 *   <li>Architecture detection (x86_64, ARM)</li>
 *   <li>Platform string generation for ChromeDriver downloads</li>
 *   <li>Thread-safe, immutable implementation</li>
 * </ul>
 *
 * <h2>Platform Detection Strategy:</h2>
 * <p>
 * The class uses Java system properties to detect the platform:
 * <ul>
 *   <li>{@code os.name} - Operating system name</li>
 *   <li>{@code os.arch} - System architecture</li>
 * </ul>
 * Detection is performed once at class loading and cached in static fields
 * for performance.
 * </p>
 *
 * <h2>Platform Strings:</h2>
 * <p>
 * The {@link #getPlatform()} method returns platform identifiers that match
 * Chrome's naming convention for driver downloads:
 * <ul>
 *   <li>{@code "win32"} - Windows (both 32 and 64-bit)</li>
 *   <li>{@code "mac64"} - macOS Intel (x86_64)</li>
 *   <li>{@code "mac_arm64"} - macOS Apple Silicon (ARM64)</li>
 *   <li>{@code "linux64"} - Linux 64-bit</li>
 * </ul>
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Check operating system
 * if (SystemUtils.isWindows()) {
 *     System.out.println("Running on Windows");
 * }
 *
 * // Get platform string for driver download
 * String platform = SystemUtils.getPlatform();
 * String driverUrl = String.format("https://chromedriver.storage.googleapis.com/%s/chromedriver_%s.zip",
 *                                  version, platform);
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * All methods in this class are thread-safe. System properties are read once
 * in class initialization and stored in immutable static fields.
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see System#getProperty(String)
 */
public class SystemUtils {

    /**
     * The operating system name in lowercase.
     * <p>
     * Cached value of {@code System.getProperty("os.name").toLowerCase()} to avoid
     * repeated system property lookups. The value is converted to lowercase for
     * case-insensitive comparisons.
     * </p>
     *
     * <h3>Common Values:</h3>
     * <ul>
     *   <li>Windows: "windows 10", "windows 11", "windows server 2019"</li>
     *   <li>macOS: "mac os x"</li>
     *   <li>Linux: "linux", "ubuntu", "fedora"</li>
     * </ul>
     */
    private static final String OS = System.getProperty("os.name").toLowerCase();


    /**
     * The system architecture in the lowercase.
     * <p>
     * Cached value of {@code System.getProperty("os.arch").toLowerCase()} to avoid
     * repeated system property lookups. Used to distinguish between different
     * processor architectures.
     * </p>
     *
     * <h3>Common Values:</h3>
     * <ul>
     *   <li>Intel/AMD 64-bit: "amd64", "x86_64"</li>
     *   <li>Intel 32-bit: "x86", "i386"</li>
     *   <li>ARM 64-bit: "aarch64", "arm64"</li>
     *   <li>ARM 32-bit: "arm"</li>
     * </ul>
     */
    private static final String ARCH = System.getProperty("os.arch").toLowerCase();


    /**
     * Determines if the current operating system is Windows.
     * <p>
     * This method checks if the OS name contains "win", which matches all
     * Windows variants including:
     * <ul>
     *   <li>Windows 10</li>
     *   <li>Windows 11</li>
     *   <li>Windows Server editions</li>
     * </ul>
     * </p>
     *
     * @return {@code true} if running on Windows, {@code false} otherwise
     */
    public static boolean isWindows() {
        return OS.contains("win");
    }


    /**
     * Determines if the current operating system is macOS.
     * <p>
     * This method checks if the OS name contains "mac", which matches:
     * <ul>
     *   <li>Mac OS X</li>
     *   <li>macOS (all versions)</li>
     * </ul>
     * </p>
     *
     * @return {@code true} if running on macOS, {@code false} otherwise
     */
    public static boolean isMac() {
        return OS.contains("mac");
    }


    /**
     * Determines if the current operating system is Linux.
     * <p>
     * This method checks if the OS name contains "nux", which matches:
     * <ul>
     *   <li>Linux</li>
     *   <li>GNU/Linux</li>
     *   <li>Various Linux distributions</li>
     * </ul>
     * </p>
     *
     * <h3>Note:</h3>
     * <p>
     * The check for "nux" instead of "linux" ensures compatibility with
     * systems that might report "GNU/Linux" or similar variants.
     * </p>
     *
     * @return {@code true} if running on Linux, {@code false} otherwise
     */
    public static boolean isLinux() {
        return OS.contains("nux");
    }


    /**
     * Determines if the current system architecture is ARM-based.
     * <p>
     * This method checks for both 32-bit and 64-bit ARM architectures,
     * including:
     * <ul>
     *   <li>arm - 32-bit ARM</li>
     *   <li>aarch64 - 64-bit ARM (ARMv8)</li>
     *   <li>arm64 - Alternative name for 64-bit ARM</li>
     * </ul>
     * </p>
     *
     * <h3>Use Case:</h3>
     * <p>
     * Primarily used to distinguish between Intel and Apple Silicon Macs,
     * as well as ARM-based Linux systems like Raspberry Pi.
     * </p>
     *
     * @return {@code true} if running on ARM architecture, {@code false} otherwise
     */
    public static boolean isArm() {
        return ARCH.contains("arm") || ARCH.contains("aarch64");
    }


    /**
     * Returns the platform identifier string for ChromeDriver downloads.
     * <p>
     * This method returns a platform string that matches Chrome's naming
     * convention for driver downloads. The returned value can be used to
     * construct download URLs for the appropriate ChromeDriver binary.
     * </p>
     *
     * <h3>Platform Mapping:</h3>
     * <ul>
     *   <li>Windows (all versions) → "win32"</li>
     *   <li>macOS Intel → "mac64"</li>
     *   <li>macOS Apple Silicon → "mac_arm64"</li>
     *   <li>Linux (all distributions) → "linux64"</li>
     * </ul>
     *
     * <h3>Notes:</h3>
     * <ul>
     *   <li>Windows always returns "win32" even on 64-bit systems for
     *       historical compatibility</li>
     *   <li>32-bit Linux is no longer supported by Chrome</li>
     *   <li>The method assumes 64-bit Linux as 32-bit is deprecated</li>
     * </ul>
     *
     * @return platform identifier string suitable for ChromeDriver downloads
     * @see #isWindows()
     * @see #isMac()
     * @see #isLinux()
     * @see #isArm()
     */
    public static String getPlatform() {
        if (isWindows()) {
            return "win32";
        } else if (isMac()) {
            return isArm() ? "mac_arm64" : "mac64";
        } else {
            return "linux64";
        }
    }
}