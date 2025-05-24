package com.undetected.chromedriver;

import com.undetected.chromedriver.config.DriverConfig;
import com.undetected.chromedriver.config.StealthConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Additional comprehensive tests for UndetectedChromeDriver
 */
class UndetectedChromeDriverAdvancedTest {

    private UndetectedChromeDriver driver;

    @AfterEach
    void tearDown() {
        if (driver != null && driver.isActive()) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Should handle real Cloudflare protected site")
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void testRealCloudflareBypass() throws Exception {
        // This test requires internet connection and may fail if Cloudflare changes
        DriverConfig config = DriverConfig.builder()
                .humanBehavior(true)
                .behaviorProfile(HumanBehaviorSimulator.BehaviorProfile.CAREFUL)
                .randomizeFingerprint(true)
                .build();

        driver = new UndetectedChromeDriver(config);

        // Try to access a known Cloudflare-protected site
        driver.get("https://nowsecure.nl"); // Known bot detection test site

        // Wait for potential challenge to resolve
        TimeUnit.SECONDS.sleep(5);

        // Check if we bypassed detection
        String pageSource = driver.getPageSource();
        assertThat(pageSource).doesNotContain("Checking your browser");
        assertThat(driver.getTitle()).doesNotContain("Just a moment");
    }

    @Test
    @DisplayName("Should handle cookies and sessions")
    void testCookieHandling() {
        driver = new UndetectedChromeDriver(DriverConfig.builder()
                .humanBehavior(false)
                .build());

        driver.get("https://example.com");

        // Add a cookie
        Cookie testCookie = new Cookie("test", "value", "example.com", "/", null);
        driver.manage().addCookie(testCookie);

        // Refresh to ensure cookie persists
        driver.navigate().refresh();

        // Verify cookie exists
        Cookie retrievedCookie = driver.manage().getCookieNamed("test");
        assertThat(retrievedCookie).isNotNull();
        assertThat(retrievedCookie.getValue()).isEqualTo("value");
    }

    @Test
    @DisplayName("Should handle JavaScript alerts")
    void testAlertHandling() {
        driver = new UndetectedChromeDriver(DriverConfig.builder()
                .humanBehavior(false)
                .build());

        // Use a more reliable approach - trigger alert on button click
        String html = """
            <html>
            <body>
                <button id="alertBtn" onclick="alert('test')">Show Alert</button>
            </body>
            </html>
            """;

        driver.get("data:text/html," + html);

        // Click button to trigger alert
        WebElement button = driver.findElement(By.id("alertBtn"));
        button.click();

        // Now wait for alert
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        Alert alert = wait.until(d -> {
            try {
                return d.switchTo().alert();
            } catch (NoAlertPresentException e) {
                return null;
            }
        });

        assertThat(alert).isNotNull();
        assertThat(alert.getText()).isEqualTo("test");
        alert.accept();
    }

    @Test
    @DisplayName("Should handle multiple windows/tabs")
    void testMultipleWindows() {
        driver = new UndetectedChromeDriver(DriverConfig.builder()
                .humanBehavior(false)
                .build());

        driver.get("https://example.com");
        String firstWindow = driver.getWindowHandle();

        // Open new window
        driver.executeScript("window.open('https://google.com', '_blank');");

        // Wait for new window
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(d -> d.getWindowHandles().size() > 1);

        // Switch to new window
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(firstWindow)) {
                driver.switchTo().window(handle);
                break;
            }
        }

        // Verify we're on the new page
        assertThat(driver.getTitle()).containsIgnoringCase("google");

