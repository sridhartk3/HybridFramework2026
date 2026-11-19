package com.framework.base;

import com.framework.config.ConfigReader;
import com.framework.driver.DriverFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Simple, wait-safe actions. Every page class extends this. Keep page-specific logic out of here. */
public abstract class BasePage {

    private static final int SHORT_WAIT_SECONDS = 3;

    protected final Logger log = LogManager.getLogger(getClass());
    protected final WebDriver driver;
    protected final WebDriverWait wait;
    private final int timeoutSeconds;

    protected BasePage() {
        driver = DriverFactory.getDriver();
        if (driver == null) {
            throw new IllegalStateException("No WebDriver on this thread. Call DriverFactory.init() "
                    + "before creating a page object, and create pages inside test methods.");
        }
        timeoutSeconds = ConfigReader.getInt("timeout.seconds");
        wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        // Transient problems are retried until the timeout instead of failing at once.
        wait.ignoreAll(List.of(StaleElementReferenceException.class,
                ElementClickInterceptedException.class,
                ElementNotInteractableException.class));
    }

    // ===== Locators for dynamic elements =====

    /**
     * Builds a locator from a template + runtime values. XPath or CSS is detected automatically
     * (starts with "/", "./" or "(" = XPath, otherwise CSS). Use %% for a literal percent sign.
     * <pre>
     * click(dynamic("//nav//a[normalize-space()='%s']", "Orders"));
     * getText(dynamic("//tr[td[text()='%s']]/td[%d]", "John", 3));
     * click(dynamic("button[data-id='%s']", id));
     * </pre>
     * Tip: keep templates as constants at the top of each page class.
     */
    protected static By dynamic(String template, Object... args) {
        String locator;
        try {
            locator = (args == null || args.length == 0) ? template : String.format(Locale.ROOT, template, args);
        } catch (IllegalFormatException e) {
            throw new IllegalArgumentException("Bad locator template '" + template + "' with args "
                    + Arrays.toString(args) + ": " + e.getMessage(), e);
        }
        String t = locator.trim();
        boolean xpath = t.startsWith("/") || t.startsWith("./") || t.startsWith("(");
        return xpath ? By.xpath(t) : By.cssSelector(t);
    }

    /** Any element showing exactly this text (any tag). Safe for text containing ' or ". */
    protected static By byText(String text) {
        return By.xpath("//*[text()[normalize-space()=" + xpathLiteral(text) + "]]");
    }

    protected static String xpathLiteral(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        return "concat('" + value.replace("'", "', \"'\", '") + "')";
    }

    // ===== Actions =====

    protected void click(By locator) {
        log.debug("click {}", locator);
        act("click", locator, ExpectedConditions.elementToBeClickable(locator), el -> {
            try {
                el.click();
            } catch (ElementClickInterceptedException e) {
                scrollToCenter(el);   // then the wait retries the click
                throw e;
            }
        });
    }

    protected void type(By locator, String text) {
        log.debug("type into {}", locator);   // value is never logged (may be a password)
        act("type into", locator, ExpectedConditions.visibilityOfElementLocated(locator), el -> {
            el.clear();
            if (text != null && !text.isEmpty()) {
                el.sendKeys(text);
            }
        });
    }

    protected String getText(By locator) {
        return read("read text of", locator,
                ExpectedConditions.visibilityOfElementLocated(locator), WebElement::getText);
    }

    protected String getAttribute(By locator, String name) {
        return read("read attribute of", locator,
                ExpectedConditions.presenceOfElementLocated(locator), el -> el.getAttribute(name));
    }

    /** Text of all matching elements (waits for at least one). */
    protected List<String> getTexts(By locator) {
        Function<WebDriver, List<String>> condition = d -> {
            List<WebElement> els = d.findElements(locator);
            return els.isEmpty() ? null : els.stream().map(WebElement::getText).collect(Collectors.toList());
        };
        try {
            return wait.until(condition);
        } catch (TimeoutException e) {
            throw new TimeoutException("No elements found for " + locator + " within " + timeoutSeconds + "s", e);
        }
    }

    protected void selectByText(By locator, String text) {
        act("select option in", locator, ExpectedConditions.visibilityOfElementLocated(locator),
                el -> new Select(el).selectByVisibleText(text));
    }

    // ===== Checks and waits =====

    /** Short wait, so "should NOT be shown" checks don't burn the full timeout. */
    protected boolean isDisplayed(By locator) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_SECONDS))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
        } catch (TimeoutException | NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    protected void waitForInvisible(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void waitForPageLoad() {
        Function<WebDriver, Boolean> loaded = d ->
                "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState"));
        wait.until(loaded);
    }

    // ===== Internals: one retry engine behind every action =====

    /** Waits for the element, runs op on it, retries the whole step on stale/intercepted/not-interactable. */
    private <T> T read(String what, By locator, Function<WebDriver, WebElement> ready,
                       Function<WebElement, T> op) {
        // Optional lets a legitimate null result (e.g. missing attribute) count as success.
        Function<WebDriver, Optional<T>> condition = d -> {
            WebElement el = ready.apply(d);
            return el == null ? null : Optional.ofNullable(op.apply(el));
        };
        try {
            return wait.until(condition).orElse(null);
        } catch (TimeoutException e) {
            throw new TimeoutException("Could not " + what + " " + locator + " within " + timeoutSeconds + "s", e);
        }
    }

    private void act(String what, By locator, Function<WebDriver, WebElement> ready, Consumer<WebElement> op) {
        read(what, locator, ready, el -> {
            op.accept(el);
            return Boolean.TRUE;
        });
    }

    private void scrollToCenter(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }
}