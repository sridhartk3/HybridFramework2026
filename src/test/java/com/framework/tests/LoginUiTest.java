package com.framework.tests;

import com.framework.pages.LoginPage;
import com.framework.utils.DataProviders;
import com.framework.utils.DataSource;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class LoginUiTest extends UiBaseTest {

  //  @DataProvider(name = "loginData")
//    public Object[][] loginData() {
//        return new Object[][] {
//            {"tomsmith", "SuperSecretPassword!", "You logged into a secure area!"},
//            {"Admin","admin123","Dashboard"},
//            {"tomsmith", "wrong",                "Your password is invalid!"},
//            {"nobody",   "wrong",                "Your username is invalid!"}
//        };
//    }

    @Test(dataProvider = "data", dataProviderClass = DataProviders.class)
    @DataSource(file = "src/test/resources/testdata/testdata.xlsx", sheet = "verifyLogin")
	public void verifyLogin(Map<String, String> row) {
    	String user = row.get("username");
        String pass = row.get("password");
        String expectedMsg = row.get("expectedMessage");
        String actual = new LoginPage().login(user, pass).getHomepageTitle();
        Assert.assertTrue(actual.contains(expectedMsg),
                "Expected [" + expectedMsg + "] but got [" + actual + "]");
    }
}
