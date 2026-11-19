package com.framework.tests;

import com.framework.api.ApiClient;
import com.framework.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * HYBRID pattern: use the fast API to prepare/validate data, then verify in the UI.
 * Real projects: create a user/order via API, then log in and check it on screen.
 */
public class HybridTest extends UiBaseTest {

    @Test
    public void apiFetchesUser_thenUiLogin() {
        // 1) API step
        String name = ApiClient.get("/users/1")
                .then().statusCode(200)
                .extract().path("name");
        Assert.assertNotNull(name, "API returned no user name");

        // 2) UI step
        String msg = new LoginPage().login("tomsmith", "SuperSecretPassword!").getFlashMessage();
        Assert.assertTrue(msg.contains("You logged into a secure area!"));
    }
}
