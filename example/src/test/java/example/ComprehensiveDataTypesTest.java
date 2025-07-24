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
 * Comprehensive test demonstrating framework support for various standard SQL data types.
 *
 * <p>This test class validates the framework's core functionality: Excel-based data insertion and
 * verification. It focuses on testing how the framework handles a wide range of SQL data types that
 * can be represented in Excel format, without concerning database constraints or relationships.
 *
 * <p><strong>Framework's Core Purpose:</strong>
 *
 * <ul>
 *   <li>Load test data from Excel files into database tables
 *   <li>Verify database state against expected Excel data
 *   <li>Handle various SQL data types correctly during Excel ↔ Database conversion
 * </ul>
 *
 * <p><strong>Excel File Structure:</strong>
 *
 * <ul>
 *   <li>Sheet names correspond to table names (COMPREHENSIVE_DATA_TYPES sheet)
 *   <li>[Pattern] column groups data for specific test methods
 *   <li>Test method names automatically match pattern values (convention-based)
 * </ul>
 *
 * <p><strong>Tested Data Types:</strong>
 *
 * <ul>
 *   <li>Integer types (TINYINT, SMALLINT, INT, BIGINT)
 *   <li>Decimal/Numeric types (DECIMAL, NUMERIC with various precision)
 *   <li>Floating point types (REAL, FLOAT, DOUBLE)
 *   <li>Character types (CHAR, VARCHAR, TEXT)
 *   <li>Date and time types (DATE, TIME, DATETIME, TIMESTAMP)
 *   <li>Boolean types
 *   <li>NULL value handling
 *   <li>Default value handling
 * </ul>
 */
@Testcontainers
@ExtendWith(DatabaseTestExtension.class)
final class ComprehensiveDataTypesTest {

  private static final Logger logger = LoggerFactory.getLogger(ComprehensiveDataTypesTest.class);

  @Container
  @SuppressWarnings("resource") // Managed by Testcontainers framework
  static final MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName("testdb")
          .withUsername("testuser")
          .withPassword("testpass");

