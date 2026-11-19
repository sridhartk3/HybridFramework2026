package com.framework.tests;

import com.framework.config.ConfigReader;
import com.framework.driver.DriverFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/** Extend this for any test that needs a browser. API-only tests skip it. */
public abstract class UiBaseTest {

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DriverFactory.init(ConfigReader.get("browser"), ConfigReader.getBoolean("headless"));
        DriverFactory.getDriver().get(ConfigReader.get("ui.Orangehrm.url") + "/login");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverFactory.quit();
    }
}
