package com.undetected.chromedriver;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Thread-safe fingerprint randomizer for runtime fingerprint changes.
 * <p>
 * This class implements sophisticated browser fingerprint randomization to evade
 * detection systems that track users based on their browser characteristics.
 * It periodically modifies various browser APIs and properties to create a
 * constantly changing fingerprint that appears natural and realistic.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Thread-safe concurrent fingerprint modification</li>
 *   <li>Periodic randomization with variable intervals</li>
 *   <li>Multiple fingerprinting vector coverage</li>
 *   <li>Graceful error handling and recovery</li>
 *   <li>Asynchronous execution for performance</li>
 * </ul>
 *
 * <h2>Fingerprinting Techniques Covered:</h2>
 * <ul>
 *   <li><b>Canvas:</b> Pixel noise injection for canvas fingerprinting</li>
 *   <li><b>WebGL:</b> GPU vendor and renderer spoofing</li>
 *   <li><b>Audio:</b> Audio processing fingerprint modification</li>
 *   <li><b>Fonts:</b> Text measurement variations</li>
 *   <li><b>Battery:</b> Battery API status randomization</li>
 *   <li><b>Hardware:</b> CPU cores and memory spoofing</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * WebDriver driver = new ChromeDriver();
 * FingerprintRandomizer randomizer = new FingerprintRandomizer(driver, 1);
 *
 * // Start randomization
 * randomizer.start();
 *
 * // Use driver normally...
 * driver.get("https://example.com");
 *
 * // Stop when done
 * randomizer.stop();
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * This class is fully thread-safe. The start() and stop() methods can be
 * called from any thread and use atomic operations to ensure proper state
 * management. All fingerprint modifications are synchronized.
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see JavascriptExecutor
 */
@Slf4j
public class FingerprintRandomizer {

    /**
     * The WebDriver instance to randomize fingerprints for.
     * <p>
     * Must implement {@link JavascriptExecutor} for fingerprint modification
     * scripts to work. Non-JavaScript capable drivers are handled gracefully.
     * </p>
     */
    private final WebDriver driver;

    /**
     * Unique identifier for this randomizer instance.
     * <p>
     * Used in logging and thread naming to distinguish between multiple
     * concurrent randomizer instances.
     * </p>
     */
    private final int instanceId;

    /**
     * Scheduled executor service for periodic randomization.
     * <p>
     * Single-threaded executor that runs fingerprint randomization tasks
     * at variable intervals. Uses daemon threads for proper JVM shutdown.
     * </p>
     */
    private final ScheduledExecutorService executor;

    /**
     * Atomic flag indicating if randomization is currently active.
     * <p>
     * Ensures thread-safe state management and prevents multiple
     * concurrent start/stop operations.
     * </p>
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Future reference to the scheduled randomization task.
     * <p>
     * Used to cancel the periodic randomization when stop() is called.
     * May be null if randomization hasn't started.
     * </p>
     */
    private ScheduledFuture<?> randomizationTask;


