package com.undetected.chromedriver.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test class for {@link RandomUtils}.
 * <p>
 * This test class validates the functionality of the RandomUtils utility class,
 * ensuring that generated user agents and language codes meet expected formats
 * and provide sufficient randomization for anti-detection purposes.
 * </p>
 *
 * <h2>Test Coverage:</h2>
 * <ul>
 *   <li>User agent format validation</li>
 *   <li>User agent randomization verification</li>
 *   <li>Language code format validation</li>
 *   <li>Language code value verification</li>
 * </ul>
 *
 * <h2>Testing Framework:</h2>
 * <p>
 * Uses JUnit 5 (Jupiter) for test execution and AssertJ for fluent assertions.
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see RandomUtils
 */
class RandomUtilsTest {
    /**
     * Tests that the generated user agent string is valid and contains required components.
     * <p>
     * This test verifies that {@link RandomUtils#getRandomUserAgent()} produces
     * a properly formatted Chrome user agent string containing:
     * <ul>
     *   <li>Non-null and non-empty value</li>
     *   <li>Mozilla/5.0 prefix (standard for all modern browsers)</li>
     *   <li>Chrome/ identifier</li>
     *   <li>Valid Chrome version format (e.g., Chrome/115.0.5790.0)</li>
     * </ul>
     * </p>
     *
     * @see RandomUtils#getRandomUserAgent()
     */
    @Test
    @DisplayName("Should generate valid user agent")
    void testGetRandomUserAgent() {
        String userAgent = RandomUtils.getRandomUserAgent();                    // Generate a random user agent string

        assertThat(userAgent).isNotNull();                                      // Verify the user agent is not null
        assertThat(userAgent).isNotEmpty();                                     // Verify the user agent is not empty
        assertThat(userAgent).contains("Mozilla/5.0");              // Verify it contains the standard Mozilla prefix
        assertThat(userAgent).contains("Chrome/");                  // Verify it contains the Chrome identifier

        // Verify it matches the Chrome version pattern: Chrome/{major}.0.{build}.0
        // Pattern explanation:
        // - Chrome/ - literal string
        // - \d+ - one or more digits (major version)
        // - \.0\. - literal .0.
        // - \d+ - one or more digits (build number)
        // - \.0 - literal .0
        assertThat(userAgent).matches(".*Chrome/\\d+\\.0\\.\\d+\\.0.*");
    }


    /**
     * Tests that the user agent generation provides adequate randomization.
     * <p>
     * This repeated test runs 10 times to ensure consistent behavior across
     * multiple executions. Each execution generates 100 user agents and verifies
     * that at least some variety exists in the generated values.
     * </p>
     * <p>
     * The test uses a {@link HashSet} to automatically deduplicate user agents,
     * allowing us to count unique values. This ensures the randomization is
     * working properly and not returning the same value repeatedly.
     * </p>
     *
     * @see RandomUtils#getRandomUserAgent()
     */
    @RepeatedTest(10)
    @DisplayName("Should generate different user agents")
    void testUserAgentRandomization() {
        Set<String> userAgents = new HashSet<>();   // Use a Set to store unique user agents (automatically deduplicates)

        for (int i = 0; i < 100; i++) {                         // Generate 100 user agents
            userAgents.add(RandomUtils.getRandomUserAgent());
        }

        // Should generate at least some variety
        // The set size represents the number of unique user agents generated
        assertThat(userAgents.size()).isGreaterThan(1);
    }


    /**
     * Tests that the generated language code follows the expected format.
     * <p>
     * This test verifies that {@link RandomUtils#getRandomLanguage()} returns
     * a properly formatted BCP 47 language tag in the format: {@code ll-CC}
     * where:
     * <ul>
     *   <li>{@code ll} - two lowercase letters representing the language code</li>
     *   <li>{@code CC} - two uppercase letters representing the country/region code</li>
     * </ul>
     * </p>
     * <p>
     * Example valid values: en-US, fr-FR, de-DE, ja-JP
     * </p>
     *
     * @see RandomUtils#getRandomLanguage()
     */
    @Test
    @DisplayName("Should generate valid language code")
    void testGetRandomLanguage() {
        String language = RandomUtils.getRandomLanguage();  // Generate a random language code

        assertThat(language).isNotNull();   // Verify the language code is not null

        // Verify it matches the pattern: two lowercase letters, hyphen, two uppercase letters
        // Pattern explanation:
        // - [a-z]{2} - exactly 2 lowercase letters
        // - - - literal hyphen
        // - [A-Z]{2} - exactly 2 uppercase letters
        assertThat(language).matches("[a-z]{2}-[A-Z]{2}");
    }


    /**
     * Tests that generated language codes are from a known, predefined set.
     * <p>
     * This test ensures that {@link RandomUtils#getRandomLanguage()} only returns
     * language codes from an expected set of values, preventing any unexpected
     * or invalid language codes from being generated.
     * </p>
     * <p>
     * The test generates 100 language codes to ensure comprehensive coverage
     * of the possible values, then verifies all generated codes exist in the
     * known set of valid codes.
     * </p>
     *
     * <h3>Expected Language Codes:</h3>
     * <ul>
     *   <li>{@code en-US} - English (United States)</li>
     *   <li>{@code en-GB} - English (United Kingdom)</li>
     *   <li>{@code es-ES} - Spanish (Spain)</li>
     *   <li>{@code fr-FR} - French (France)</li>
     *   <li>{@code de-DE} - German (Germany)</li>
     *   <li>{@code it-IT} - Italian (Italy)</li>
     *   <li>{@code pt-BR} - Portuguese (Brazil)</li>
     *   <li>{@code ja-JP} - Japanese (Japan)</li>
     *   <li>{@code ko-KR} - Korean (South Korea)</li>
     *   <li>{@code zh-CN} - Chinese (Simplified, China)</li>
     * </ul>
     *
     * @see RandomUtils#getRandomLanguage()
     */
    @Test
    @DisplayName("Should return known language codes")
    void testKnownLanguageCodes() {

        // Define the complete set of expected language codes
        // Using Set.of() creates an immutable set for comparison
        Set<String> knownCodes = Set.of(
                "en-US", "en-GB", "es-ES", "fr-FR", "de-DE",
                "it-IT", "pt-BR", "ja-JP", "ko-KR", "zh-CN"
        );

        Set<String> generatedCodes = new HashSet<>();           // Collect all generated language codes in a set
        for (int i = 0; i < 100; i++) {         // Generate 100 language codes to ensure we see most/all possible values
            generatedCodes.add(RandomUtils.getRandomLanguage());
        }

        // Verify that all generated codes are within the known set
        // isSubsetOf ensures every element in generatedCodes exists in knownCodes
        assertThat(generatedCodes).isSubsetOf(knownCodes);
    }
}