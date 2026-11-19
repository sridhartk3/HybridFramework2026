package com.framework.utils;

import com.framework.config.ConfigReader;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/** Re-runs a failed test (count from config: retry.count). */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int attempt = 0;

    @Override
    public boolean retry(ITestResult result) {
        return attempt++ < ConfigReader.getInt("retry.count");
    }
}
