package com.framework.pages;

import com.framework.base.BasePage;
import org.openqa.selenium.By;

public class LoginPage extends BasePage {

    private final By username = By.id("username");
    private final By password = By.id("password");
    private final By Orangeuser = By.name("username");
    private final By Orangepass = By.name("password");
    private final By loginBtn = By.cssSelector("button[type='submit']");
    private final By flashMsg = By.id("flash");
    private final By HomepageTitle = By.xpath("//h6[text()='Dashboard']");

    public LoginPage login(String user, String pass) {
        type(Orangeuser, user);
        type(Orangepass, pass);
        click(loginBtn);
        return this;
    }

    public LoginPage Orangehrmlogin(String user, String pass) {
    	click(Orangeuser);
        type(Orangeuser, user);
        click(Orangepass);
        type(Orangepass, pass);
        click(loginBtn);
        return this;
    }
    public String getFlashMessage() {
        return getText(flashMsg);
    }
    
    public String getHomepageTitle() {
        return getText(HomepageTitle);
    }
}
