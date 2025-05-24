package com.undetected.chromedriver;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe executor for JavaScript stealth techniques.
 * <p>
 * This class applies various JavaScript-based stealth techniques to make
 * automated Chrome browsers appear more like regular user-driven browsers.
 * It loads and executes multiple evasion scripts that modify browser APIs
 * and properties to avoid detection by anti-bot systems.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Thread-safe script execution with synchronization</li>
 *   <li>Cached script loading for performance</li>
 *   <li>Multiple evasion techniques in modular scripts</li>
 *   <li>Fallback to embedded scripts if files not found</li>
 *   <li>Runtime modifications for dynamic properties</li>
 * </ul>
 *
 * <h2>Stealth Techniques Applied:</h2>
 * <ul>
 *   <li><b>navigator.webdriver:</b> Removes automation indicator</li>
 *   <li><b>navigator.plugins:</b> Populates with realistic plugins</li>
 *   <li><b>navigator.permissions:</b> Fixes permission queries</li>
 *   <li><b>window.chrome:</b> Creates Chrome-specific objects</li>
 *   <li><b>WebGL:</b> Spoofs GPU vendor and renderer</li>
 *   <li><b>Screen:</b> Fixes dimensions in headless mode</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * WebDriver driver = new ChromeDriver();
 * StealthExecutor executor = new StealthExecutor(driver, 1);
 *
 * // Apply stealth techniques to current page
 * executor.applyStealthTechniques();
 *
 * // Navigate to protected site
 * driver.get("https://protected-site.com");
 *
 * // Reapply after navigation if needed
 * executor.applyStealthTechniques();
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * The class uses synchronized methods and ConcurrentHashMap to ensure
 * thread-safe operation when used from multiple threads.
 * </p>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see JavascriptExecutor
 */
@Slf4j
public class StealthExecutor {

    /**
     * Shared cache for stealth scripts (thread-safe).
     * <p>
     * Static cache shared across all StealthExecutor instances to avoid
     * reloading the same scripts. Uses ConcurrentHashMap for thread-safe
     * access without explicit synchronization.
     * </p>
     *
     * <h3>Cache Benefits:</h3>
     * <ul>
     *   <li>Avoids repeated file I/O operations</li>
     *   <li>Improves performance for multiple instances</li>
     *   <li>Thread-safe concurrent access</li>
     * </ul>
     */
    private static final ConcurrentHashMap<String, String> SCRIPT_CACHE = new ConcurrentHashMap<>();

    /**
     * The WebDriver instance to execute scripts on.
     * <p>
     * Must implement JavascriptExecutor for script execution to work.
     * Non-JavaScript capable drivers are handled gracefully.
     * </p>
     */
    private final WebDriver driver;

    /**
     * Unique identifier for this executor instance.
     * <p>
     * Used in logging to distinguish between multiple executor instances
     * when running parallel browser sessions.
     * </p>
     */
    private final int instanceId;

    /**
     * List of stealth scripts to execute.
     * <p>
     * Loaded once during construction and reused for all executions.
     * Contains both file-based and embedded scripts.
     * </p>
     */
    private final List<String> stealthScripts;


    /**
     * Constructs a new StealthExecutor for the given WebDriver.
     * <p>
     * Loads all stealth scripts during construction, using the cache
     * for previously loaded scripts. Scripts are loaded from classpath
     * resources with fallback to embedded strings.
     * </p>
     *
     * @param driver the WebDriver instance to apply stealth to
     * @param instanceId unique identifier for this executor
     */
    public StealthExecutor(WebDriver driver, int instanceId) {
        this.driver = driver;
        this.instanceId = instanceId;
        this.stealthScripts = loadStealthScripts(); // Load scripts once during construction
    }