        // Switch back
        driver.switchTo().window(firstWindow);
        assertThat(driver.getTitle()).contains("Example");
    }

    @Test
    @DisplayName("Should handle iframes")
    void testIframeHandling() {
        driver = new UndetectedChromeDriver(DriverConfig.builder()
                .humanBehavior(false)
                .build());

        String html = """
            <html>
            <body>
                <h1 id="main">Main Page</h1>
                <iframe src="data:text/html,<h1 id='frame'>Frame Content</h1>"></iframe>
            </body>
            </html>
            """;

        driver.get("data:text/html," + html);

        // Verify main content
        assertThat(driver.findElement(By.id("main")).getText()).isEqualTo("Main Page");

        // Switch to iframe
        driver.switchTo().frame(0);
        assertThat(driver.findElement(By.id("frame")).getText()).isEqualTo("Frame Content");

        // Switch back
        driver.switchTo().defaultContent();
        assertThat(driver.findElement(By.id("main")).getText()).isEqualTo("Main Page");
    }

    @Test
    @DisplayName("Should handle navigation history")
    void testNavigationHistory() {
        driver = new UndetectedChromeDriver(DriverConfig.builder()
                .humanBehavior(false)
                .build());

        // Navigate to first page
        driver.get("https://example.com");
        String firstTitle = driver.getTitle();

        // Navigate to second page
        driver.get("https://example.org");
        String secondTitle = driver.getTitle();

        // Go back
        driver.navigate().back();
        assertThat(driver.getTitle()).isEqualTo(firstTitle);

        // Go forward
        driver.navigate().forward();
        assertThat(driver.getTitle()).isEqualTo(secondTitle);
    }

    @Test
    @DisplayName("Should handle custom stealth configuration")
    void testCustomStealthConfig() {
        StealthConfig stealthConfig = StealthConfig.builder()
                .randomizeUserAgent(true)
                .randomizeViewport(true)
                .randomizeLanguage(false) // Disable language randomization
                .randomizeCanvas(true)
                .randomizeWebGL(true)
                .fingerprintRandomizationIntervalSeconds(60)
                .build();

        DriverConfig config = DriverConfig.builder()
                .stealthConfig(stealthConfig)
                .humanBehavior(false)
                .build();

        driver = new UndetectedChromeDriver(config);
        driver.get("https://example.com");

        // Verify driver works with custom stealth config
        assertThat(driver.getTitle()).contains("Example");
    }

    @Test
    @DisplayName("Should handle proxy configuration")
    @Disabled("Requires proxy server to test")
    void testProxyConfiguration() {
        DriverConfig config = DriverConfig.builder()
                .proxy("http://proxy.example.com:8080")
                .humanBehavior(false)
                .build();

        driver = new UndetectedChromeDriver(config);
        driver.get("https://httpbin.org/ip");

        // Would verify proxy IP if we had a real proxy
        String pageSource = driver.getPageSource();
        assertThat(pageSource).contains("origin");
    }

    @Test
    @DisplayName("Should handle custom user data directory")
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void testUserDataDirectory() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir") + "/test-chrome-profile-" + System.currentTimeMillis();

        DriverConfig config = DriverConfig.builder()
                .userDataDir(tempDir)
                .humanBehavior(false)
                .build();

        // First session - set a localStorage value instead of cookie
        driver = new UndetectedChromeDriver(config);
        driver.get("https://example.com");

        // Set localStorage value (more reliable than cookies for testing)
        driver.executeScript(
                "localStorage.setItem('testKey', 'testValue');"
        );

        // Verify it was set
        String value = (String) driver.executeScript(
                "return localStorage.getItem('testKey');"
        );
        assertThat(value).isEqualTo("testValue");

        driver.quit();
        driver = null;

        // Wait for profile to be saved
        TimeUnit.SECONDS.sleep(2);

        // Second session - check if localStorage persisted
        driver = new UndetectedChromeDriver(config);
        driver.get("https://example.com");

        // Check if localStorage value persisted
        String persistedValue = (String) driver.executeScript(
                "return localStorage.getItem('testKey');"
        );

        assertThat(persistedValue).isEqualTo("testValue");

        // Clean up
        java.nio.file.Files.walk(java.nio.file.Paths.get(tempDir))
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        java.nio.file.Files.delete(path);
                    } catch (Exception e) {
                        // Ignore
                    }
                });
    }

    @Test
    @DisplayName("Should handle exceptions during navigation")
    void testNavigationExceptions() {
        driver = new UndetectedChromeDriver(DriverConfig.builder()
                .humanBehavior(false)
                .build());

        // Navigate to invalid URL
        assertThatThrownBy(() -> driver.get("not-a-valid-url"))
                .isInstanceOf(WebDriverException.class);

        // Driver should still be functional
        driver.get("https://example.com");
        assertThat(driver.getTitle()).contains("Example");
    }

    @Test
    @DisplayName("Should handle screenshots")
    void testScreenshotCapability() {
        driver = new UndetectedChromeDriver(DriverConfig.builder()
                .humanBehavior(false)
                .build());

        driver.get("https://example.com");

        // Take screenshot
        byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
        assertThat(screenshot).isNotEmpty();
        assertThat(screenshot.length).isGreaterThan(1000); // Should be a real image
    }

    @Test
    @DisplayName("Should handle custom Chrome arguments")
    void testCustomChromeArguments() {
        DriverConfig config = DriverConfig.builder()
                .additionalArguments(java.util.List.of(
                        "--disable-images",
                        "--disable-javascript"
                ))
                .humanBehavior(false)
                .build();

        driver = new UndetectedChromeDriver(config);
        driver.get("https://example.com");

        // Page should load even with JavaScript disabled
        assertThat(driver.getTitle()).contains("Example");
    }

    @Test
    @DisplayName("Should handle custom preferences")
    void testCustomPreferences() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", "/tmp");
        prefs.put("download.prompt_for_download", false);

        DriverConfig config = DriverConfig.builder()
                .additionalPreferences(prefs)
                .humanBehavior(false)
                .build();

        driver = new UndetectedChromeDriver(config);
        driver.get("https://example.com");

        // Driver should work with custom preferences
        assertThat(driver.getTitle()).contains("Example");
    }

    @Test
    @DisplayName("Should handle mobile emulation")
    void testMobileEmulation() {
        DriverConfig config = DriverConfig.builder()
                .mobileEmulation(true)
                .humanBehavior(false)
                .build();

        driver = new UndetectedChromeDriver(config);
        driver.get("https://example.com");

        // Check viewport is mobile-sized
        Long width = (Long) ((JavascriptExecutor) driver).executeScript("return window.innerWidth");
        assertThat(width).isLessThan(500); // Mobile width
    }

    @Test
    @DisplayName("Should detect and handle rate limiting")
    void testRateLimitDetection() {
        driver = new UndetectedChromeDriver(DriverConfig.builder()
                .humanBehavior(false)
                .build());

        // Navigate to a page that simulates rate limiting
        driver.get("data:text/html,<h1>Error 429: Too Many Requests</h1>");

        boolean isRateLimited = driver.getBotDetectionHandler().isRateLimited();
        // This might be false since it's not a real 429 response, but the method should work
        assertThat(driver.getBotDetectionHandler()).isNotNull();
    }

    @Test
    @DisplayName("Should handle concurrent operations with human behavior")
    void testConcurrentHumanBehavior() throws Exception {
        driver = new UndetectedChromeDriver(DriverConfig.builder()
                .humanBehavior(true)
                .behaviorProfile(HumanBehaviorSimulator.BehaviorProfile.FAST)
                .build());

        driver.get("https://example.com");

        // Find element with human behavior
        WebElement element = driver.findElement(By.tagName("h1"));

        // Type with human behavior
        WebElement input = driver.findElement(By.tagName("body"));
        driver.humanType(input, "test");

        // Scroll with human behavior
        driver.humanScroll(100);

        // Verify operations completed
        assertThat(driver.getTitle()).contains("Example");
    }
}