package com.github.seijikohara.junit5dbtester.assertion;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.dbunit.DatabaseUnitException;
import org.dbunit.assertion.DbUnitAssert;
import org.dbunit.assertion.Difference;
import org.dbunit.assertion.FailureHandler;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.filter.DefaultColumnFilter;

/**
 * Enhanced database assertion implementation that extends DbUnitAssert. Provides detailed error
 * messages and column filtering capabilities.
 */
final class DatabaseAssert extends DbUnitAssert {

  /**
   * Asserts that two datasets are equal, ignoring specified columns in a table.
   *
   * @param expectedDataset The expected dataset
   * @param actualDataset The actual dataset
   * @param tableName The table name to compare
   * @param ignoreCols The columns to ignore during comparison
   * @throws DatabaseUnitException if assertion fails
   */
  @Override
  public void assertEqualsIgnoreCols(
      final IDataSet expectedDataset,
      final IDataSet actualDataset,
      final String tableName,
      final String[] ignoreCols)
      throws DatabaseUnitException {
    final var expectedTable = expectedDataset.getTable(tableName);
    final var actualTable = actualDataset.getTable(tableName);
    assertEqualsIgnoreCols(expectedTable, actualTable, ignoreCols);
  }

  /**
   * Asserts that two tables are equal, ignoring specified columns.
   *
   * @param expectedTable The expected table
   * @param actualTable The actual table
   * @param ignoreCols The columns to ignore during comparison
   * @throws DatabaseUnitException if assertion fails
   */
  @Override
  public void assertEqualsIgnoreCols(
      final ITable expectedTable, final ITable actualTable, final String[] ignoreCols)
      throws DatabaseUnitException {
    final var filteredExpected =
        DefaultColumnFilter.excludedColumnsTable(expectedTable, ignoreCols);
    final var filteredActual = DefaultColumnFilter.excludedColumnsTable(actualTable, ignoreCols);
    assertEquals(filteredExpected, filteredActual);
  }

  /**
   * Asserts that expected dataset equals actual database state obtained by SQL query.
   *
   * @param expectedDataset The expected dataset
   * @param connection Database connection
   * @param sqlQuery SQL query to get actual data
   * @param tableName Table name for comparison
   * @param ignoreCols Columns to ignore during comparison
   * @throws DatabaseUnitException if assertion fails
   * @throws SQLException if database operation fails
   */
  @Override
  public void assertEqualsByQuery(
      final IDataSet expectedDataset,
      final IDatabaseConnection connection,
      final String sqlQuery,
      final String tableName,
      final String[] ignoreCols)
      throws DatabaseUnitException, SQLException {
    final var expectedTable = expectedDataset.getTable(tableName);
    assertEqualsByQuery(expectedTable, connection, tableName, sqlQuery, ignoreCols);
  }

  /**
   * Asserts that expected table equals actual database state obtained by SQL query.
   *
   * @param expectedTable The expected table
   * @param connection Database connection
   * @param tableName Table name for comparison
   * @param sqlQuery SQL query to get actual data
   * @param ignoreCols Columns to ignore during comparison
   * @throws DatabaseUnitException if assertion fails
   * @throws SQLException if database operation fails
   */
  @Override
  public void assertEqualsByQuery(
      final ITable expectedTable,
      final IDatabaseConnection connection,
      final String tableName,
      final String sqlQuery,
      final String[] ignoreCols)
      throws DatabaseUnitException, SQLException {
    final var actualTable = connection.createQueryTable(tableName, sqlQuery);
    assertEqualsIgnoreCols(expectedTable, actualTable, ignoreCols);
  }

  @Override
  public void assertEquals(
      final IDataSet expectedDataSet,
      final IDataSet actualDataSet,
      final FailureHandler failureHandler)
      throws DatabaseUnitException {
    final var validator = new DataSetValidator(expectedDataSet, actualDataSet, failureHandler);
    validator.validateDataSets();
  }

