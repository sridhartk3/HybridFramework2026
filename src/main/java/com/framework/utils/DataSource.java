package com.framework.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.testng.annotations.DataProvider;

/**
 * OPTIONAL. Put on a test to choose its own data source.
 * Leave everything out and the framework uses testdata.path + sheet = method name.
 *
 *   @DataSource(file = "src/test/resources/testdata/users.csv")
 *   @DataSource(file = "https://docs.google.com/spreadsheets/d/ID/export?format=csv")
 *   @DataSource(file = "data.xlsx", sheet = "MySheet")
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DataSource {
    String file() default "";     // local path or http(s) URL
    String sheet() default "";    // Excel sheet / JSON key (default = test method name)
    String format() default ""; // excel | csv | json  (default = detected from file name)

    
}

