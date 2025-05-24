package com.undetected.chromedriver;

import com.fasterxml.jackson.databind.*;
import com.undetected.chromedriver.utils.SystemUtils;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.*;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.regex.*;


/**
 * Manages Chrome version detection and validation.
 * <p>
 * This class provides comprehensive Chrome version management capabilities including
 * detection of installed Chrome versions across different operating systems and
 * retrieval of the latest stable version from Google's official API. It employs
 * multiple detection strategies to ensure reliable version information.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Multi-platform Chrome detection (Windows, macOS, Linux)</li>
 *   <li>Multiple detection strategies for reliability</li>
 *   <li>Latest version retrieval from Google's API</li>
 *   <li>Version comparison and update checking</li>
 *   <li>Fallback mechanisms for edge cases</li>
 * </ul>
 *
 * <h2>Detection Strategies:</h2>
 * <ol>
 *   <li><b>Quick Detection:</b> Registry (Windows) or plist (macOS)</li>
 *   <li><b>Execution:</b> Running Chrome with --version flag</li>
 *   <li><b>Alternative:</b> WMIC queries on Windows</li>
 *   <li><b>Fallback:</b> Known stable version as last resort</li>
 * </ol>
 *
 * <h2>Version Format:</h2>
 * <p>
 * Chrome versions follow the format: MAJOR.MINOR.BUILD.PATCH
 * Example: 121.0.6167.57
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * ChromeVersionManager manager = new ChromeVersionManager();
 *
 * // Get installed version
 * String currentVersion = manager.getCurrentVersion();
 * System.out.println("Installed: " + currentVersion);
 *
 * // Get latest version
 * String latestVersion = manager.getLatestVersion();
 * System.out.println("Latest: " + latestVersion);
 *
 * // Check if update available
 * boolean needsUpdate = !manager.isLatestVersion();
 * }</pre>
 *
 * @author Reubin George
 * @version 1.0
 * @since 1.0
 * @see SystemUtils
 */
@Slf4j
public class ChromeVersionManager {

    /**
     * Regular expression pattern for matching Chrome version numbers.
     * <p>
     * Matches version strings in the format X.X.X.X where X is one or more digits.
     * Captures the entire version string in group 1.
     * </p>
     *
     * <h3>Pattern Examples:</h3>
     * <ul>
     *   <li>Matches: 121.0.6167.57</li>
     *   <li>Matches: 120.0.6099.109</li>
     *   <li>Captures: Full version string</li>
     * </ul>
     */
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");

    /**
     * Google Chrome version history API endpoint template.
     * <p>
     * Official API for retrieving Chrome version information. The %s placeholder
     * is replaced with the platform identifier (win64, mac, mac_arm64, linux).
     * </p>
     *
     * <h3>API Details:</h3>
     * <ul>
     *   <li>Provider: Google</li>
     *   <li>Channel: Stable releases only</li>
     *   <li>Format: JSON response with a version array</li>
     * </ul>
     */
    private static final String CHROME_RELEASES_API =
            "https://versionhistory.googleapis.com/v1/chrome/platforms/%s/channels/stable/versions";

    /**
     * Jackson ObjectMapper for JSON parsing.
     * <p>
     * Used to parse responses from the Chrome version API. Configured with
     * default settings for simple JSON deserialization.
     * </p>
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new ChromeVersionManager instance.
     * <p>
     * Initializes the Jackson ObjectMapper for JSON parsing. No Chrome
     * detection is performed during construction.
     * </p>
     */
    public ChromeVersionManager() {
        this.objectMapper = new ObjectMapper(); // Initialize Jackson ObjectMapper for API response parsing
    }


