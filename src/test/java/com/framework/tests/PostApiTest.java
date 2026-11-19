package com.framework.tests;

import com.framework.api.ApiClient;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class PostApiTest {

    @Test
    public void getPost_returns200() {
        ApiClient.get("/posts/1")
                .then().statusCode(200)
                .body("id", equalTo(1))
                .body("title", notNullValue());
    }

    @Test
    public void createPost_returns201() {
        Map<String, Object> body = Map.of("title", "hello", "body", "world", "userId", 1);
        Response res = ApiClient.post("/posts", body);
        res.then().statusCode(201).body("title", equalTo("hello"));
    }

    @Test
    public void getUnknownPost_returns404() {
        ApiClient.get("/posts/99999").then().statusCode(404);
    }
}
