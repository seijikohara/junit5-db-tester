package example;

import com.github.seijikohara.junit5dbtester.context.DatabaseTesterContext;
import com.github.seijikohara.junit5dbtester.dataset.excel.ExcelPatternDataSet;
import com.mysql.cj.jdbc.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Demonstrates direct Excel dataset loading and inspection capabilities.
 *
 * <p>This test class shows how to manually load and inspect Excel datasets without using the
 * DatabaseTestExtension framework. Useful for debugging, data validation, and building custom
 * testing workflows that require direct access to Excel data structures.
 */
@Testcontainers
final class ExcelDatasetInspectionTest {

  private static final Logger logger = LoggerFactory.getLogger(ExcelDatasetInspectionTest.class);

  @Container
  @SuppressWarnings("resource") // Managed by Testcontainers framework
  static final MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName("testdb")
          .withUsername("testuser")
          .withPassword("testpass");

  @BeforeAll
  static void setupDatabase() {
    logger.info("Setting up Excel dataset inspection test");

    final var dataSource = createDataSource(mysql);
    DatabaseTesterContext.getInstance().registerDataSource("dataSource", dataSource);

    logger.info("Excel dataset inspection setup completed");
  }

  private static DataSource createDataSource(final MySQLContainer<?> container) {
    try {
      return new SimpleDriverDataSource(
          new Driver(), container.getJdbcUrl(), container.getUsername(), container.getPassword());
    } catch (final SQLException e) {
      throw new RuntimeException("Failed to create data source", e);
    }
  }

  /**
   * Tests direct Excel dataset loading and structure inspection without DatabaseTestExtension.
   *
   * <p>This test demonstrates low-level framework usage for advanced scenarios:
   *
   * <ul>
   *   <li>Direct instantiation of {@link ExcelPatternDataSet} from Excel resource
   *   <li>Pattern-based data filtering ("pattern1") without annotation framework
   *   <li>Manual inspection of dataset structure (tables, rows, columns)
   *   <li>Programmatic access to Excel data without database operations
   * </ul>
   *
   * <p>Useful for debugging Excel file structure, validating data before tests, or building custom
   * testing workflows that extend beyond the standard annotation-based approach.
   *
   * @throws Exception if Excel file loading or data access fails
   */
  @Test
  void testDirectExcelDatasetInspection() throws Exception {
    logger.info(
        "Initiating direct Excel dataset loading - bypassing annotation framework for low-level access");

    final var resource =
        getClass().getClassLoader().getResource("example/ConventionBasedTest.xlsx");
    if (resource == null) {
      throw new IllegalStateException("Excel file not found");
    }

    try (final var inputStream = resource.openStream()) {
      final var dataset = new ExcelPatternDataSet(inputStream, "pattern1");

      logger.info("Excel dataset loaded successfully - pattern filtering applied");
      logger.info("Discovered table names: {}", Arrays.toString(dataset.getTableNames()));

      Arrays.stream(dataset.getTableNames())
          .forEach(
              tableName -> {
                try {
                  final var table = dataset.getTable(tableName);
                  logger.info(
                      "Table '{}' structure: {} data rows, {} columns defined",
                      tableName,
                      table.getRowCount(),
                      table.getTableMetaData().getColumns().length);

                  // Log column schema for debugging
                  final var columns = table.getTableMetaData().getColumns();
                  IntStream.range(0, columns.length)
                      .forEach(
                          i -> logger.info("  Column[{}]: '{}'", i, columns[i].getColumnName()));
                } catch (final Exception e) {
                  throw new RuntimeException("Failed to process table: " + tableName, e);
                }
              });
    }

    logger.info(
        "Direct Excel loading validation completed - dataset structure verified successfully");
  }
}
