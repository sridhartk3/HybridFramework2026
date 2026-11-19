# Hybrid Test Automation Framework

Selenium + TestNG + RestAssured, Java 17, Maven. Excel-driven data, Log4j2 logging, runs locally or in GitHub Actions.

## Run locally
```
mvn clean test
mvn clean test -Dbrowser=firefox -Dheadless=true -DsuiteFile=testng.xml
```

## Run in the cloud
Actions tab > "Test Automation" > Run workflow (pick browser and suite). It also runs on every push / pull request to `main`, and nightly.
Reports and logs: open the run > Artifacts.

## Configuration
Values come from `src/test/resources/config.properties`; `-D` system properties override them
(`browser`, `headless`, `timeout.seconds`, ...). Secrets go in GitHub: Settings > Secrets and variables > Actions.

## Structure
```
src/test/java/com/framework/
  base/      BasePage (waits, dynamic locators)
  driver/    DriverFactory (ThreadLocal)
  config/    ConfigReader
  utils/     DataProviders, DataReader, DataSource
  pages/     page objects
  tests/     test classes
src/test/resources/  config.properties, log4j2.xml, testdata/
```

## Conventions
- Page objects hold locators and actions; assertions stay in tests.
- File and sheet names are case-sensitive on Linux (CI): match the exact case.
- Never commit passwords; use GitHub Secrets.
