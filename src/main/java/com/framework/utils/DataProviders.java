package com.framework.utils;

import com.framework.config.ConfigReader;
import org.testng.annotations.DataProvider;

import java.lang.reflect.Method;

/**
 * The only provider you ever need:
 *   @Test(dataProvider = "data", dataProviderClass = DataProviders.class)
 * Source  = @DataSource(file) if present, else testdata.path from config.
 * Sheet   = @DataSource(sheet) if present, else the test method name.
 * Rows with run = N are skipped.
 */
public class DataProviders {

    @DataProvider(name = "data")
    public static Object[][] data(Method testMethod) {
        DataSource ds = testMethod.getAnnotation(DataSource.class);

        String file   = (ds != null && !ds.file().isEmpty())  ? ds.file()  : ConfigReader.get("testdata.path");
        String sheet  = (ds != null && !ds.sheet().isEmpty()) ? ds.sheet() : testMethod.getName();
        String format = (ds != null) ? ds.format() : "";

        return DataReader.read(file, sheet, format).stream()
                .filter(row -> !"N".equalsIgnoreCase(row.getOrDefault("run", "Y")))
                .map(row -> new Object[] { row })
                .toArray(Object[][]::new);
    }
}
