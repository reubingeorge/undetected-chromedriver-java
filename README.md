# Undetected ChromeDriver

[![Java](https://img.shields.io/badge/Java-11%2B-orange.svg)](https://www.java.com)
[![Selenium](https://img.shields.io/badge/Selenium-4.x-43B02A.svg)](https://www.selenium.dev)
[![Chrome](https://img.shields.io/badge/Chrome-Latest-4285F4.svg)](https://www.google.com/chrome/)
[![WebDriverManager](https://img.shields.io/badge/WebDriverManager-5.x-blue.svg)](https://github.com/bonigarcia/webdrivermanager)
[![Thread Safe](https://img.shields.io/badge/Thread-Safe-brightgreen.svg)](https://docs.oracle.com/javase/tutorial/essential/concurrency/sync.html)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A sophisticated Java implementation of Selenium ChromeDriver that bypasses anti-bot detection systems like Cloudflare, PerimeterX, and others. This library extends the standard ChromeDriver with advanced stealth techniques, human behavior simulation, and intelligent bot detection handling.

This project is inspired by and based on the concepts from the Python [undetected-chromedriver](https://github.com/ultrafunkamsterdam/undetected-chromedriver) by ultrafunkamsterdam, reimplemented in Java with additional features like human behavior simulation and enhanced thread safety.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Usage Examples](#usage-examples)
    - [Basic Usage](#basic-usage)
    - [Human Behavior Simulation](#human-behavior-simulation)
    - [Bot Detection Handling](#bot-detection-handling)
    - [Advanced Configuration](#advanced-configuration)
    - [Concurrent Usage](#concurrent-usage)
    - [Custom Behavior Profiles](#custom-behavior-profiles)
    - [Fingerprint Randomization](#fingerprint-randomization)
    - [Proxy Configuration](#proxy-configuration)
    - [Headless Mode](#headless-mode)
    - [Mobile Emulation](#mobile-emulation)
- [API Reference](#api-reference)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Performance Considerations](#performance-considerations)
- [Contributing](#contributing)
- [License](#license)

## Features

### Core Features
- **Automatic ChromeDriver Management** - No manual driver downloads required
- **Stealth JavaScript Injection** - Hides automation indicators like `navigator.webdriver`
- **Browser Fingerprint Randomization** - Dynamically changes browser fingerprints
- **Human Behavior Simulation** - Realistic mouse movements, typing, and scrolling
- **Bot Detection Monitoring** - Detects and handles Cloudflare, CAPTCHAs, and rate limiting
- **Thread-Safe Design** - Supports concurrent instances without conflicts
- **Comprehensive Configuration** - Extensive customization options via `DriverConfig`

### Anti-Detection Techniques
- Removes `navigator.webdriver` property
- Populates realistic `navigator.plugins` array
- Fixes Chrome runtime objects
- Spoofs WebGL vendor and renderer
- Randomizes canvas fingerprints
- Modifies audio context fingerprints
- Handles permission queries realistically
- Fixes screen dimensions in headless mode

### Human Behavior Simulation
- Natural mouse movements with Bezier curves
- Realistic typing with variable speed and typos
- Smooth scrolling with easing functions
- Random idle behaviors
- Page scanning patterns
- Configurable behavior profiles (FAST, NORMAL, CAREFUL)

## Requirements

- Java 11 or higher
- Chrome browser installed
- Maven or Gradle for dependency management

## Installation

### Maven

```xml
<dependency>
    <groupId>com.undetected</groupId>
    <artifactId>undetected-chromedriver</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.undetected:undetected-chromedriver:1.0.0'
```

## Quick Start

```java
import com.undetected.chromedriver.UndetectedChromeDriver;

public class QuickStart {
    public static void main(String[] args) {
        // Create driver with default configuration
        UndetectedChromeDriver driver = new UndetectedChromeDriver();
        
        try {
            // Navigate to a protected site
            driver.get("https://protected-site.com");
            
            // Use the driver like regular Selenium
            WebElement element = driver.findElement(By.id("search"));
            driver.humanType(element, "undetected selenium");
            
            // Perform human-like click
            WebElement button = driver.findElement(By.name("submit"));
            driver.humanClick(button);
            
        } finally {
            driver.quit();
        }
    }
}
```

## Configuration

The `DriverConfig` class provides extensive configuration options:

```java
import com.undetected.chromedriver.config.DriverConfig;
import com.undetected.chromedriver.HumanBehaviorSimulator.BehaviorProfile;

DriverConfig config = DriverConfig.builder()
    // Browser options
    .headless(false)                          // Run in headful mode
    .windowSize(1920, 1080)                   // Set window size
    .startMaximized(false)                    // Don't start maximized
    
    // Anti-detection features
    .randomizeFingerprint(true)               // Enable fingerprint randomization
    .humanBehavior(true)                      // Enable human behavior simulation
    .behaviorProfile(BehaviorProfile.NORMAL)  // Set behavior speed
    
    // Network configuration
    .proxy("http://proxy.example.com:8080")   // Set HTTP proxy
    .userAgent("Custom User Agent")           // Custom user agent
    
    // Driver configuration
    .implicitWaitMs(5000)                     // Implicit wait timeout
    .pageLoadTimeoutMs(30000)                 // Page load timeout
    .requireLatestChrome(true)                // Use latest Chrome version
    
    // Advanced options
    .verboseLogging(false)                    // Disable verbose logging
    .disableImages(false)                     // Load images normally
    .mobileEmulation(false)                   // Desktop mode
    
    .build();

UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
```

## Usage Examples

### Basic Usage

```java
import com.undetected.chromedriver.UndetectedChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class BasicExample {
    public static void main(String[] args) {
        UndetectedChromeDriver driver = new UndetectedChromeDriver();
        
        try {
            // Navigate to website
            driver.get("https://example.com");
            
            // Find and interact with elements
            WebElement searchBox = driver.findElement(By.name("q"));
            searchBox.sendKeys("selenium automation");
            
            WebElement searchButton = driver.findElement(By.name("btnK"));
            searchButton.click();
            
            // Wait for results
            Thread.sleep(2000);
            
            // Get page title
            System.out.println("Page title: " + driver.getTitle());
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
```

### Human Behavior Simulation

```java
import com.undetected.chromedriver.UndetectedChromeDriver;
import com.undetected.chromedriver.config.DriverConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class HumanBehaviorExample {
    public static void main(String[] args) {
        // Enable human behavior simulation
        DriverConfig config = DriverConfig.builder()
            .humanBehavior(true)
            .behaviorProfile(BehaviorProfile.NORMAL)
            .build();
            
        UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
        
        try {
            driver.get("https://example.com/form");
            
            // Human-like typing with realistic delays and occasional typos
            WebElement nameField = driver.findElement(By.id("name"));
            driver.humanType(nameField, "John Doe");
            
            // Smooth scrolling to element
            WebElement emailField = driver.findElement(By.id("email"));
            driver.humanScrollToElement(emailField);
            driver.humanType(emailField, "john.doe@example.com");
            
            // Natural mouse movement and click
            WebElement submitButton = driver.findElement(By.id("submit"));
            driver.humanClick(submitButton);
            
            // Simulate user reading the page
            driver.performRandomActions();
            
        } finally {
            driver.quit();
        }
    }
}
```

### Bot Detection Handling

```java
import com.undetected.chromedriver.UndetectedChromeDriver;
import com.undetected.chromedriver.config.DriverConfig;

public class BotDetectionExample {
    public static void main(String[] args) {
        DriverConfig config = DriverConfig.builder()
            .humanBehavior(true)
            .randomizeFingerprint(true)
            .build();
            
        UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
        
        try {
            // Navigate to Cloudflare-protected site
            driver.get("https://protected-site.com");
            
            // Check if bot detection is active
            if (driver.getBotDetectionHandler().isBotDetectionActive()) {
                System.out.println("Bot detection detected! Handling...");
                
                // Wait for automatic resolution
                boolean resolved = driver.getBotDetectionHandler().handleBotDetection(60);
                
                if (resolved) {
                    System.out.println("Bot detection bypassed successfully!");
                } else {
                    System.out.println("Failed to bypass bot detection");
                }
            }
            
            // Check for rate limiting
            if (driver.getBotDetectionHandler().isRateLimited()) {
                int waitTime = driver.getBotDetectionHandler().getRecommendedWaitTime();
                System.out.println("Rate limited. Waiting " + waitTime + " seconds...");
                Thread.sleep(waitTime * 1000);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
```

### Advanced Configuration

```java
import com.undetected.chromedriver.UndetectedChromeDriver;
import com.undetected.chromedriver.config.DriverConfig;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AdvancedConfigExample {
    public static void main(String[] args) {
        // Custom Chrome arguments
        List<String> customArgs = Arrays.asList(
            "--disable-blink-features=AutomationControlled",
            "--disable-dev-shm-usage",
            "--no-sandbox",
            "--disable-web-security",
            "--disable-features=IsolateOrigins,site-per-process"
        );
        
        // Custom preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("download.default_directory", "/path/to/downloads");
        
        // Environment variables
        Map<String, String> env = new HashMap<>();
        env.put("DISPLAY", ":99");
        
        DriverConfig config = DriverConfig.builder()
            .headless(false)
            .windowSize(1366, 768)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .additionalArguments(customArgs)
            .additionalPreferences(prefs)
            .environmentVariables(env)
            .driverCachePath("/custom/cache/path")
            .logFile("/custom/logs/chromedriver.log")
            .pageLoadTimeoutMs(60000)
            .implicitWaitMs(10000)
            .disableGpu(true)
            .disableImages(false)
            .acceptInsecureCerts(true)
            .build();
            
        UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
        
        try {
            driver.get("https://example.com");
            // Your automation code here
        } finally {
            driver.quit();
        }
    }
}
```

### Concurrent Usage

```java
import com.undetected.chromedriver.UndetectedChromeDriver;
import com.undetected.chromedriver.config.DriverConfig;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentExample {
    public static void main(String[] args) {
        int numberOfInstances = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfInstances);
        List<Future<?>> futures = new ArrayList<>();
        
        // Create multiple driver instances concurrently
        for (int i = 0; i < numberOfInstances; i++) {
            final int instanceNum = i;
            
            Future<?> future = executor.submit(() -> {
                DriverConfig config = DriverConfig.builder()
                    .humanBehavior(true)
                    .randomizeFingerprint(true)
                    .build();
                    
                UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
                
                try {
                    System.out.println("Instance " + instanceNum + " starting...");
                    driver.get("https://example.com/page" + instanceNum);
                    
                    // Perform automation tasks
                    Thread.sleep(5000);
                    
                    System.out.println("Instance " + instanceNum + " completed: " + 
                                     driver.getTitle());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    driver.quit();
                }
            });
            
            futures.add(future);
        }
        
        // Wait for all instances to complete
        futures.forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        executor.shutdown();
    }
}
```

### Custom Behavior Profiles

```java
import com.undetected.chromedriver.UndetectedChromeDriver;
import com.undetected.chromedriver.config.DriverConfig;
import com.undetected.chromedriver.HumanBehaviorSimulator.BehaviorProfile;

public class BehaviorProfileExample {
    public static void main(String[] args) {
        // Fast profile - for quick automation
        testWithProfile(BehaviorProfile.FAST, "Fast automation");
        
        // Normal profile - balanced speed and realism
        testWithProfile(BehaviorProfile.NORMAL, "Normal automation");
        
        // Careful profile - maximum realism, slower
        testWithProfile(BehaviorProfile.CAREFUL, "Careful automation");
    }
    
    private static void testWithProfile(BehaviorProfile profile, String description) {
        DriverConfig config = DriverConfig.builder()
            .humanBehavior(true)
            .behaviorProfile(profile)
            .build();
            
        UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
        
        try {
            System.out.println("Testing " + description);
            long startTime = System.currentTimeMillis();
            
            driver.get("https://example.com");
            
            WebElement searchBox = driver.findElement(By.name("q"));
            driver.humanType(searchBox, "test query");
            
            WebElement button = driver.findElement(By.name("search"));
            driver.humanClick(button);
            
            long endTime = System.currentTimeMillis();
            System.out.println("Completed in: " + (endTime - startTime) + "ms");
            
        } finally {
            driver.quit();
        }
    }
}
```

### Fingerprint Randomization

```java
import com.undetected.chromedriver.UndetectedChromeDriver;
import com.undetected.chromedriver.config.DriverConfig;

public class FingerprintExample {
    public static void main(String[] args) throws InterruptedException {
        DriverConfig config = DriverConfig.builder()
            .randomizeFingerprint(true)  // Enable fingerprint randomization
            .build();
            
        UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
        
        try {
            // Navigate to fingerprint testing site
            driver.get("https://browserleaks.com/canvas");
            Thread.sleep(3000);
            
            // Take screenshot of first fingerprint
            System.out.println("First fingerprint captured");
            
            // Wait for fingerprint to change (randomization occurs periodically)
            Thread.sleep(65000);  // Wait just over 1 minute
            
            // Refresh to see new fingerprint
            driver.navigate().refresh();
            Thread.sleep(3000);
            
            System.out.println("Fingerprint should be different now");
            
        } finally {
            driver.quit();
        }
    }
}
```

### Proxy Configuration

```java
import com.undetected.chromedriver.UndetectedChromeDriver;
import com.undetected.chromedriver.config.DriverConfig;

public class ProxyExample {
    public static void main(String[] args) {
        // HTTP proxy
        DriverConfig httpProxyConfig = DriverConfig.builder()
            .proxy("http://proxy.example.com:8080")
            .build();
            
        // SOCKS proxy
        DriverConfig socksProxyConfig = DriverConfig.builder()
            .proxy("socks5://proxy.example.com:1080")
            .build();
            
        // Authenticated proxy
        DriverConfig authProxyConfig = DriverConfig.builder()
            .proxy("http://username:password@proxy.example.com:8080")
            .build();
            
        UndetectedChromeDriver driver = new UndetectedChromeDriver(httpProxyConfig);
        
        try {
            driver.get("https://whatismyipaddress.com");
            Thread.sleep(5000);
            // IP should show proxy's IP address
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
```

### Headless Mode

```java
import com.undetected.chromedriver.UndetectedChromeDriver;
import com.undetected.chromedriver.config.DriverConfig;
import org.openqa.selenium.OutputType;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HeadlessExample {
    public static void main(String[] args) {
        DriverConfig config = DriverConfig.builder()
            .headless(true)              // Enable headless mode
            .windowSize(1920, 1080)      // Set virtual window size
            .randomizeFingerprint(true)  // Still randomize fingerprints
            .build();
            
        UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
        
        try {
            driver.get("https://example.com");
            
            // Take screenshot in headless mode
            File screenshot = driver.getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), 
                      Paths.get("headless-screenshot.png"));
            
            System.out.println("Screenshot saved!");
            System.out.println("Page title: " + driver.getTitle());
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
```

### Mobile Emulation

```java
import com.undetected.chromedriver.UndetectedChromeDriver;
import com.undetected.chromedriver.config.DriverConfig;

public class MobileEmulationExample {
    public static void main(String[] args) {
        DriverConfig config = DriverConfig.builder()
            .mobileEmulation(true)       // Enable mobile emulation
            .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)")
            .windowSize(375, 812)        // iPhone X dimensions
            .build();
            
        UndetectedChromeDriver driver = new UndetectedChromeDriver(config);
        
        try {
            driver.get("https://m.example.com");
            
            // Site should display mobile version
            System.out.println("Mobile site loaded: " + driver.getTitle());
            
            // Interact with mobile-specific elements
            WebElement mobileMenu = driver.findElement(By.className("mobile-menu"));
            driver.humanClick(mobileMenu);
            
        } finally {
            driver.quit();
        }
    }
}
```

## API Reference

### UndetectedChromeDriver Methods

| Method | Description |
|--------|-------------|
| `humanClick(WebElement element)` | Performs human-like click with mouse movement |
| `humanType(WebElement element, String text)` | Types text with realistic delays and typos |
| `humanScroll(int targetY)` | Smooth scrolling to Y coordinate |
| `humanScrollToElement(WebElement element)` | Scrolls element into view naturally |
| `performRandomActions()` | Simulates idle user behavior |
| `getBotDetectionHandler()` | Returns the bot detection handler |
| `getHumanBehavior()` | Returns the human behavior simulator |
| `getInstanceId()` | Returns unique instance identifier |
| `isActive()` | Checks if driver is still active |

### DriverConfig Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `headless` | boolean | false | Run in headless mode |
| `windowSize` | int, int | 1200x800 | Browser window dimensions |
| `userAgent` | String | Random | Custom user agent string |
| `proxy` | String | null | Proxy server URL |
| `humanBehavior` | boolean | false | Enable human behavior simulation |
| `randomizeFingerprint` | boolean | false | Enable fingerprint randomization |
| `behaviorProfile` | BehaviorProfile | NORMAL | Speed of human behavior |
| `implicitWaitMs` | int | 3000 | Implicit wait timeout |
| `pageLoadTimeoutMs` | int | 30000 | Page load timeout |
| `disableImages` | boolean | false | Disable image loading |
| `verboseLogging` | boolean | false | Enable verbose logging |

## Best Practices

### 1. Resource Management
Always use try-finally or try-with-resources to ensure proper cleanup:
```java
try (UndetectedChromeDriver driver = new UndetectedChromeDriver()) {
    // Your automation code
} // Driver automatically quits
```

### 2. Human Behavior
Enable human behavior for sites with advanced bot detection:
```java
DriverConfig config = DriverConfig.builder()
    .humanBehavior(true)
    .behaviorProfile(BehaviorProfile.NORMAL)
    .build();
```

### 3. Rate Limiting
Always check for rate limiting before making requests:
```java
if (driver.getBotDetectionHandler().isRateLimited()) {
    int waitTime = driver.getBotDetectionHandler().getRecommendedWaitTime();
    Thread.sleep(waitTime * 1000);
}
```

### 4. Fingerprint Diversity
Enable fingerprint randomization for long-running sessions:
```java
DriverConfig config = DriverConfig.builder()
    .randomizeFingerprint(true)
    .build();
```

### 5. Error Handling
Implement proper error handling for bot detection scenarios:
```java
try {
    driver.get(url);
    if (driver.getBotDetectionHandler().isBotDetectionActive()) {
        if (!driver.getBotDetectionHandler().handleBotDetection()) {
            // Implement fallback strategy
        }
    }
} catch (Exception e) {
    // Handle errors appropriately
}
```

## Troubleshooting

### Common Issues

#### 1. ChromeDriver Not Found
**Problem**: `ChromeDriver not found at: /path/to/driver`

**Solution**:
- Ensure Chrome browser is installed
- Clear WebDriverManager cache: `~/.cache/selenium`
- Enable force download: `.forceDriverDownload(true)`

#### 2. Bot Detection Not Bypassed
**Problem**: Still detected by anti-bot systems

**Solution**:
- Enable all anti-detection features
- Use CAREFUL behavior profile
- Add delays between actions
- Rotate proxies/user agents

#### 3. Performance Issues
**Problem**: Automation runs slowly

**Solution**:
- Use FAST behavior profile
- Disable image loading
- Reduce implicit wait times
- Use headless mode when possible

#### 4. Memory Leaks
**Problem**: Memory usage increases over time

**Solution**:
- Always call `driver.quit()`
- Limit concurrent instances
- Monitor thread pool usage
- Implement periodic restarts

### Debug Logging

Enable verbose logging for troubleshooting:
```java
DriverConfig config = DriverConfig.builder()
    .verboseLogging(true)
    .logFile("/path/to/debug.log")
    .build();
```

## Performance Considerations

### Memory Usage
- Each driver instance uses ~100-200MB RAM
- Fingerprint randomization adds ~10MB overhead
- Human behavior simulation has minimal impact

### CPU Usage
- Stealth JavaScript execution: <1% CPU
- Human behavior simulation: 2-5% CPU during actions
- Fingerprint randomization: 1-2% CPU periodically

### Network Impact
- No additional network requests
- Proxy usage may add latency
- Rate limiting checks are passive

### Optimization Tips
1. Use headless mode when UI not required
2. Disable images for faster loading
3. Adjust timeouts based on network speed
4. Use appropriate behavior profiles
5. Limit concurrent instances based on resources

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup
```bash
# Clone repository
git clone https://github.com/yourusername/undetected-chromedriver.git

# Build project
mvn clean install

# Run tests
mvn test
```

### Code Style
- Follow Java naming conventions
- Add JavaDoc for public methods
- Include unit tests for new features
- Ensure thread safety

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [ultrafunkamsterdam/undetected-chromedriver](https://github.com/ultrafunkamsterdam/undetected-chromedriver) - The original Python implementation that inspired this Java port
- Selenium WebDriver team for the excellent automation framework
- WebDriverManager for automatic driver management
- The open-source community for inspiration and contributions

## Support

For issues, questions, or contributions:
- GitHub Issues: [Report a bug](https://github.com/yourusername/undetected-chromedriver/issues)
- Documentation: [Wiki](https://github.com/yourusername/undetected-chromedriver/wiki)
- Email: support@undetected-chromedriver.com

---

**Note**: This library is for educational and testing purposes. Always respect website terms of service and use responsibly.