    /**
     * Get the currently installed Chrome version.
     * <p>
     * This method attempts multiple strategies to detect the installed Chrome
     * version, starting with the fastest methods and falling back to more
     * reliable but slower approaches if needed.
     * </p>
     *
     * <h3>Detection Flow:</h3>
     * <ol>
     *   <li>Find a Chrome installation path</li>
     *   <li>Try quick version detection (registry/plist)</li>
     *   <li>Fall back to Chrome execution if needed</li>
     * </ol>
     *
     * @return Chrome version string in format X.X.X.X
     * @throws IOException if version detection fails or Chrome is not installed
     */
    public String getCurrentVersion() throws IOException {
        String chromePath = getChromePath(); // First, find Chrome installation

        // Check if Chrome is installed
        if (chromePath == null) {
            throw new IOException("Chrome installation not found");
        }

        // This is faster as it doesn't require executing Chrome
        String quickVersion = getQuickVersion();
        if (quickVersion != null) {
            return quickVersion;
        }

        // More reliable but slower method
        return getVersionByExecution(chromePath);
    }


    /**
     * Try to get a Chrome version without executing Chrome.
     * <p>
     * Platform-specific quick detection methods that avoid process execution
     * for better performance. These methods read version information from
     * system locations where Chrome stores its version.
     * </p>
     *
     * <h3>Platform Methods:</h3>
     * <ul>
     *   <li>Windows: Registry query</li>
     *   <li>macOS: Info.plist file</li>
     *   <li>Linux: Not supported (returns null)</li>
     * </ul>
     *
     * @return Chrome version string or null if quick detection fails
     */
    private String getQuickVersion() {
        if (SystemUtils.isWindows()) {
            return getWindowsRegistryVersion(); // Windows: Check registry
        } else if (SystemUtils.isMac()) {
            return getMacPlistVersion(); // macOS: Check Info.plist
        }
        return null; // Linux doesn't have a quick method
    }


    /**
     * Get a Chrome version from the Windows registry.
     * <p>
     * Queries the Windows registry for Chrome version information stored
     * in the BLBeacon key. This is faster than executing Chrome but may
     * not always be available.
     * </p>
     *
     * <h3>Registry Location:</h3>
     * <pre>
     * HKEY_CURRENT_USER\Software\Google\Chrome\BLBeacon\version
     * </pre>
     *
     * @return Chrome version string or null if isn't found in the registry
     */
    private String getWindowsRegistryVersion() {
        try {
            // Execute registry query command
            ProcessResult result = new ProcessExecutor()
                    .command("reg", "query",
                            "HKEY_CURRENT_USER\\Software\\Google\\Chrome\\BLBeacon",
                            "/v", "version")
                    .readOutput(true)
                    .timeout(2, TimeUnit.SECONDS) // Quick timeout for a registry query
                    .execute();

            String output = result.outputUTF8(); // Parse version from registry output
            Matcher matcher = VERSION_PATTERN.matcher(output);
            if (matcher.find()) {
                log.debug("Got Chrome version from registry: {}", matcher.group(1));
                return matcher.group(1);
            }
        } catch (Exception e) {
            // Registry query failed, will fall back to other methods
            log.debug("Failed to get version from registry", e);
        }
        return null;
    }