  /**
   * Validator for comparing two datasets with detailed error reporting.
   *
   * <p>This inner class encapsulates the complex logic for dataset comparison, providing
   * comprehensive validation that includes both structural (table names, counts) and content
   * (row-by-row) comparison with detailed error reporting.
   *
   * <p>Validation process:
   *
   * <ol>
   *   <li>Identity check - skip validation if datasets are the same instance
   *   <li>Structural validation - compare table names and counts
   *   <li>Content validation - perform detailed table-by-table comparison
   *   <li>Error aggregation - collect and report all validation failures
   * </ol>
   *
   * <p>The validator uses a fail-fast approach for structural issues but continues validation to
   * collect all content errors before reporting, providing comprehensive feedback for debugging
   * test failures.
   */
  private final class DataSetValidator {
    private final IDataSet expectedDataSet;
    private final IDataSet actualDataSet;
    private final FailureHandler failureHandler;

    private DataSetValidator(
        final IDataSet expectedDataSet,
        final IDataSet actualDataSet,
        final FailureHandler failureHandler) {
      this.expectedDataSet = expectedDataSet;
      this.actualDataSet = actualDataSet;
      this.failureHandler =
          Optional.ofNullable(failureHandler)
              .orElse(DatabaseAssert.this.getDefaultFailureHandler());
    }

    private void validateDataSets() throws DatabaseUnitException {
      if (isSameInstance()) {
        return;
      }
      validateTableStructure();
      validateTableContents();
    }

    private boolean isSameInstance() {
      return expectedDataSet == actualDataSet;
    }

    private void validateTableStructure() throws DatabaseUnitException {
      final var expectedNames = DatabaseAssert.this.getSortedTableNames(expectedDataSet);
      final var actualNames = DatabaseAssert.this.getSortedTableNames(actualDataSet);

      validateTableCount(expectedNames, actualNames);
      validateTableNames(expectedNames, actualNames);
    }

    private void validateTableCount(final String[] expectedNames, final String[] actualNames)
        throws DatabaseUnitException {
      if (expectedNames.length != actualNames.length) {
        throw failureHandler.createFailure(
            "table count",
            String.valueOf(expectedNames.length),
            String.valueOf(actualNames.length));
      }
    }

    private void validateTableNames(final String[] expectedNames, final String[] actualNames) {
      final var expectedList = List.of(expectedNames);
      final var actualList = List.of(actualNames);
      IntStream.range(0, expectedList.size())
          .filter(i -> !actualList.get(i).equals(expectedList.get(i)))
          .findFirst()
          .ifPresent(
              i -> {
                throw new RuntimeException(
                    failureHandler.createFailure(
                        "tables", expectedList.toString(), actualList.toString()));
              });
    }

    private void validateTableContents() throws DatabaseUnitException {
      final var expectedNames = DatabaseAssert.this.getSortedTableNames(expectedDataSet);
      final var errorMessages = collectTableErrors(expectedNames);

      if (!errorMessages.isEmpty()) {
        throw failureHandler.createFailure(
            "Comparison failure"
                + System.lineSeparator()
                + String.join(System.lineSeparator(), errorMessages));
      }
    }

    private List<String> collectTableErrors(final String[] expectedNames) {
      return Stream.of(expectedNames)
          .map(this::validateSingleTable)
          .flatMap(Optional::stream)
          .toList();
    }

    private Optional<String> validateSingleTable(final String tableName) {
      try {
        DatabaseAssert.this.assertEquals(
            expectedDataSet.getTable(tableName), actualDataSet.getTable(tableName), failureHandler);
        return Optional.empty();
      } catch (final AssertionError error) {
        return Optional.ofNullable(error.getMessage())
            .or(() -> Optional.of(error.getClass().getSimpleName()));
      } catch (final DatabaseUnitException e) {
        return Optional.of("DataSet error for table " + tableName + ": " + e.getMessage());
      }
    }
  }

  @Override
  protected void compareData(
      final ITable expectedTable,
      final ITable actualTable,
      final ComparisonColumn[] comparisonCols,
      final FailureHandler failureHandler)
      throws DataSetException {
    final var comparator =
        new TableDataComparator(expectedTable, actualTable, comparisonCols, failureHandler);
    comparator.compareAllData();
  }

