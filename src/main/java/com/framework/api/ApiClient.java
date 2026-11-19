package com.framework.api;

import com.framework.config.ConfigReader;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/** One place for base URI, headers and logging. Tests only call get/post/put/delete. */
public final class ApiClient {

    private ApiClient() { }

    private static RequestSpecification spec() {
        return new RequestSpecBuilder()
                .setBaseUri(ConfigReader.get("api.base.url"))
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                // .addHeader("Authorization", "Bearer " + token)  <- add auth here once
                .build();
    }

    public static Response get(String path) {
        return given().spec(spec()).log().ifValidationFails().when().get(path);
    }

    public static Response post(String path, Object body) {
        return given().spec(spec()).body(body).log().ifValidationFails().when().post(path);
    }

    public static Response put(String path, Object body) {
        return given().spec(spec()).body(body).log().ifValidationFails().when().put(path);
    }

    public static Response delete(String path) {
        return given().spec(spec()).log().ifValidationFails().when().delete(path);
    }
}