    /**
     * Apply all stealth techniques to the current page.
     * Thread-safe - can be called from multiple threads.
     * <p>
     * Executes all loaded stealth scripts followed by runtime modifications.
     * The method is synchronized to ensure thread-safe execution when called
     * from multiple threads.
     * </p>
     *
     * <h3>Execution Order:</h3>
     * <ol>
     *   <li>Check if driver supports JavaScript</li>
     *   <li>Execute all loaded stealth scripts</li>
     *   <li>Apply runtime modifications</li>
     * </ol>
     *
     * <h3>Error Handling:</h3>
     * <p>
     * Individual script failures are logged but don't stop execution of
     * other scripts. This ensures maximum stealth application even if
     * some techniques fail.
     * </p>
     */
    public synchronized void applyStealthTechniques() {
        if (!(driver instanceof JavascriptExecutor js)) { // Check if driver supports JavaScript execution
            log.warn("Driver instance #{} does not support JavaScript execution", instanceId);
            return;
        }

        // Execute each stealth script
        for (String script : stealthScripts) {
            try {
                js.executeScript(script);
            } catch (Exception e) {
                // Log but continue with other scripts
                log.warn("Failed to execute stealth script for instance #{}", instanceId, e);
            }
        }

        // Apply runtime modifications; These are applied last as they may depend on other scripts
        applyRuntimeModifications(js);
    }


    /**
     * Loads all stealth scripts from files and cache.
     * <p>
     * Attempts to load each script file from classpath resources. Uses
     * the shared cache to avoid reloading scripts that have already been
     * loaded by other instances.
     * </p>
     *
     * <h3>Script Loading Strategy:</h3>
     * <ol>
     *   <li>Check the cache for a previously loaded script</li>
     *   <li>If not cached, load from the classpath resource</li>
     *   <li>If resource not found, use embedded fallback</li>
     *   <li>Cache the loaded script for future use</li>
     * </ol>
     *
     * @return list of loaded stealth scripts
     */
    private List<String> loadStealthScripts() {
        List<String> scripts = new ArrayList<>();

        // List of evasion script files to load
        String[] scriptFiles = {
                "navigator.webdriver.js",       // Remove webdriver property
                "navigator.permissions.js",     // Fix permissions API
                "navigator.plugins.js",         // Populate plugins array
                "window.chrome.js",             // Create a chrome object
                "chrome.runtime.js"             // Add runtime properties
        };

        // Load each script file
        for (String file : scriptFiles) {
            // Use cache to avoid reloading
            String script = SCRIPT_CACHE.computeIfAbsent(file, this::loadScriptFromFile);
            if (script != null && !script.isEmpty()) {
                scripts.add(script);
            }
        }

        // Load main stealth script; This is a comprehensive script that includes multiple evasions
        String mainScript = SCRIPT_CACHE.computeIfAbsent("stealth.min.js",
                this::loadScriptFromFile);
        if (mainScript != null && !mainScript.isEmpty()) {
            scripts.add(mainScript);
        }

        return scripts;
    }


    /**
     * Loads a script from a file with fallback to an embedded version.
     * <p>
     * Attempts to load the script from the classpath resource. If the
     * file is not found or loading fails, returns an embedded version
     * of the script.
     * </p>
     *
     * @param fileName the script file name to load
     * @return the loaded script content or embedded fallback
     */
    private String loadScriptFromFile(String fileName) {
        try {
            return loadScript("/stealth/evasions/" + fileName); // Try to load from classpath resource
        } catch (IOException e) {
            log.error("Failed to load stealth script: {}", fileName, e); // Log failure and return embedded version
            return getEmbeddedScript(fileName);
        }
    }


    /**
     * Loads a script from a classpath resource.
     * <p>
     * Uses the class loader to load scripts packaged with the application.
     * Scripts should be placed in the /stealth/evasions/ directory in the
     * classpath.
     * </p>
     *
     * @param resourcePath the classpath resource path
     * @return the script content
     * @throws IOException if resource loading fails
     */
    private String loadScript(String resourcePath) throws IOException {
        // Try to load resource from the classpath
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return getEmbeddedScript(resourcePath);  // Resource isn't found, use embedded fallback
            }

