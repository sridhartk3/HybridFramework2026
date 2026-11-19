package com.framework.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.framework.driver.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/** HTML report (target/extent-report.html) + screenshot on UI failure. */
public class TestListener implements ITestListener, ISuiteListener {

    private static final ExtentReports EXTENT = new ExtentReports();
    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();

    @Override
    public void onStart(ISuite suite) {
        EXTENT.attachReporter(new ExtentSparkReporter("target/extent-report.html"));
    }

    @Override
    public void onFinish(ISuite suite) {
        EXTENT.flush();
    }

    @Override
    public void onTestStart(ITestResult r) {
        TEST.set(EXTENT.createTest(r.getMethod().getMethodName()));
    }

    @Override
    public void onTestSuccess(ITestResult r) {
        TEST.get().pass("Passed");
    }

    @Override
    public void onTestFailure(ITestResult r) {
        TEST.get().fail(r.getThrowable());
        if (DriverFactory.getDriver() != null) {   // null for pure API tests
            String img = ((TakesScreenshot) DriverFactory.getDriver())
                    .getScreenshotAs(OutputType.BASE64);
            TEST.get().addScreenCaptureFromBase64String(img, "Failure screenshot");
        }
    }

    @Override
    public void onTestSkipped(ITestResult r) {
        TEST.get().skip("Skipped / retried");
    }
}