  @BeforeAll
  static void setupDatabase() throws Exception {
    logger.info("Setting up comprehensive data types test database");

    final var dataSource = createDataSource(mysql);
    DatabaseTesterContext.getInstance().registerDataSource("dataSource", dataSource);
    executeScript(dataSource, "ddl/ddl-comprehensive.sql");

    logger.info("Comprehensive database setup completed");
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
    final var resource = ComprehensiveDataTypesTest.class.getClassLoader().getResource(scriptPath);
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
   * Tests integer data types with various precision ranges.
   *
   * <p>This test validates the framework's handling of integer data types including:
   *
   * <ul>
   *   <li>TINYINT (-128 to 127)
   *   <li>SMALLINT (-32,768 to 32,767)
   *   <li>INT/INTEGER (-2,147,483,648 to 2,147,483,647)
   *   <li>BIGINT (large 64-bit integers)
   * </ul>
   *
   * <p>Demonstrates Excel-to-database conversion accuracy for integer boundary values and ensures
   * proper data type mapping during insertion and validation phases.
   */
  @Test
  @Preparation
  @Expectation
  void testIntegerDataTypes() {
    logger.info(
        "Validating integer data types (TINYINT, SMALLINT, INT, BIGINT) - testing precision ranges and boundary values");
    // Framework automatically loads rows where [Pattern] = 'testIntegerDataTypes'
  }

  /**
   * Tests decimal and numeric data types with different precision and scale configurations.
   *
   * <p>This test validates precise decimal handling including:
   *
   * <ul>
   *   <li>DECIMAL(10,2) - 10 total digits with 2 decimal places
   *   <li>NUMERIC(8,3) - 8 total digits with 3 decimal places
   *   <li>DECIMAL(15,4) - high precision with 4 decimal places
   * </ul>
   *
   * <p>Ensures Excel decimal values are accurately preserved during database operations without
   * precision loss or rounding errors.
   */
  @Test
  @Preparation
  @Expectation
  void testDecimalDataTypes() {
    logger.info(
        "Validating decimal and numeric data types - testing precision, scale, and floating-point accuracy");
    // Framework automatically loads rows where [Pattern] = 'testDecimalDataTypes'
  }

  /**
   * Tests floating point data types with various precision levels.
   *
   * <p>This test validates floating-point number handling including:
   *
   * <ul>
   *   <li>REAL - single precision floating point (32-bit)
   *   <li>FLOAT - configurable precision floating point
   *   <li>DOUBLE PRECISION - double precision floating point (64-bit)
   * </ul>
   *
   * <p>Tests Excel's numeric representation compatibility with database floating-point storage,
   * including precision limits and scientific notation handling.
   */
  @Test
  @Preparation
  @Expectation
  void testFloatingPointDataTypes() {
    logger.info(
        "Validating floating point data types (REAL, FLOAT, DOUBLE) - testing precision levels and scientific notation");
    // Framework automatically loads rows where [Pattern] = 'testFloatingPointDataTypes'
  }

  /**
   * Tests character data types with various length constraints and encodings.
   *
   * <p>This test validates text data handling including:
   *
   * <ul>
   *   <li>CHAR(10) - fixed-length character strings with padding
   *   <li>VARCHAR(100) - variable-length strings with length limits
   *   <li>VARCHAR(500) - longer variable-length strings
   *   <li>TEXT - unlimited length text storage
   * </ul>
   *
   * <p>Tests special character handling, UTF-8 encoding, string truncation behavior, and Excel text
   * cell formatting compatibility with database string storage.
   */
  @Test
  @Preparation
  @Expectation
  void testCharacterDataTypes() {
    logger.info(
        "Validating character data types (CHAR, VARCHAR, TEXT) - testing length constraints, special characters, and encoding");
    // Framework automatically loads rows where [Pattern] = 'testCharacterDataTypes'
  }

  /**
   * Tests date and time data types with various temporal precision levels.
   *
   * <p>This test validates temporal data handling including:
   *
   * <ul>
   *   <li>DATE - date-only values (YYYY-MM-DD)
   *   <li>TIME - time-only values (HH:MM:SS)
   *   <li>DATETIME - combined date and time values
   *   <li>TIMESTAMP - timestamp with timezone considerations
   * </ul>
   *
   * <p>Tests Excel date/time cell formatting compatibility, timezone handling, and precision
   * preservation during database storage and retrieval operations.
   */
  @Test
  @Preparation
  @Expectation
  void testDateTimeDataTypes() {
    logger.info(
        "Validating date and time data types - testing temporal precision, formatting, and timezone handling");
    // Framework automatically loads rows where [Pattern] = 'testDateTimeDataTypes'
  }

  /**
   * Tests boolean data types and selective NULL value handling.
   *
   * <p>This test validates logical and null data handling including:
   *
   * <ul>
   *   <li>BOOLEAN - TRUE/FALSE values with Excel compatibility
   *   <li>NULL values in specific columns (NULLABLE_INT, NULLABLE_VARCHAR, NULLABLE_DATE)
   *   <li>Mixed data scenarios with both populated and null fields
   * </ul>
   *
   * <p>Tests Excel boolean cell interpretation (TRUE/FALSE, 1/0) and empty cell handling for
   * nullable columns while maintaining non-null data in other fields.
   */
  @Test
  @Preparation
  @Expectation
  void testBooleanAndNullDataTypes() {
    logger.info(
        "Validating boolean data types and selective NULL handling - testing logical values and mixed null scenarios");
    // Framework automatically loads rows where [Pattern] = 'testBooleanAndNullDataTypes'
  }

  /**
   * Tests database default value behavior and framework interaction.
   *
   * <p>This test validates default value handling including:
   *
   * <ul>
   *   <li>DEFAULT_INT - integer column with default value 0
   *   <li>DEFAULT_VARCHAR - text column with default value 'default_value'
   *   <li>DEFAULT_BOOLEAN - boolean column with default value FALSE
   *   <li>DEFAULT_TIMESTAMP - timestamp column with CURRENT_TIMESTAMP default
   * </ul>
   *
   * <p>Tests how the framework interacts with database-defined defaults when Excel data explicitly
   * provides values versus relying on database defaults.
   */
  @Test
  @Preparation
  @Expectation
  void testDefaultValues() {
    logger.info(
        "Validating default value behavior - testing database defaults interaction with explicit Excel values");
    // Framework automatically loads rows where [Pattern] = 'testDefaultValues'
  }

  /**
   * Tests comprehensive NULL value handling across all nullable columns.
   *
   * <p>This test validates NULL value processing for all nullable data types:
   *
   * <ul>
   *   <li>All integer types (TINYINT, SMALLINT, INT, INTEGER, BIGINT) set to NULL
   *   <li>All decimal types (DECIMAL, NUMERIC, DECIMAL_PRECISION) set to NULL
   *   <li>All floating-point types (REAL, FLOAT, DOUBLE) set to NULL
   *   <li>All character types (CHAR, VARCHAR, VARCHAR_LONG, TEXT) set to NULL
   *   <li>All temporal types (DATE, TIME, DATETIME, TIMESTAMP) set to NULL
   *   <li>Boolean type set to NULL
   *   <li>Explicitly nullable columns (NULLABLE_*) set to NULL
   * </ul>
   *
   * <p>Demonstrates framework's ability to handle empty Excel cells as NULL values across all
   * supported SQL data types while preserving DEFAULT column behavior.
   */
  @Test
  @Preparation
  @Expectation
  void testAllNullValues() {
    logger.info(
        "Validating comprehensive NULL value handling - testing empty Excel cells as NULL across all data types");
    // Framework automatically loads rows where [Pattern] = 'testAllNullValues'
  }

  /**
   * Tests comprehensive end-to-end data insertion with all supported SQL data types.
   *
   * <p>This integration test validates the complete framework workflow:
   *
   * <ul>
   *   <li>CLEAN_INSERT operation for complete table refresh
   *   <li>All standard SQL data types in a single comprehensive test
   *   <li>Boundary values, precision limits, and edge cases
   *   <li>Excel-to-database-to-Excel round-trip data integrity
   *   <li>Framework's ability to handle complex, mixed data scenarios
   * </ul>
   *
   * <p>Includes additional validation logic to verify data insertion success and provides a
   * comprehensive demonstration of the framework's capabilities with real-world data complexity.
   *
   * @throws Exception if database connection or data validation fails
   */
  @Test
  @Preparation(dataSets = {@DataSet(operation = Operation.CLEAN_INSERT)})
  @Expectation
  void testComprehensiveDataInsertion() throws Exception {
    logger.info(
        "Executing comprehensive end-to-end test - validating complete data workflow with all SQL types");
    // Framework automatically loads rows where [Pattern] = 'testComprehensiveDataInsertion'

    // Verify data was inserted correctly
    final var dataSource = DatabaseTesterContext.getInstance().getDataSource(null);
    try (final var connection = dataSource.getConnection();
        final var statement = connection.createStatement();
        final var resultSet =
            statement.executeQuery("SELECT COUNT(*) FROM COMPREHENSIVE_DATA_TYPES")) {

      if (!resultSet.next()) {
        throw new AssertionError("Failed to query COMPREHENSIVE_DATA_TYPES row count");
      }

      final var count = resultSet.getInt(1);
      logger.info(
          "Data insertion verification: {} rows found in COMPREHENSIVE_DATA_TYPES table", count);
      assert count > 0 : "Expected data to be inserted, but COMPREHENSIVE_DATA_TYPES is empty";
    }

    logger.info(
        "Comprehensive data type validation completed successfully - all SQL types processed correctly");
  }
}