            Scanner scanner = new Scanner(is, StandardCharsets.UTF_8); // Read entire resource content
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }


    /**
     * Applies runtime modifications using inline JavaScript.
     * <p>
     * These modifications are applied directly rather than loaded from
     * files, as they need to be executed after page load and may need
     * to override properties that are set dynamically.
     * </p>
     *
     * <h3>Modifications Applied:</h3>
     * <ul>
     *   <li>navigator.webdriver removal</li>
     *   <li>navigator.plugins population</li>
     *   <li>permissions.query override</li>
     *   <li>window.chrome object creation</li>
     *   <li>WebGL vendor/renderer spoofing</li>
     *   <li>Screen dimension fixes for headless</li>
     * </ul>
     *
     * @param js the JavascriptExecutor to use
     */
    private void applyRuntimeModifications(JavascriptExecutor js) {
        try {
            js.executeScript("""
                // Instance-specific runtime modifications
                (function() {
                    // Override navigator.webdriver - check if configurable first
                    try {
                        const desc = Object.getOwnPropertyDescriptor(navigator, 'webdriver');
                        if (!desc || desc.configurable) {
                            Object.defineProperty(navigator, 'webdriver', {
                                get: () => undefined
                            });
                        }
                    } catch (e) {
                        // Property might be non-configurable
                    }
                    
                    // Override navigator.plugins with proper structure
                    try {
                        const originalPlugins = navigator.plugins;
                        if (originalPlugins.length === 0) {
                            Object.defineProperty(navigator, 'plugins', {
                                get: function() {
                                    return {
                                        0: {name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer'},
                                        1: {name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai'},
                                        2: {name: 'Native Client', filename: 'internal-nacl-plugin'},
                                        length: 3,
                                        item: function(i) { return this[i]; },
                                        namedItem: function(name) {
                                            for (let i = 0; i < this.length; i++) {
                                                if (this[i].name === name) return this[i];
                                            }
                                            return null;
                                        }
                                    };
                                }
                            });
                        }
                    } catch (e) {
                        // Plugins might already be defined
                    }
                    
                    // Fix permissions
                    try {
                        if (window.navigator.permissions && window.navigator.permissions.query) {
                            const originalQuery = window.navigator.permissions.query.bind(window.navigator.permissions);
                            window.navigator.permissions.query = function(parameters) {
                                if (parameters.name === 'notifications') {
                                    return Promise.resolve({ state: 'prompt' });
                                }
                                return originalQuery(parameters);
                            };
                        }
                    } catch (e) {
                        // Permissions might not be modifiable
                    }
                    
                    // Fix chrome object
                    if (!window.chrome) {
                        window.chrome = {};
                    }
                    
                    if (!window.chrome.runtime) {
                        window.chrome.runtime = {
                            PlatformOs: {
                                MAC: 'mac',
                                WIN: 'win',
                                ANDROID: 'android',
                                CROS: 'cros',
                                LINUX: 'linux',
                                OPENBSD: 'openbsd'
                            },
                            PlatformArch: {
                                ARM: 'arm',
                                X86_32: 'x86-32',
                                X86_64: 'x86-64'
                            }
                        };
                    }
                    
                    // Fix WebGL vendor
                    try {
                        const getParameter = WebGLRenderingContext.prototype.getParameter;
                        WebGLRenderingContext.prototype.getParameter = function(parameter) {
                            if (parameter === 37445) {
                                return 'Intel Inc.';
                            }
                            if (parameter === 37446) {
                                return 'Intel Iris OpenGL Engine';
                            }
                            return getParameter.apply(this, arguments);
                        };
                    } catch (e) {
                        // WebGL might not be available
                    }
                    
                    // Fix screen dimensions in headless
                    if (window.screen.width === 0) {
                        try {
                            Object.defineProperty(window.screen, 'width', {
                                get: () => 1920
                            });
                            Object.defineProperty(window.screen, 'height', {
                                get: () => 1080
                            });
                        } catch (e) {
                            // Screen properties might not be modifiable
                        }
                    }
                })();
                """);
        } catch (Exception e) {
            log.debug("Failed to apply runtime modifications for instance #{}: {}",
                    instanceId, e.getMessage());
        }
    }


    /**
     * Returns embedded versions of critical stealth scripts.
     * <p>
     * Provides fallback implementations for essential scripts when the
     * script files cannot be loaded from resources. These embedded versions
     * ensure basic stealth functionality even without external files.
     * </p>
     *
     * @param scriptName the name of the script to get an embedded version for
     * @return embedded script content or empty string if not available
     */
    private String getEmbeddedScript(String scriptName) {
        // Return embedded versions of critical scripts
        return switch (scriptName) {
            // Navigator webdriver removal
            case "navigator.webdriver.js", "/stealth/evasions/navigator.webdriver.js" -> """
                    Object.defineProperty(navigator, 'webdriver', {
                        get: () => undefined
                    });
                    """;

            // Chrome runtime creation
            case "chrome.runtime.js", "/stealth/evasions/chrome.runtime.js" -> """
                    if (!window.chrome) window.chrome = {};
                    if (!window.chrome.runtime) window.chrome.runtime = {};
                    """;

            // No embedded version for other scripts
            default -> "";
        };
    }
}