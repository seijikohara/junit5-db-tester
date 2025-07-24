package example;

import com.github.seijikohara.junit5dbtester.annotation.DataSet;
import com.github.seijikohara.junit5dbtester.annotation.Expectation;
import com.github.seijikohara.junit5dbtester.annotation.Preparation;
import com.github.seijikohara.junit5dbtester.context.DatabaseTesterContext;
import com.github.seijikohara.junit5dbtester.extension.DatabaseTestExtension;
import com.github.seijikohara.junit5dbtester.operation.Operation;
import com.mysql.cj.jdbc.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Convention-based test case demonstrating framework core functionality. Based on the original
 * simple-db-tester XlsTestCase.java.
 */
@Testcontainers
@ExtendWith(DatabaseTestExtension.class)
final class ConventionBasedTest {

  private static final Logger logger = LoggerFactory.getLogger(ConventionBasedTest.class);

  @Container
  @SuppressWarnings("resource") // Managed by Testcontainers framework
  static final MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName("testdb")
          .withUsername("testuser")
          .withPassword("testpass");

  @Container
  @SuppressWarnings("resource") // Managed by Testcontainers framework
  static final MySQLContainer<?> mysql2 =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName("testdb2")
          .withUsername("testuser2")
          .withPassword("testpass2");

  @BeforeAll
  static void setupDatabase() throws Exception {
    logger.info("Setting up convention-based test with multiple data sources");

    // Setup first database
    final var dataSource = createDataSource(mysql);
    DatabaseTesterContext.getInstance().registerDataSource("dataSource", dataSource);
    executeScript(dataSource, "ddl/ddl-datasource.sql");

    // Setup second database
    final var dataSource2 = createDataSource(mysql2);
    DatabaseTesterContext.getInstance().registerDataSource("dataSource2", dataSource2);
    executeScript(dataSource2, "ddl/ddl-datasource2.sql");

    logger.info("Multiple database setup completed");
  }

  private static DataSource createDataSource(final MySQLContainer<?> container) {
    try {
      return new SimpleDriverDataSource(
          new Driver(), container.getJdbcUrl(), container.getUsername(), container.getPassword());
    } catch (final SQLException e) {
      throw new RuntimeException("Failed to create data source", e);
    }
  }

  private static void executeScript(final DataSource dataSource, final String scriptPath)
      throws Exception {
    final var resource = ConventionBasedTest.class.getClassLoader().getResource(scriptPath);
    if (resource == null) {
      throw new IllegalStateException("Script not found: " + scriptPath);
    }

    try (final var connection = dataSource.getConnection();
        final var statement = connection.createStatement();
        final var inputStream = resource.openStream()) {
      final var sql = new String(inputStream.readAllBytes());
      Arrays.stream(sql.split(";"))
          .map(String::trim)
          .filter(trimmed -> !trimmed.isEmpty())
          .forEach(
              trimmed -> {
                try {
                  statement.execute(trimmed);
                } catch (final SQLException e) {
                  throw new RuntimeException("Failed to execute SQL: " + trimmed, e);
                }
              });
    }
  }

  /**
   * Tests convention-based data loading with pattern1 scenario.
   *
   * <p>This test demonstrates the framework's core convention-based functionality:
   *
   * <ul>
   *   <li>Loads preparation data from ConventionBasedTest.xlsx with pattern "pattern1"
   *   <li>Executes the test method
   *   <li>Validates results against ConventionBasedTest-expected.xlsx with pattern "pattern1"
   * </ul>
   *
   * <p>Uses default data source and convention-based file resolution.
   */
  @Test
  @Preparation
  @Expectation
  void pattern1() {
    logger.info("Executing pattern1 test - validating basic convention-based data flow");
  }

  /**
   * Tests convention-based data loading with pattern2 scenario.
   *
   * <p>This test demonstrates pattern-based data filtering within the same Excel file:
   *
   * <ul>
   *   <li>Loads preparation data from ConventionBasedTest.xlsx with pattern "pattern2"
   *   <li>Executes the test method with different data set than pattern1
   *   <li>Validates results against ConventionBasedTest-expected.xlsx with pattern "pattern2"
   * </ul>
   *
   * <p>Shows how multiple test scenarios can coexist in the same Excel file using pattern markers.
   */
  @Test
  @Preparation
  @Expectation
  void pattern2() {
    logger.info("Executing pattern2 test - demonstrating pattern-based data segregation");
  }

  /**
   * Tests multi-datasource functionality with custom Excel file locations and multiple patterns.
   *
   * <p>This test demonstrates advanced framework capabilities:
   *
   * <ul>
   *   <li>Uses named datasource "dataSource2" instead of default
   *   <li>Loads data from custom file location (not convention-based naming)
   *   <li>Combines multiple patterns ("customPattern1" and "customPattern2") in one operation
   *   <li>Performs CLEAN_INSERT operation for complete data refresh
   *   <li>Validates against separate expectation file for dataSource2
   * </ul>
   *
   * <p>Shows framework flexibility beyond convention-based defaults.
   */
  @Test
  @Preparation(
      dataSets =
          @DataSet(
              patternNames = {"customPattern1", "customPattern2"},
              dataSourceName = "dataSource2",
              resourceLocation =
                  "classpath:example/custom-file/ConventionBasedTest-dataSource2.xlsx"),
      operation = Operation.CLEAN_INSERT)
  @Expectation(
      dataSets =
          @DataSet(
              dataSourceName = "dataSource2",
              resourceLocation =
                  "classpath:example/custom-file/ConventionBasedTest-dataSource2-expected.xlsx"))
  void dataSource2() {
    logger.info(
        "Executing dataSource2 test - demonstrating multi-datasource and custom file handling");
  }
}
