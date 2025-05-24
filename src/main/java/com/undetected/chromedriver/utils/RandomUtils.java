package com.undetected.chromedriver.utils;

import java.util.concurrent.ThreadLocalRandom;


/**
 * Utility class for generating randomized browser-related data to enhance undetectability.
 * <p>
 * This class provides static methods to generate random user agents and language codes
 * that mimic real browser behavior patterns. The randomization helps avoid detection by
 * anti-automation systems that look for static or predictable browser fingerprints.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Random user agent generation for Windows, macOS, and Linux</li>
 *   <li>Random Chrome version generation (110-119)</li>
 *   <li>Random language code selection from common locales</li>
 *   <li>Thread-safe implementation using {@link ThreadLocalRandom}</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Generate a random user agent string
 * String userAgent = RandomUtils.getRandomUserAgent();
 * // Example output: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.5790.0 Safari/537.36"
 *
 * // Generate a random language code
 * String language = RandomUtils.getRandomLanguage();
 * // Example output: "en-US"
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * All methods in this class are thread-safe and can be called concurrently from
 * multiple threads without external synchronization. This is achieved through the
 * use of {@link ThreadLocalRandom} which provides better performance than
 * {@link java.util.Random} in concurrent environments.
 * </p>
 *
 * <h2>Anti-Detection Strategy:</h2>
 * <p>
 * The randomization provided by this class helps evade detection by:
 * <ul>
 *   <li>Varying user agent strings across sessions</li>
 *   <li>Using realistic Chrome version numbers</li>
 *   <li>Rotating through common language settings</li>
 *   <li>Avoiding patterns that could be flagged as automated</li>
 * </ul>
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see ThreadLocalRandom
 */
public class RandomUtils {

    /**
     * Array of user agent string templates for different operating systems.
     * <p>
     * Each template contains a placeholder (%s) for the Chrome version number.
     * The templates represent the most common desktop operating systems:
     * <ul>
     *   <li>Index 0: Windows 10 (64-bit)</li>
     *   <li>Index 1: macOS (Intel, Catalina)</li>
     *   <li>Index 2: Linux (64-bit)</li>
     * </ul>
     * </p>
     *
     * <h3>Template Structure:</h3>
     * <p>
     * All templates follow the standard Chrome user agent format:
     * {@code Mozilla/5.0 (Platform) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/VERSION Safari/537.36}
     * </p>
     *
     * <h3>Platform Details:</h3>
     * <ul>
     *   <li><b>Windows:</b> NT 10.0; Win64; x64 - Represents Windows 10/11 64-bit</li>
     *   <li><b>macOS:</b> Intel Mac OS X 10_15_7 - Represents macOS Catalina on Intel</li>
     *   <li><b>Linux:</b> X11; Linux x86_64 - Represents 64-bit Linux with X Window System</li>
     * </ul>
     */
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Safari/537.36"
    };


    /**
     * Array of user agent string templates for different operating systems.
     * <p>
     * Each template contains a placeholder (%s) for the Chrome version number.
     * The templates represent the most common desktop operating systems:
     * <ul>
     *   <li>Index 0: Windows 10 (64-bit)</li>
     *   <li>Index 1: macOS (Intel, Catalina)</li>
     *   <li>Index 2: Linux (64-bit)</li>
     * </ul>
     * </p>
     *
     * <h3>Template Structure:</h3>
     * <p>
     * All templates follow the standard Chrome user agent format:
     * {@code Mozilla/5.0 (Platform) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/VERSION Safari/537.36}
     * </p>
     *
     * <h3>Platform Details:</h3>
     * <ul>
     *   <li><b>Windows:</b> NT 10.0; Win64; x64 - Represents Windows 10/11 64-bit</li>
     *   <li><b>macOS:</b> Intel Mac OS X 10_15_7 - Represents macOS Catalina on Intel</li>
     *   <li><b>Linux:</b> X11; Linux x86_64 - Represents 64-bit Linux with X Window System</li>
     * </ul>
     */
    private static final String[] LANGUAGES = {
            "en-US", "en-GB", "es-ES", "fr-FR", "de-DE",
            "it-IT", "pt-BR", "ja-JP", "ko-KR", "zh-CN"
    };


    /**
     * Generates a random Chrome user agent string.
     * <p>
     * This method creates a realistic user agent string by:
     * <ol>
     *   <li>Randomly selecting an operating system template</li>
     *   <li>Generating a random Chrome version between 110 and 119</li>
     *   <li>Creating a random build number (0-9999)</li>
     *   <li>Formatting the complete user agent string</li>
     * </ol>
     * </p>
     *
     * <h3>Version Generation:</h3>
     * <p>
     * Chrome versions follow the pattern: MAJOR.0.BUILD.0
     * <ul>
     *   <li>Major version: 110-119 (covers recent Chrome releases)</li>
     *   <li>Minor version: Always 0 (Chrome convention)</li>
     *   <li>Build number: 0-9999 (random 4-digit number)</li>
     *   <li>Patch version: Always 0 (Chrome convention)</li>
     * </ul>
     * </p>
     *
     * <h3>Example Output:</h3>
     * <pre>
     * "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.5790.0 Safari/537.36"
     * "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.3421.0 Safari/537.36"
     * </pre>
     *
     * @return a randomly generated Chrome user agent string that mimics real browser patterns
     * @see ThreadLocalRandom#nextInt(int)
     * @see ThreadLocalRandom#nextInt(int, int)
     * @see String#format(String, Object...)
     */
    public static String getRandomUserAgent() {

        // Select a random user agent template from the available options
        // ThreadLocalRandom provides better performance in multithreaded environments
        String template = USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)];

        // Generate random Chrome major version between 110 and 119 (exclusive upper bound)
        // These versions represent recent Chrome releases as of 2023-2024
        int majorVersion = ThreadLocalRandom.current().nextInt(110, 120);

        // Generate random build number between 0 and 9999
        // This creates realistic-looking build numbers like 5790, 3421, etc.
        int minorVersion = ThreadLocalRandom.current().nextInt(0, 9999);

        // Format the complete user agent string
        // Inserts version in Chrome's standard format: MAJOR.0.BUILD.0
        return String.format(template, majorVersion + ".0." + minorVersion + ".0");
    }


    /**
     * Returns a randomly selected browser language code.
     * <p>
     * This method randomly selects one of the predefined common language codes
     * to simulate different user locales. The selection is uniformly distributed
     * across all available languages.
     * </p>
     *
     * <h3>Use Cases:</h3>
     * <ul>
     *   <li>Setting the Accept-Language header</li>
     *   <li>Configuring browser language preferences</li>
     *   <li>Simulating international user traffic</li>
     * </ul>
     *
     * <h3>Distribution:</h3>
     * <p>
     * Each language has an equal probability of being selected (10% chance
     * with the current array of 10 languages).
     * </p>
     *
     * @return a randomly selected language code in BCP 47 format (e.g., "en-US", "fr-FR")
     * @see ThreadLocalRandom#nextInt(int)
     */
    public static String getRandomLanguage() {

        // Select a random index within the LANGUAGES array bounds
        // ThreadLocalRandom.current() returns the thread-local instance for better concurrency
        return LANGUAGES[ThreadLocalRandom.current().nextInt(LANGUAGES.length)];
    }
}