    /**
     * Constructs a new FingerprintRandomizer for the given WebDriver.
     * <p>
     * Creates a daemon thread executor for periodic randomization tasks.
     * The executor uses a descriptive thread name including the instance ID.
     * </p>
     *
     * @param driver the WebDriver instance to randomize (should implement JavascriptExecutor)
     * @param instanceId unique identifier for this randomizer instance
     */
    public FingerprintRandomizer(WebDriver driver, int instanceId) {
        this.driver = driver;
        this.instanceId = instanceId;

        // Create single-threaded executor with custom thread factory
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            // Create daemon thread with descriptive name
            Thread t = new Thread(r, "FingerprintRandomizer-" + instanceId);
            t.setDaemon(true); // Daemon thread won't prevent JVM shutdown
            return t;
        });
    }


    /**
     * Start fingerprint randomization.
     * Thread-safe - can be called multiple times safely.
     * <p>
     * Initiates periodic fingerprint randomization with randomized intervals
     * to avoid predictable patterns. The first randomization occurs immediately,
     * followed by periodic updates.
     * </p>
     *
     * <h3>Randomization Schedule:</h3>
     * <ul>
     *   <li>Initial delay: 30-60 seconds (random)</li>
     *   <li>Period: 60-180 seconds (random)</li>
     * </ul>
     *
     * <h3>Idempotency:</h3>
     * <p>
     * Multiple calls to start() are safe and will not create multiple
     * randomization tasks. Only the first call has effect.
     * </p>
     */
    public void start() {
        // Atomic compare-and-set ensures only one start
        if (isRunning.compareAndSet(false, true)) {
            log.debug("Starting fingerprint randomization for instance #{}", instanceId);

            // Initial randomization; Apply fingerprint changes immediately
            randomizeFingerprint();

            // Schedule periodic randomization; Randomize intervals to avoid patterns
            int initialDelay = ThreadLocalRandom.current().nextInt(30, 60);
            int period = ThreadLocalRandom.current().nextInt(60, 180);

            //Schedule with a fixed rate (not affected by execution time)
            randomizationTask = executor.scheduleAtFixedRate(
                    this::randomizeFingerprint,
                    initialDelay,
                    period,
                    TimeUnit.SECONDS
            );
        }
    }


    /**
     * Stop fingerprint randomization.
     * Thread-safe - can be called multiple times safely.
     * <p>
     * Gracefully shuts down the randomization executor and cancels any
     * pending tasks. Ensures proper cleanup with timeout handling.
     * </p>
     *
     * <h3>Shutdown Process:</h3>
     * <ol>
     *   <li>Cancel scheduled tasks</li>
     *   <li>Shutdown executor gracefully</li>
     *   <li>Wait up to 5 seconds for termination</li>
     *   <li>Force shutdown if necessary</li>
     * </ol>
     *
     * <h3>Thread Interruption:</h3>
     * <p>
     * Properly handles thread interruption during shutdown, preserving
     * the interrupt status and ensuring cleanup completes.
     * </p>
     */
    public void stop() {
        // Atomic compare-and-set ensures only one stop
        if (isRunning.compareAndSet(true, false)) {
            log.debug("Stopping fingerprint randomization for instance #{}", instanceId);

            // Cancel a scheduled task if exists
            if (randomizationTask != null) {
                randomizationTask.cancel(false); // Don't interrupt if running
            }

            executor.shutdown(); // Shutdown executor gracefully
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) { // Wait for termination with timeout
                    executor.shutdownNow(); // Force shutdown of timeout
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                executor.shutdownNow(); // Force immediate shutdown
            }
        }
    }

    /**
     * Performs fingerprint randomization across all tracked vectors.
     * <p>
     * Executes all fingerprint modifications asynchronously for performance.
     * Each modification runs in parallel with a global timeout to prevent
     * hanging. Errors are logged but don't stop other randomizations.
     * </p>
     *
     * <h3>Execution Strategy:</h3>
     * <ul>
     *   <li>Check if randomization is still active</li>
     *   <li>Verify driver supports JavaScript execution</li>
     *   <li>Run all randomizations in parallel</li>
     *   <li>Apply 5-second timeout for all operations</li>
     * </ul>
     */
    private void randomizeFingerprint() {
        if (!isRunning.get()) { // Check if still running
            return;
        }

        try {
            if (!(driver instanceof JavascriptExecutor js)) { // Check if a driver supports JavaScript execution
                return; // Silently skip for non-JS drivers
            }

            // Randomize different fingerprint aspects; Run all randomization's in parallel for efficiency
            CompletableFuture.allOf(
                    CompletableFuture.runAsync(() -> randomizeCanvas(js)),
                    CompletableFuture.runAsync(() -> randomizeWebGL(js)),
                    CompletableFuture.runAsync(() -> randomizeAudio(js)),
                    CompletableFuture.runAsync(() -> randomizeFonts(js)),
                    CompletableFuture.runAsync(() -> randomizeBattery(js)),
                    CompletableFuture.runAsync(() -> randomizeHardware(js))
            ).get(5, TimeUnit.SECONDS); // Global timeout for all operations

        } catch (Exception e) {
            // Log but don't crash - randomization should be resilient
            log.warn("Error during fingerprint randomization for instance #{}", instanceId, e);
        }
    }


    /**
     * Randomizes Canvas fingerprinting by injecting noise into pixel data.
     * <p>
     * Overrides the Canvas toDataURL method to add tiny random variations
     * to pixel values. This changes the Canvas fingerprint while keeping
     * visual changes imperceptible.
     * </p>
     *
     * <h3>Noise Parameters:</h3>
     * <ul>
     *   <li>Range: 0.0001 to 0.001 (very small)</li>
     *   <li>Applied to RGB channels independently</li>
     *   <li>Centered around zero (±noise/2)</li>
     * </ul>
     *
     * @param js the JavascriptExecutor to use for script injection
     */
    private synchronized void randomizeCanvas(JavascriptExecutor js) {
        // Generate small noise value for pixel manipulation
        double noise = ThreadLocalRandom.current().nextDouble(0.0001, 0.001);

        // Canvas fingerprinting countermeasure script
        String script = String.format("""
            (function() {
                const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
                HTMLCanvasElement.prototype.toDataURL = function() {
                    const context = this.getContext('2d');
                    if (context) {
                        const imageData = context.getImageData(0, 0, this.width, this.height);
                        for (let i = 0; i < imageData.data.length; i += 4) {
                            imageData.data[i] += Math.random() * %.6f - %.6f;
                            imageData.data[i+1] += Math.random() * %.6f - %.6f;
                            imageData.data[i+2] += Math.random() * %.6f - %.6f;
                        }
                        context.putImageData(imageData, 0, 0);
                    }
                    return originalToDataURL.apply(this, arguments);
                };
            })();
            """, noise, noise/2, noise, noise/2, noise, noise/2);

        try {
            js.executeScript(script);
        } catch (Exception e) {
            // Log at trace level - failures are expected sometimes
            log.trace("Canvas randomization failed for instance #{}", instanceId, e);
        }
    }


    /**
     * Randomizes WebGL fingerprinting by spoofing GPU information.
     * <p>
     * Overrides WebGL's getParameter method to return randomized vendor
     * and renderer strings. This prevents GPU-based fingerprinting.
     * </p>
     *
     * <h3>WebGL Parameters:</h3>
     * <ul>
     *   <li>37445 (0x9245): UNMASKED_VENDOR_WEBGL</li>
     *   <li>37446 (0x9246): UNMASKED_RENDERER_WEBGL</li>
     * </ul>
     *
     * @param js the JavascriptExecutor to use for script injection
     */
    private synchronized void randomizeWebGL(JavascriptExecutor js) {
        // Get random GPU vendor and renderer strings
        String vendor = randomVendor();
        String renderer = randomRenderer();

        String script = String.format("""
            (function() {
                const getParameter = WebGLRenderingContext.prototype.getParameter;
                WebGLRenderingContext.prototype.getParameter = function(parameter) {
                    if (parameter === 37445) return '%s';
                    if (parameter === 37446) return '%s';
                    return getParameter.apply(this, arguments);
                };
            })();
            """, vendor, renderer);

        try {
            js.executeScript(script);
        } catch (Exception e) {
            log.trace("WebGL randomization failed for instance #{}", instanceId, e);
        }
    }


    /**
     * Randomizes Audio fingerprinting by modifying audio processing.
     * <p>
     * Adds tiny variations to audio oscillator output and dynamics
     * compressor settings. These changes affect the audio fingerprint
     * while being inaudible to users.
     * </p>
     *
     * <h3>Audio Modifications:</h3>
     * <ul>
     *   <li>Oscillator gain: 0.00001-0.0001 reduction</li>
     *   <li>Compressor threshold: 0.00001-0.0001 offset</li>
     * </ul>
     *
     * @param js the JavascriptExecutor to use for script injection
     */
    private synchronized void randomizeAudio(JavascriptExecutor js) {
        double oscillatorNoise = ThreadLocalRandom.current().nextDouble(0.00001, 0.0001);
        double dynamicsNoise = ThreadLocalRandom.current().nextDouble(0.00001, 0.0001);

        // Audio fingerprinting countermeasure script
        String script = String.format("""
            (function() {
                const AudioContext = window.AudioContext || window.webkitAudioContext;
                if (AudioContext) {
                    const originalCreateOscillator = AudioContext.prototype.createOscillator;
                    AudioContext.prototype.createOscillator = function() {
                        const oscillator = originalCreateOscillator.apply(this, arguments);
                        const originalConnect = oscillator.connect;
                        oscillator.connect = function() {
                            const gainNode = this.context.createGain();
                            gainNode.gain.value = 1 - %.6f;
                            originalConnect.call(this, gainNode);
                            return gainNode.connect.apply(gainNode, arguments);
                        };
                        return oscillator;
                    };
                    
                    const originalCreateDynamicsCompressor = AudioContext.prototype.createDynamicsCompressor;
                    AudioContext.prototype.createDynamicsCompressor = function() {
                        const compressor = originalCreateDynamicsCompressor.apply(this, arguments);
                        compressor.threshold.value += %.6f;
                        return compressor;
                    };
                }
            })();
            """, oscillatorNoise, dynamicsNoise);

        try {
            js.executeScript(script);
        } catch (Exception e) {
            log.trace("Audio randomization failed for instance #{}", instanceId, e);
        }
    }


    /**
     * Randomizes Font fingerprinting by modifying text measurements.
     * <p>
     * Adds small pixel offsets to text width measurements. This changes
     * font-based fingerprints while keeping text visually identical.
     * </p>
     *
     * <h3>Font Offset:</h3>
     * <ul>
     *   <li>Range: -2 to +2 pixels</li>
     *   <li>Applied to measureText width only</li>
     *   <li>Consistent per randomization cycle</li>
     * </ul>
     *
     * @param js the JavascriptExecutor to use for script injection
     */
    private synchronized void randomizeFonts(JavascriptExecutor js) {
        // Generate small offset for text measurements
        int fontOffset = ThreadLocalRandom.current().nextInt(-2, 3);

        // Font fingerprinting countermeasure script
        String script = String.format("""
            (function() {
                const originalMeasureText = CanvasRenderingContext2D.prototype.measureText;
                CanvasRenderingContext2D.prototype.measureText = function() {
                    const metrics = originalMeasureText.apply(this, arguments);
                    const originalWidth = metrics.width;
                    Object.defineProperty(metrics, 'width', {
                        get: () => originalWidth + %d
                    });
                    return metrics;
                };
            })();
            """, fontOffset);

        try {
            js.executeScript(script);
        } catch (Exception e) {
            log.trace("Font randomization failed for instance #{}", instanceId, e);
        }
    }


    /**
     * Randomizes Battery API fingerprinting by spoofing battery status.
     * <p>
     * Overrides the Battery Status API to return randomized but realistic
     * battery information. This prevents battery-based tracking.
     * </p>
     *
     * <h3>Battery Parameters:</h3>
     * <ul>
     *   <li>Level: 50-100% (0.5-1.0)</li>
     *   <li>Charging: Random true/false</li>
     *   <li>Charging time: 0-3600 seconds if charging</li>
     *   <li>Discharging time: 3600-28800 seconds if not charging</li>
     * </ul>
     *
     * @param js the JavascriptExecutor to use for script injection
     */
    private synchronized void randomizeBattery(JavascriptExecutor js) {
        // Generate realistic battery parameters
        double batteryLevel = ThreadLocalRandom.current().nextDouble(0.5, 1.0);
        boolean charging = ThreadLocalRandom.current().nextBoolean();

        // Charging time only if charging
        int chargingTime = charging ? ThreadLocalRandom.current().nextInt(0, 3600) : 0;

        // Discharging time: infinity if charging, else 1-8 hours
        int dischargingTime = charging ? Integer.MAX_VALUE : ThreadLocalRandom.current().nextInt(3600, 28800);

        // Battery API spoofing a script
        String script = String.format("""
            (function() {
                if (navigator.getBattery) {
                    navigator.getBattery = async function() {
                        return {
                            charging: %s,
                            chargingTime: %d,
                            dischargingTime: %d,
                            level: %.2f
                        };
                    };
                }
            })();
            """,
                charging,
                chargingTime,
                dischargingTime,
                batteryLevel
        );

        try {
            js.executeScript(script);
        } catch (Exception e) {
            log.trace("Battery randomization failed for instance #{}", instanceId, e);
        }
    }


    /**
     * Randomizes Hardware fingerprinting by spoofing system capabilities.
     * <p>
     * Overrides hardware-related APIs to return randomized but plausible
     * values for CPU cores and memory. This prevents hardware-based tracking.
     * </p>
     *
     * <h3>Hardware Parameters:</h3>
     * <ul>
     *   <li>CPU cores: 2, 4, 6, 8, 12, or 16</li>
     *   <li>Memory: 2, 4, 8, 16, or 32 GB</li>
     * </ul>
     *
     * @param js the JavascriptExecutor to use for script injection
     */
    private synchronized void randomizeHardware(JavascriptExecutor js) {

        // Get random but realistic hardware values
        int cores = randomCores();
        int memory = randomMemory();

        // Hardware fingerprinting countermeasure script
        String script = String.format("""
            (function() {
                Object.defineProperty(navigator, 'hardwareConcurrency', {
                    get: () => %d
                });
                
                if (navigator.deviceMemory) {
                    Object.defineProperty(navigator, 'deviceMemory', {
                        get: () => %d
                    });
                }
            })();
            """, cores, memory);

        try {
            js.executeScript(script);
        } catch (Exception e) {
            log.trace("Hardware randomization failed for instance #{}", instanceId, e);
        }
    }


    /**
     * Returns a random GPU vendor string.
     * <p>
     * Selects from common GPU manufacturers to create realistic
     * WebGL fingerprints.
     * </p>
     *
     * @return random vendor string
     */
    private String randomVendor() {
        String[] vendors = {
                "Intel Inc.",
                "NVIDIA Corporation",
                "AMD",
                "Qualcomm",
                "ARM"
        };
        return vendors[ThreadLocalRandom.current().nextInt(vendors.length)];
    }


    /**
     * Returns a random GPU renderer string.
     * <p>
     * Selects from common GPU models to create realistic
     * WebGL fingerprints that match the vendor.
     * </p>
     *
     * @return random renderer string
     */
    private String randomRenderer() {
        String[] renderers = {
                "Intel Iris OpenGL Engine",
                "Intel HD Graphics 620",
                "NVIDIA GeForce GTX 1050",
                "AMD Radeon Pro 560",
                "Mesa DRI Intel(R) HD Graphics"
        };
        return renderers[ThreadLocalRandom.current().nextInt(renderers.length)];
    }


    /**
     * Returns a random CPU core count.
     * <p>
     * Selects from common core counts found in modern systems.
     * </p>
     *
     * @return random core count
     */
    private int randomCores() {
        int[] coreCounts = {2, 4, 6, 8, 12, 16}; // Common CPU core counts
        return coreCounts[ThreadLocalRandom.current().nextInt(coreCounts.length)];
    }


    /**
     * Returns a random memory amount in GB.
     * <p>
     * Selects from common RAM configurations found in modern systems.
     * </p>
     *
     * @return random memory amount in GB
     */
    private int randomMemory() {
        int[] memoryAmounts = {2, 4, 8, 16, 32}; // Common RAM amounts in GB
        return memoryAmounts[ThreadLocalRandom.current().nextInt(memoryAmounts.length)];
    }
}