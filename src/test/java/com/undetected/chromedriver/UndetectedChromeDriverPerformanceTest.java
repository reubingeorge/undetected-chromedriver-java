package com.undetected.chromedriver;

import com.undetected.chromedriver.config.DriverConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance and stress tests for UndetectedChromeDriver
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class UndetectedChromeDriverPerformanceTest {

    private final List<UndetectedChromeDriver> drivers = new ArrayList<>();
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        for (UndetectedChromeDriver driver : drivers) {
            try {
                if (driver != null && driver.isActive()) {
                    driver.quit();
                }
            } catch (Exception ignored) {}
        }
        drivers.clear();

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
    @DisplayName("Should handle many concurrent instances")
    @Timeout(120)
    void testManyConcurrentInstances() throws Exception {
        int numInstances = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numInstances);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Create all instances concurrently
        for (int i = 0; i < numInstances; i++) {
            final int instanceNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for signal to start

                    DriverConfig config = DriverConfig.builder()
                            .requireLatestChrome(false)
                            .randomizeFingerprint(false)
                            .humanBehavior(false)
                            .build();

                    UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
                    drivers.add(driver);

                    driver.get("https://example.com");

                    if (driver.getTitle().contains("Example")) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Instance " + instanceNum + " failed: " + e.getMessage());
                    failureCount.incrementAndGet();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Start all at once
        startLatch.countDown();

        // Wait for completion
        assertThat(completeLatch.await(90, TimeUnit.SECONDS)).isTrue();

        System.out.println("Success: " + successCount.get() + ", Failures: " + failureCount.get());
        assertThat(successCount.get()).isGreaterThan(numInstances / 2); // At least half should succeed
    }

    @Test
    @DisplayName("Should handle rapid sequential navigations")
    @Timeout(60)
    void testRapidSequentialNavigations() throws Exception {
        UndetectedChromeDriver driver = new UndetectedChromeDriver(
                DriverConfig.builder()
                        .humanBehavior(false)
                        .randomizeFingerprint(false)
                        .build()
        );
        drivers.add(driver);

        String[] urls = {
                "https://example.com",
                "https://example.org",
                "https://example.net",
                "https://httpbin.org/html",
                "https://www.google.com"
        };

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 20; i++) {
            String url = urls[i % urls.length];
            driver.get(url);
            assertThat(driver.getCurrentUrl()).contains(url.replace("https://", ""));
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("20 navigations completed in " + duration + "ms");

        // Should complete reasonably fast
        assertThat(duration).isLessThan(60000); // Less than 60 seconds
    }

    @Test
    @DisplayName("Should handle memory usage with long sessions")
    @Timeout(120)
    void testMemoryUsageOverTime() throws Exception {
        UndetectedChromeDriver driver = new UndetectedChromeDriver(
                DriverConfig.builder()
                        .humanBehavior(false)
                        .randomizeFingerprint(true) // Keep fingerprint randomization on
                        .build()
        );
        drivers.add(driver);

        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Navigate many times
        for (int i = 0; i < 50; i++) {
            driver.get("https://example.com");

            // Perform some operations
            driver.findElement(org.openqa.selenium.By.tagName("body"));
            driver.executeScript("return document.title");

            if (i % 10 == 0) {
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                System.out.println("Iteration " + i + " - Memory: " +
                        (currentMemory - initialMemory) / 1024 / 1024 + " MB increase");
            }
        }

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = (finalMemory - initialMemory) / 1024 / 1024; // MB

        System.out.println("Total memory increase: " + memoryIncrease + " MB");

        // Memory increase should be reasonable (not a leak)
        assertThat(memoryIncrease).isLessThan(500); // Less than 500MB increase
    }

    @Test
    @DisplayName("Should handle concurrent operations on multiple drivers")
    @Timeout(60)
    void testConcurrentMultiDriverOperations() throws Exception {
        int numDrivers = 5;
        List<UndetectedChromeDriver> testDrivers = new ArrayList<>();

        // Create drivers
        for (int i = 0; i < numDrivers; i++) {
            UndetectedChromeDriver driver = new UndetectedChromeDriver(
                    DriverConfig.builder()
                            .humanBehavior(false)
                            .randomizeFingerprint(false)
                            .build()
            );
            drivers.add(driver);
            testDrivers.add(driver);
            driver.get("https://example.com");
        }

        // Perform concurrent operations
        CountDownLatch latch = new CountDownLatch(numDrivers * 3);
        AtomicInteger operationCount = new AtomicInteger(0);

        for (UndetectedChromeDriver driver : testDrivers) {
            // Navigation operation
            executor.submit(() -> {
                try {
                    driver.get("https://example.org");
                    operationCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            // Find element operation
            executor.submit(() -> {
                try {
                    driver.findElement(org.openqa.selenium.By.tagName("body"));
                    operationCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            // JavaScript execution
            executor.submit(() -> {
                try {
                    driver.executeScript("return document.readyState");
                    operationCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(operationCount.get()).isEqualTo(numDrivers * 3);
    }

    @Test
    @DisplayName("Should measure stealth overhead")
    @Timeout(120)
    void testStealthOverhead() throws Exception {
        long regularTime = 0;
        long stealthTime = 0;
        int iterations = 5;

        // Test without stealth features
        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();

            UndetectedChromeDriver driver = new UndetectedChromeDriver(
                    DriverConfig.builder()
                            .randomizeFingerprint(false)
                            .humanBehavior(false)
                            .build()
            );
            drivers.add(driver);

            driver.get("https://example.com");
            regularTime += System.currentTimeMillis() - start;

            driver.quit();
            drivers.remove(driver);
        }

        // Test with full stealth features
        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();

            UndetectedChromeDriver driver = new UndetectedChromeDriver(
                    DriverConfig.builder()
                            .randomizeFingerprint(true)
                            .humanBehavior(true)
                            .behaviorProfile(HumanBehaviorSimulator.BehaviorProfile.FAST)
                            .build()
            );
            drivers.add(driver);

            driver.get("https://example.com");
            stealthTime += System.currentTimeMillis() - start;

            driver.quit();
            drivers.remove(driver);
        }

        double regularAvg = regularTime / (double) iterations;
        double stealthAvg = stealthTime / (double) iterations;
        double overhead = ((stealthAvg - regularAvg) / regularAvg) * 100;

        System.out.println("Average time without stealth: " + regularAvg + "ms");
        System.out.println("Average time with stealth: " + stealthAvg + "ms");
        System.out.println("Stealth overhead: " + String.format("%.2f", overhead) + "%");

        // Overhead should be reasonable
        assertThat(overhead).isLessThan(200); // Less than 200% overhead
    }
}