  /**
   * Comparator for detailed table data comparison with comprehensive error handling.
   *
   * <p>This inner class performs cell-by-cell comparison of table data, using DBUnit's
   * ComparisonColumn specifications to determine how each column should be compared. It provides
   * detailed error reporting for mismatched values.
   *
   * <p>Comparison process:
   *
   * <ol>
   *   <li>Iterate through all rows in the expected table
   *   <li>For each row, compare all specified columns
   *   <li>Use ComparisonColumn's DataType for type-specific comparison
   *   <li>Collect all errors before reporting for comprehensive feedback
   *   <li>Handle DataSetExceptions during value retrieval
   * </ol>
   *
   * <p>Key features:
   *
   * <ul>
   *   <li>Type-aware comparison using DBUnit's DataType system
   *   <li>Comprehensive error collection and reporting
   *   <li>Proper null and empty value handling
   *   <li>Integration with DBUnit's FailureHandler for consistent error formatting
   * </ul>
   *
   * <p>The comparator validates all constructor parameters to prevent null pointer exceptions
   * during comparison operations.
   */
  private final class TableDataComparator {
    private final ITable expectedTable;
    private final ITable actualTable;
    private final ComparisonColumn[] comparisonCols;
    private final FailureHandler failureHandler;

    private TableDataComparator(
        final ITable expectedTable,
        final ITable actualTable,
        final ComparisonColumn[] comparisonCols,
        final FailureHandler failureHandler) {
      this.expectedTable = validateNotNull(expectedTable, "expectedTable");
      this.actualTable = validateNotNull(actualTable, "actualTable");
      this.comparisonCols = validateNotNull(comparisonCols, "comparisonCols");
      this.failureHandler = validateNotNull(failureHandler, "failureHandler");
    }

    private static <T> T validateNotNull(final T parameter, final String parameterName) {
      return Optional.ofNullable(parameter)
          .orElseThrow(
              () ->
                  new NullPointerException(
                      "The parameter '" + parameterName + "' must not be null"));
    }

    private void compareAllData() throws DataSetException {
      final var errorMessages = collectAllErrors();
      if (!errorMessages.isEmpty()) {
        throw failureHandler.createFailure(String.join(System.lineSeparator(), errorMessages));
      }
    }

    private List<String> collectAllErrors() {
      return IntStream.range(0, expectedTable.getRowCount())
          .boxed()
          .flatMap(this::compareRow)
          .flatMap(Optional::stream)
          .toList();
    }

    private Stream<Optional<String>> compareRow(final int rowIndex) {
      return Arrays.stream(comparisonCols).map(column -> compareColumn(rowIndex, column));
    }

    private Optional<String> compareColumn(final int rowIndex, final ComparisonColumn column) {
      final var columnName = column.getColumnName();
      final var dataType = column.getDataType();

      try {
        final var expectedValue = expectedTable.getValue(rowIndex, columnName);
        final var actualValue = actualTable.getValue(rowIndex, columnName);

        if (DatabaseAssert.this.skipCompare(columnName, expectedValue, actualValue)) {
          return Optional.empty();
        }

        if (dataType.compare(expectedValue, actualValue) != 0) {
          return handleDifference(rowIndex, columnName, expectedValue, actualValue);
        }

        return Optional.empty();
      } catch (final DataSetException e) {
        return Optional.of(createDataSetErrorMessage(rowIndex, columnName, e));
      }
    }

    private Optional<String> handleDifference(
        final int rowIndex,
        final String columnName,
        final Object expectedValue,
        final Object actualValue) {
      final var diff =
          new Difference(
              expectedTable, actualTable, rowIndex, columnName, expectedValue, actualValue);

      try {
        failureHandler.handle(diff);
        return Optional.empty();
      } catch (final AssertionError error) {
        return Optional.ofNullable(error.getMessage())
            .or(() -> Optional.of(error.getClass().getSimpleName()));
      }
    }

    private String createDataSetErrorMessage(
        final int rowIndex, final String columnName, final DataSetException e) {
      return "DataSet error at row " + rowIndex + ", column " + columnName + ": " + e.getMessage();
    }
  }
}
