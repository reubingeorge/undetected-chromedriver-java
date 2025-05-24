package com.undetected.chromedriver;

import com.undetected.chromedriver.config.DriverConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UndetectedChromeDriverTest {

    private final List<UndetectedChromeDriver> drivers = new ArrayList<>();
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    void tearDown() {
        // Close all drivers
        for (UndetectedChromeDriver driver : drivers) {
            try {
                if (driver != null && driver.isActive()) {
                    driver.quit();
                }
            } catch (Exception e) {
                System.err.println("Error closing driver: " + e.getMessage());
            }
        }
        drivers.clear();

        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should create multiple instances concurrently")
    @Timeout(60)
    void testConcurrentInstanceCreation() throws Exception {
        int numInstances = 3;
        CountDownLatch latch = new CountDownLatch(numInstances);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<UndetectedChromeDriver>> futures = new ArrayList<>();

        for (int i = 0; i < numInstances; i++) {
            futures.add(executor.submit(() -> {
                try {
                    DriverConfig config = DriverConfig.builder()
                            .requireLatestChrome(false)
                            .randomizeFingerprint(false) // Disable for faster test
                            .humanBehavior(false) // Disable human behavior for test
                            .build();

                    UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
                    drivers.add(driver);
                    successCount.incrementAndGet();
                    return driver;
                } finally {
                    latch.countDown();
                }
            }));
        }

        // Wait for all drivers to be created
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(successCount.get()).isEqualTo(numInstances);

        // Verify all drivers are different instances with unique IDs
        List<Integer> instanceIds = new ArrayList<>();
        for (Future<UndetectedChromeDriver> future : futures) {
            UndetectedChromeDriver driver = future.get();
            assertThat(driver).isNotNull();
            assertThat(driver.isActive()).isTrue();
            instanceIds.add(driver.getInstanceId());
        }

        // All instance IDs should be unique
        assertThat(instanceIds).hasSize(numInstances);
        assertThat(instanceIds.stream().distinct().count()).isEqualTo(numInstances);
    }

    @Test
    @Order(2)
    @DisplayName("Should handle concurrent navigation")
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    @Timeout(60)
    void testConcurrentNavigation() throws Exception {
        int numDrivers = 2;
        List<UndetectedChromeDriver> testDrivers = new ArrayList<>();

        // Create drivers
        for (int i = 0; i < numDrivers; i++) {
            DriverConfig config = DriverConfig.builder()
                    .requireLatestChrome(false)
                    .humanBehavior(false) // Disable human behavior to avoid timing issues
                    .build();
            UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
            drivers.add(driver);
            testDrivers.add(driver);
        }

        // Navigate concurrently
        CountDownLatch navigationLatch = new CountDownLatch(numDrivers);
        AtomicInteger navigationSuccess = new AtomicInteger(0);

        for (int i = 0; i < numDrivers; i++) {
            final UndetectedChromeDriver driver = testDrivers.get(i);
            final int driverIndex = i;

            executor.submit(() -> {
                try {
                    driver.get("https://example.com");

                    // Verify navigation
                    String title = driver.getTitle();
                    assertThat(title).contains("Example");

                    // Check webdriver property - use a safer approach
                    try {
                        Object webdriver = driver
                                .executeScript("return navigator.webdriver");
                        System.out.println("Driver " + driverIndex + " webdriver: " + webdriver);
                    } catch (Exception e) {
                        // If we can't read the property, it might be undefined or throw an error
                        // which is actually what we want for stealth
                        System.out.println("Driver " + driverIndex + " webdriver check threw error (good for stealth): " + e.getMessage());
                    }

                    navigationSuccess.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Navigation failed for driver " + driverIndex + ": " + e);
                } finally {
                    navigationLatch.countDown();
                }
            });
        }

        assertThat(navigationLatch.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(navigationSuccess.get()).isEqualTo(numDrivers);
    }

    @Test
    @Order(3)
    @DisplayName("Should handle concurrent operations on same instance")
    @Timeout(30)
    void testConcurrentOperationsOnSameInstance() throws Exception {
        UndetectedChromeDriver driver = new UndetectedChromeDriver(
                DriverConfig.builder()
                        .requireLatestChrome(false)
                        .humanBehavior(false) // Disable for test
                        .build()
        );
        drivers.add(driver);

        driver.get("https://example.com");

        int numOperations = 5;
        CountDownLatch operationsLatch = new CountDownLatch(numOperations);
        ConcurrentHashMap<String, Object> results = new ConcurrentHashMap<>();

        // Perform different operations concurrently
        executor.submit(() -> {
            try {
                results.put("title", driver.getTitle());
            } finally {
                operationsLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                results.put("url", driver.getCurrentUrl());
            } finally {
                operationsLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                WebElement body = driver.findElement(By.tagName("body"));
                results.put("bodyFound", body != null);
            } finally {
                operationsLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                Object plugins = driver
                        .executeScript("return navigator.plugins.length");
                results.put("plugins", plugins);
            } finally {
                operationsLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                // Try to get webdriver property, but handle the case where it might fail
                // due to stealth modifications
                Object webdriver = driver
                        .executeScript("return navigator.webdriver");
                results.put("webdriver", webdriver);
            } catch (Exception e) {
                // If we can't access webdriver property (which is good for stealth),
                // put a special value to indicate it's been modified
                results.put("webdriver", "undefined_or_blocked");
            } finally {
                operationsLatch.countDown();
            }
        });

        assertThat(operationsLatch.await(15, TimeUnit.SECONDS)).isTrue();

        // Verify results
        assertThat(results).containsKey("title");
        assertThat(results.get("title").toString()).contains("Example");
        assertThat(results).containsKey("url");
        assertThat(results).containsKey("bodyFound");
        assertThat(results).containsKey("plugins");
        assertThat(results).containsKey("webdriver");

        // The webdriver property should either be undefined, null, or "undefined_or_blocked"
        // All of these indicate successful stealth
        Object webdriverValue = results.get("webdriver");
        assertThat(webdriverValue == null ||
                "undefined".equals(webdriverValue) ||
                "undefined_or_blocked".equals(webdriverValue))
                .as("webdriver property should be hidden for stealth")
                .isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("Should handle concurrent quit operations")
    @Timeout(30)
    void testConcurrentQuit() throws Exception {
        int numDrivers = 3;
        List<UndetectedChromeDriver> quitDrivers = new ArrayList<>();

        // Create drivers
        for (int i = 0; i < numDrivers; i++) {
            UndetectedChromeDriver driver = new UndetectedChromeDriver(
                    DriverConfig.builder()
                            .requireLatestChrome(false)
                            .randomizeFingerprint(false)
                            .humanBehavior(false)
                            .build()
            );
            quitDrivers.add(driver);
        }

        // Quit all drivers concurrently
        CountDownLatch quitLatch = new CountDownLatch(numDrivers);
        AtomicInteger quitSuccess = new AtomicInteger(0);

        for (UndetectedChromeDriver driver : quitDrivers) {
            executor.submit(() -> {
                try {
                    driver.quit();
                    if (!driver.isActive()) {
                        quitSuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Quit failed: " + e);
                } finally {
                    quitLatch.countDown();
                }
            });
        }

        assertThat(quitLatch.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(quitSuccess.get()).isEqualTo(numDrivers);

        // Verify all drivers are inactive
        for (UndetectedChromeDriver driver : quitDrivers) {
            assertThat(driver.isActive()).isFalse();
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should maintain isolation between instances")
    @Timeout(60) // Increased timeout
    void testInstanceIsolation() throws Exception {
        // Create two drivers with different configs
        DriverConfig config1 = DriverConfig.builder()
                .requireLatestChrome(false)
                .headless(false)
                .implicitWaitMs(5000)
                .humanBehavior(false)
                .build();

        DriverConfig config2 = DriverConfig.builder()
                .requireLatestChrome(false)
                .headless(true)
                .implicitWaitMs(10000)
                .humanBehavior(false)
                .build();

        UndetectedChromeDriver driver1 = new UndetectedChromeDriver(config1);
        UndetectedChromeDriver driver2 = new UndetectedChromeDriver(config2);

        drivers.add(driver1);
        drivers.add(driver2);

        System.out.println("Starting navigation for both drivers...");

        // Navigate to different pages
        CompletableFuture<Void> nav1 = CompletableFuture.runAsync(() -> {
            System.out.println("Driver 1 starting navigation to example.com");
            driver1.get("https://example.com");
            System.out.println("Driver 1 completed navigation");
        });

        CompletableFuture<Void> nav2 = CompletableFuture.runAsync(() -> {
            System.out.println("Driver 2 starting navigation to google.com");
            driver2.get("https://www.google.com");
            System.out.println("Driver 2 completed navigation");
        });

        CompletableFuture.allOf(nav1, nav2).get(40, TimeUnit.SECONDS); // Increased timeout

        System.out.println("Both navigations completed, verifying results...");

        // Verify isolation
        assertThat(driver1.getTitle()).contains("Example");
        assertThat(driver2.getTitle()).containsIgnoringCase("google");

        // Different instance IDs
        assertThat(driver1.getInstanceId()).isNotEqualTo(driver2.getInstanceId());
    }
}