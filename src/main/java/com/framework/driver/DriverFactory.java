package com.framework.driver;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.Map;

/** Creates browsers. ThreadLocal makes parallel execution safe. */
public final class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() { }

    public static void init(String browser, boolean headless) {
        if (browser == null || browser.isBlank()) {
            throw new IllegalArgumentException("Browser name is empty. Check the 'browser' config/property.");
        }
        // Never leak a browser if init() is called twice on the same thread.
        if (DRIVER.get() != null) {
            quit();
        }

        WebDriver driver;
        switch (browser.trim().toLowerCase()) {
            case "firefox" -> {
                FirefoxOptions o = new FirefoxOptions();
                if (headless) o.addArguments("-headless");
                driver = new FirefoxDriver(o);
            }
            case "edge" -> {
                EdgeOptions o = new EdgeOptions();
                if (headless) o.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
                o.addArguments("--disable-notifications");
                driver = new EdgeDriver(o);
            }
            case "chrome" -> {
                ChromeOptions o = new ChromeOptions();
                if (headless) o.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
                o.addArguments("--window-size=1920,1080", "--disable-notifications");
                // Stops the "save password" / "password breach" popups that break login tests.
                o.setExperimentalOption("prefs", Map.of(
                        "credentials_enable_service", false,
                        "profile.password_manager_enabled", false));
                driver = new ChromeDriver(o);
            }
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        }

        // maximize() does nothing in headless mode, so set an explicit size there.
        if (headless) {
            driver.manage().window().setSize(new Dimension(1920, 1080));
        } else {
            driver.manage().window().maximize();
        }
        DRIVER.set(driver);
    }

    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    public static void quit() {
        WebDriver d = DRIVER.get();
        if (d != null) {
            try {
                d.quit();
            } finally {
                DRIVER.remove();   // always clear, even if quit() throws
            }
        }
    }
}