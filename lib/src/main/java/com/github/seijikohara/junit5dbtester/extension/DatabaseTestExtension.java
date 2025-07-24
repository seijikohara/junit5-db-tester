package com.github.seijikohara.junit5dbtester.extension;

import com.github.seijikohara.junit5dbtester.assertion.DatabaseAssertion;
import com.github.seijikohara.junit5dbtester.context.DatabaseTesterContext;
import com.github.seijikohara.junit5dbtester.dataset.PatternDataSet;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.dbunit.database.DatabaseConnection;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that provides comprehensive database testing capabilities.
 *
 * <p>This extension automatically processes test method annotations to manage database state:
 *
 * <ul>
 *   <li>{@code @Preparation} - Sets up test data before test execution
 *   <li>{@code @Expectation} - Validates database state after test execution
 * </ul>
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Pattern-based Excel dataset loading with filtering capabilities
 *   <li>Multi-datasource support with configurable data source selection
 *   <li>Convention-based file resolution (TestClassName.xls/xlsx)
 *   <li>Comprehensive database operation support (INSERT, UPDATE, DELETE, etc.)
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @ExtendWith(DatabaseTestExtension.class)
 * class MyDatabaseTest {
 *   @Test
 *   @Preparation(@DataSet(patternNames = "setup"))
 *   @Expectation(@DataSet(patternNames = "expected"))
 *   void testDatabaseOperation() {
 *     // Test logic here
 *   }
 * }
 * }</pre>
 *
 * @see com.github.seijikohara.junit5dbtester.annotation.Preparation
 * @see com.github.seijikohara.junit5dbtester.annotation.Expectation
 * @see com.github.seijikohara.junit5dbtester.context.DatabaseTesterContext
 */
@Slf4j
public class DatabaseTestExtension implements BeforeEachCallback, AfterEachCallback {

  private static final String CONTEXT_KEY = "database.tester.context";

  /**
   * Creates a new DatabaseTestExtension instance.
   *
   * <p>The extension uses a singleton {@link DatabaseTesterContext} to manage configuration and
   * data sources across test executions.
   */
  public DatabaseTestExtension() {
    // Use singleton instance
  }

  @Override
  public void beforeEach(final ExtensionContext extensionContext) throws Exception {
    final var context = DatabaseTesterContext.getInstance();

    // Store context in extension context
    extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put(CONTEXT_KEY, context);

    final var testMethod = extensionContext.getRequiredTestMethod();
    final var testClass = extensionContext.getRequiredTestClass();

    // Process @Preparation annotations
    processPreparation(testClass, testMethod);
  }

  @Override
  public void afterEach(final ExtensionContext extensionContext) throws Exception {
    final var testMethod = extensionContext.getRequiredTestMethod();
    final var testClass = extensionContext.getRequiredTestClass();

    // Process @Expectation annotations
    processExpectation(testClass, testMethod);
  }

  /**
   * Processes {@code @Preparation} annotations to set up test data before test execution.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Loads datasets specified in {@code @Preparation} annotations
   *   <li>Executes database operations to insert/update test data
   *   <li>Supports multiple datasets with different patterns and data sources
   * </ul>
   *
   * @param testClass the test class containing the method
   * @param testMethod the test method to process
   * @throws Exception if dataset loading or database operations fail
   */
  private void processPreparation(final Class<?> testClass, final Method testMethod)
      throws Exception {
    final var context = DatabaseTesterContext.getInstance();
    final var dataSetLoader = context.getDataSetLoader();
    final var preparationDataSets =
        dataSetLoader.loadPreparationDataSets(context, testClass, testMethod);

    preparationDataSets.forEach(this::executePreparationDataSetUnchecked);
  }

  /**
   * Processes {@code @Expectation} annotations to validate test results after test execution.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Loads expected datasets specified in {@code @Expectation} annotations
   *   <li>Compares actual database state with expected data
   *   <li>Supports column-specific comparisons and filtering
   * </ul>
   *
   * @param testClass the test class containing the method
   * @param testMethod the test method to process
   * @throws Exception if dataset loading or database validation fails
   */
  private void processExpectation(final Class<?> testClass, final Method testMethod)
      throws Exception {
    final var context = DatabaseTesterContext.getInstance();
    final var dataSetLoader = context.getDataSetLoader();
    final var expectationDataSets =
        dataSetLoader.loadExpectationDataSets(context, testClass, testMethod);

    expectationDataSets.forEach(this::executeExpectationDataSetUnchecked);
  }

  /** Executes a preparation dataset by performing the database operation. */
  private void executePreparationDataSet(final PatternDataSet dataSet) throws Exception {
    log.info("Executing preparation dataset: {} tables", dataSet.getTableNames().length);

    final var dataSource =
        dataSet
            .getDataSource()
            .orElseThrow(() -> new IllegalStateException("DataSource not configured for dataset"));

    try (final var connection = dataSource.getConnection()) {
      final var dbConnection = new DatabaseConnection(connection);

      // Get the operation from the dataset annotation or use default
      final var context = DatabaseTesterContext.getInstance();
      final var operation = context.getDefaultPreparationOperation().getDatabaseOperation();
      operation.execute(dbConnection, dataSet);

      log.info("Successfully executed preparation dataset");
    } catch (final SQLException e) {
      throw new RuntimeException("Failed to execute preparation dataset", e);
    }
  }

  /** Wrapper for Stream forEach to handle checked exceptions. */
  private void executePreparationDataSetUnchecked(final PatternDataSet dataSet) {
    try {
      executePreparationDataSet(dataSet);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to execute preparation dataset", e);
    }
  }

  /** Wrapper for Stream forEach to handle checked exceptions. */
  private void executeExpectationDataSetUnchecked(final PatternDataSet dataSet) {
    try {
      executeExpectationDataSet(dataSet);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to execute expectation dataset", e);
    }
  }

  /** Executes an expectation dataset by comparing database state with expected data. */
  private void executeExpectationDataSet(final PatternDataSet dataSet) throws Exception {
    log.info("Validating expectation dataset: {} tables", dataSet.getTableNames().length);

    final var dataSource =
        dataSet
            .getDataSource()
            .orElseThrow(() -> new IllegalStateException("DataSource not configured for dataset"));

    try (final var connection = dataSource.getConnection()) {
      final var dbConnection = new DatabaseConnection(connection);

      // Compare each expected table with actual database state
      Arrays.stream(dataSet.getTableNames())
          .forEach(
              tableName -> {
                try {
                  final var expectedTable = dataSet.getTable(tableName);
                  final var actualTable = dbConnection.createDataSet().getTable(tableName);

                  // Use DatabaseAssertion for detailed comparison
                  DatabaseAssertion.assertEquals(expectedTable, actualTable);
                  log.info("Table '{}' validation passed", tableName);
                } catch (final Exception e) {
                  log.error("Table '{}' validation failed: {}", tableName, e.getMessage());
                  throw new RuntimeException(
                      "Expectation validation failed for table '"
                          + tableName
                          + "': "
                          + e.getMessage(),
                      e);
                }
              });

      log.info(
          "Expectation validation completed successfully for {} tables",
          dataSet.getTableNames().length);

    } catch (final SQLException e) {
      throw new RuntimeException("Failed to validate expectation dataset", e);
    }
  }
}