    /**
     * Get a Chrome version from macOS plist.
     * <p>
     * Reads a Chrome version from the Info.plist file in the Chrome application
     * bundle using the macOS defaults command. This is faster than executing
     * Chrome.
     * </p>
     *
     * <h3>Plist Location:</h3>
     * <pre>
     * /Applications/Google Chrome.app/Contents/Info.plist
     * Key: CFBundleShortVersionString
     * </pre>
     *
     * @return Chrome version string or null if isn't found in plist
     */
    private String getMacPlistVersion() {
        try {
            // Check if the Chrome app exists
            Path plistPath = Paths.get("/Applications/Google Chrome.app/Contents/Info.plist");
            if (Files.exists(plistPath)) {
                // Read version from plist using defaults command
                ProcessResult result = new ProcessExecutor()
                        .command("defaults", "read",
                                "/Applications/Google Chrome.app/Contents/Info.plist",
                                "CFBundleShortVersionString")
                        .readOutput(true)
                        .timeout(2, TimeUnit.SECONDS)
                        .execute();

                // Parse and validate version
                String version = result.outputUTF8().trim();
                if (VERSION_PATTERN.matcher(version).matches()) {
                    log.debug("Got Chrome version from plist: {}", version);
                    return version;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get version from plist", e); // Plist read failed, will fall back to other methods
        }
        return null;
    }


    /**
     * Get a Chrome version by executing Chrome with a --version flag.
     * <p>
     * The most reliable but slowest method. Executes Chrome with the --version
     * flag and parses the output. Includes multiple fallback strategies for
     * edge cases.
     * </p>
     *
     * <h3>Execution Strategy:</h3>
     * <ol>
     *   <li>Execute Chrome with a --version flag</li>
     *   <li>Capture output using custom stream processor</li>
     *   <li>Parse version from output</li>
     *   <li>Fall back to alternative methods if timeout</li>
     * </ol>
     *
     * @param chromePath path to Chrome executable
     * @return Chrome version string
     * @throws IOException if all detection methods fail
     */
    private String getVersionByExecution(String chromePath) throws IOException {
        try {
            // This is more efficient than processing full output
            StringBuilder versionOutput = new StringBuilder();
            LogOutputStream outputStream = new LogOutputStream() {
                @Override
                protected void processLine(String line) {
                    if (line != null) { // Process each line of output
                        Matcher matcher = VERSION_PATTERN.matcher(line); // Look for a version pattern in the line
                        if (matcher.find() && versionOutput.isEmpty()) {
                            versionOutput.append(matcher.group(1)); // Capture the first version found
                        }
                    }
                }
            };

            // Execute Chrome with a version flag
            ProcessResult result = new ProcessExecutor()
                    .command(chromePath, "--version")
                    .redirectOutput(outputStream)
                    .redirectError(new LogOutputStream() {
                        @Override
                        protected void processLine(String line) {
                            // Log at trace level for debugging
                            log.trace("Chrome stderr: {}", line);
                        }
                    })
                    .timeout(10, TimeUnit.SECONDS) // Increased timeout
                    .destroyOnExit() // Ensure process cleanup
                    .execute();

            // Check if we captured a version from the stream
            if (!versionOutput.isEmpty()) {
                String version = versionOutput.toString();
                log.debug("Got Chrome version by execution: {}", version);
                return version;
            }

            // This handles cases where an output format is unexpected
            String fullOutput = result.outputUTF8();
            Matcher matcher = VERSION_PATTERN.matcher(fullOutput);
            if (matcher.find()) {
                return matcher.group(1);
            }

            throw new IOException("Failed to parse Chrome version from output");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt flag
            throw new IOException("Chrome version detection was interrupted", e);
        } catch (TimeoutException e) {
            // Try alternative method for Windows
            if (SystemUtils.isWindows()) {
                return getWindowsChromeVersionAlternative(); // Chrome might be slow to start
            }
            throw new IOException("Chrome version detection timed out", e);
        } catch (Exception e) {
            throw new IOException("Failed to execute Chrome version command", e);
        }
    }


    /**
     * Alternative method to get a Chrome version on Windows using wmic.
     * <p>
     * Uses Windows Management Instrumentation (WMI) to query Chrome file
     * properties. This method works even if Chrome doesn't execute properly
     * but requires Chrome to be installed in the standard location.
     * </p>
     *
     * <h3>Fallback Strategy:</h3>
     * <ol>
     *   <li>Try WMIC query for a file version</li>
     *   <li>Parse version from WMI output</li>
     *   <li>Return a hardcoded stable version as a last resort</li>
     * </ol>
     *
     * @return Chrome version string (maybe hardcoded fallback)
     */
    private String getWindowsChromeVersionAlternative() {
        try {
            // Use WMIC to query a Chrome executable version
            ProcessResult result = new ProcessExecutor()
                    .command("wmic", "datafile", "where",
                            "name='C:\\\\Program Files\\\\Google\\\\Chrome\\\\Application\\\\chrome.exe'",
                            "get", "Version", "/value")
                    .readOutput(true)
                    .timeout(5, TimeUnit.SECONDS)
                    .execute();

            String output = result.outputUTF8(); // Parse version from WMI output
            // WMI returns format: Version=X.X.X.X
            Matcher matcher = Pattern.compile("Version=(\\d+\\.\\d+\\.\\d+\\.\\d+)")
                    .matcher(output);
            if (matcher.find()) {
                log.debug("Got Chrome version from wmic: {}", matcher.group(1));
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.debug("Failed to get version from wmic", e);
        }

        // This ensures operations can continue even if detection fails
        log.warn("Could not detect Chrome version, assuming recent stable version");
        return "121.0.6167.57"; // Known stable version
    }


    /**
     * Get the latest stable Chrome version.
     * <p>
     * Queries Google's official Chrome version API to get the latest stable
     * release version for the current platform. Requires internet connectivity.
     * </p>
     *
     * <h3>API Response Format:</h3>
     * <pre>{@code
     * {
     *   "versions": [
     *     {
     *       "version": "121.0.6167.57",
     *       "name": "chrome/platforms/win64/channels/stable/versions/121.0.6167.57"
     *     }
     *   ]
     * }
     * }</pre>
     *
     * @return latest Chrome version string
     * @throws IOException if API call fails or network is unavailable
     */
    public String getLatestVersion() throws IOException {
        // Determine platform for API query
        String platform = getApiPlatform();
        String apiUrl = String.format(CHROME_RELEASES_API, platform);

        try {
            // Make API request and parse JSON response
            JsonNode response = objectMapper.readTree(new URL(apiUrl));
            JsonNode versions = response.get("versions");

            // Extract first (latest) version from response
            if (versions != null && versions.isArray() && !versions.isEmpty()) {
                return versions.get(0).get("version").asText();
            }

            throw new IOException("No versions found in API response");
        } catch (Exception e) {
            log.error("Failed to get latest Chrome version", e);
            throw new IOException("Failed to get latest Chrome version", e);
        }
    }


    /**
     * Check if the current Chrome version is the latest.
     * <p>
     * Compares the installed Chrome version with the latest available version
     * from Google's API. Useful for determining if Chrome needs updating.
     * </p>
     *
     * <h3>Comparison Logic:</h3>
     * <ul>
     *   <li>Returns true if current >= latest</li>
     *   <li>Returns false if current < latest</li>
     *   <li>Throws IOException if detection fails</li>
     * </ul>
     *
     * @return true if current version is latest or newer
     * @throws IOException if version check fails
     */
    public boolean isLatestVersion() throws IOException {
        // Get both versions
        String current = getCurrentVersion();
        String latest = getLatestVersion();

        return compareVersions(current, latest) >= 0; // Compare versions numerically
    }


    /**
     * Find a Chrome installation path for the current platform.
     * <p>
     * Delegates to platform-specific search methods to locate the Chrome
     * executable. Each platform has different standard installation locations.
     * </p>
     *
     * @return path to Chrome executable or null if not found
     */
    private String getChromePath() {
        if (SystemUtils.isWindows()) {
            return findWindowsChrome();
        } else if (SystemUtils.isMac()) {
            return findMacChrome();
        } else {
            return findLinuxChrome();
        }
    }


    /**
     * Find Chrome installation on Windows.
     * <p>
     * Searches common Windows installation paths for Chrome executable.
     * Checks both user-specific and system-wide installation locations.
     * </p>
     *
     * <h3>Search Locations:</h3>
     * <ol>
     *   <li>%LOCALAPPDATA%\Google\Chrome</li>
     *   <li>%PROGRAMFILES%\Google\Chrome</li>
     *   <li>%PROGRAMFILES(X86)%\Google\Chrome</li>
     *   <li>C:\Program Files\Google\Chrome</li>
     *   <li>C:\Program Files (x86)\Google\Chrome</li>
     * </ol>
     *
     * @return path to chrome.exe or null if not found
     */
    private String findWindowsChrome() {
        String[] paths = {
                System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("PROGRAMFILES") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("PROGRAMFILES(X86)") + "\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
        };

        // Check each path for Chrome executable
        for (String path : paths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        return null;
    }


    /**
     * Find Chrome installation on macOS.
     * <p>
     * Searches standard macOS application locations for Chrome or Chromium.
     * Chrome is typically installed in the Applications folder.
     * </p>
     *
     * <h3>Search Locations:</h3>
     * <ol>
     *   <li>/Applications/Google Chrome.app</li>
     *   <li>/Applications/Chromium.app</li>
     * </ol>
     *
     * @return path to Chrome executable or null if not found
     */
    private String findMacChrome() {
        String[] paths = {
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "/Applications/Chromium.app/Contents/MacOS/Chromium"
        };

        // Check each path
        for (String path : paths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        return null;
    }


    /**
     * Find Chrome installation on Linux.
     * <p>
     * Uses the 'which' command to locate Chrome in the system PATH.
     * Searches for various Chrome/Chromium command names used by different
     * distributions.
     * </p>
     *
     * <h3>Search Commands:</h3>
     * <ol>
     *   <li>google-chrome</li>
     *   <li>google-chrome-stable</li>
     *   <li>chromium</li>
     *   <li>chromium-browser</li>
     * </ol>
     *
     * @return path to Chrome executable or null if not found
     */
    private String findLinuxChrome() {
        // Common Chrome command names on Linux
        String[] commands = {
                "google-chrome",
                "google-chrome-stable",
                "chromium",
                "chromium-browser"
        };

        // Try to find each command using 'which'
        for (String cmd : commands) {
            try {
                ProcessResult result = new ProcessExecutor()
                        .command("which", cmd)
                        .readOutput(true)
                        .timeout(3, TimeUnit.SECONDS)
                        .execute();

                // Check if the command was found
                String path = result.outputUTF8().trim();
                if (!path.isEmpty() && Files.exists(Paths.get(path))) {
                    return path;
                }
            } catch (InterruptedException e) {
                // Restore an interrupt flag and stop searching
                Thread.currentThread().interrupt();
                log.debug("Interrupted while searching for Chrome", e);
                break;
            } catch (TimeoutException e) {
                // Command took too long, try next
                log.debug("Timeout while searching for Chrome command: {}", cmd);
            } catch (Exception e) {
                log.debug("Error while searching for Chrome command: {}", cmd, e); // Command failed, try next
            }
        }

        return null;
    }


    /**
     * Get platform identifier for Chrome API.
     * <p>
     * Maps the current operating system to the platform identifier used by
     * Google's Chrome version API. Handles architecture differences for macOS.
     * </p>
     *
     * <h3>Platform Mappings:</h3>
     * <ul>
     *   <li>Windows → "win64"</li>
     *   <li>macOS Intel → "mac"</li>
     *   <li>macOS ARM → "mac_arm64"</li>
     *   <li>Linux → "linux"</li>
     * </ul>
     *
     * @return platform identifier for API queries
     */
    private String getApiPlatform() {
        if (SystemUtils.isWindows()) {
            return "win64";
        } else if (SystemUtils.isMac()) {
            return SystemUtils.isArm() ? "mac_arm64" : "mac"; // Differentiate between Intel and ARM Macs
        } else {
            return "linux";
        }
    }


    /**
     * Compare two version strings numerically.
     * <p>
     * Performs segment-by-segment numerical comparison of version strings.
     * Each segment (major, minor, build, patch) is compared as an integer.
     * </p>
     *
     * <h3>Comparison Examples:</h3>
     * <ul>
     *   <li>120.0.0.0 < 121.0.0.0 (returns -1)</li>
     *   <li>121.0.0.0 = 121.0.0.0 (returns 0)</li>
     *   <li>121.0.0.1 > 121.0.0.0 (returns 1)</li>
     * </ul>
     *
     * @param v1 first version string
     * @param v2 second version string
     * @return negative if v1 < v2, 0 if equal, positive if v1 > v2
     */
    private int compareVersions(String v1, String v2) {

        // Split versions into segments
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        // Compare each segment numerically
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {

            // Get segment value or 0 if missing
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2); // Compare segments
            }
        }

        return 0; // All segments equal
    }
}