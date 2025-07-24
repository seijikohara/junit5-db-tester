package com.github.seijikohara.junit5dbtester.assertion;

import java.sql.SQLException;
import org.dbunit.DatabaseUnitException;
import org.dbunit.assertion.FailureHandler;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;

/**
 * Static facade for database assertion operations. Provides convenient methods for comparing
 * database states in JUnit 5 tests.
 */
public final class DatabaseAssertion {

  /** Object that will effectively do the assertions. */
  private static final DatabaseAssert INSTANCE = new DatabaseAssert();

  private DatabaseAssertion() {
    throw new UnsupportedOperationException("This class has only static methods");
  }

  /**
   * Asserts that two datasets are equal, ignoring specified columns in a table.
   *
   * @param expectedDataset The expected dataset
   * @param actualDataset The actual dataset
   * @param tableName The table name to compare
   * @param ignoreCols The columns to ignore during comparison
   * @throws DatabaseUnitException if assertion fails
   */
  public static void assertEqualsIgnoreCols(
      final IDataSet expectedDataset,
      final IDataSet actualDataset,
      final String tableName,
      final String... ignoreCols)
      throws DatabaseUnitException {
    INSTANCE.assertEqualsIgnoreCols(expectedDataset, actualDataset, tableName, ignoreCols);
  }

  /**
   * Asserts that two tables are equal, ignoring specified columns.
   *
   * @param expectedTable The expected table
   * @param actualTable The actual table
   * @param ignoreCols The columns to ignore during comparison
   * @throws DatabaseUnitException if assertion fails
   */
  public static void assertEqualsIgnoreCols(
      final ITable expectedTable, final ITable actualTable, final String... ignoreCols)
      throws DatabaseUnitException {
    INSTANCE.assertEqualsIgnoreCols(expectedTable, actualTable, ignoreCols);
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
  public static void assertEqualsByQuery(
      final IDataSet expectedDataset,
      final IDatabaseConnection connection,
      final String sqlQuery,
      final String tableName,
      final String... ignoreCols)
      throws DatabaseUnitException, SQLException {
    INSTANCE.assertEqualsByQuery(expectedDataset, connection, sqlQuery, tableName, ignoreCols);
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
  public static void assertEqualsByQuery(
      final ITable expectedTable,
      final IDatabaseConnection connection,
      final String tableName,
      final String sqlQuery,
      final String... ignoreCols)
      throws DatabaseUnitException, SQLException {
    INSTANCE.assertEqualsByQuery(expectedTable, connection, tableName, sqlQuery, ignoreCols);
  }

  /**
   * Asserts that two datasets are equal.
   *
   * @param expectedDataSet The expected dataset
   * @param actualDataSet The actual dataset
   * @throws DatabaseUnitException if assertion fails
   */
  public static void assertEquals(final IDataSet expectedDataSet, final IDataSet actualDataSet)
      throws DatabaseUnitException {
    INSTANCE.assertEquals(expectedDataSet, actualDataSet);
  }

  /**
   * Asserts that two datasets are equal using custom failure handler.
   *
   * @param expectedDataSet The expected dataset
   * @param actualDataSet The actual dataset
   * @param failureHandler Custom failure handler
   * @throws DatabaseUnitException if assertion fails
   */
  public static void assertEquals(
      final IDataSet expectedDataSet,
      final IDataSet actualDataSet,
      final FailureHandler failureHandler)
      throws DatabaseUnitException {
    INSTANCE.assertEquals(expectedDataSet, actualDataSet, failureHandler);
  }

  /**
   * Asserts that two tables are equal.
   *
   * @param expectedTable The expected table
   * @param actualTable The actual table
   * @throws DatabaseUnitException if assertion fails
   */
  public static void assertEquals(final ITable expectedTable, final ITable actualTable)
      throws DatabaseUnitException {
    INSTANCE.assertEquals(expectedTable, actualTable);
  }

  /**
   * Asserts that two tables are equal with additional column information.
   *
   * @param expectedTable The expected table
   * @param actualTable The actual table
   * @param additionalColumnInfo Additional column information for comparison
   * @throws DatabaseUnitException if assertion fails
   */
  public static void assertEquals(
      final ITable expectedTable, final ITable actualTable, final Column[] additionalColumnInfo)
      throws DatabaseUnitException {
    INSTANCE.assertEquals(expectedTable, actualTable, additionalColumnInfo);
  }

  /**
   * Asserts that two tables are equal using custom failure handler.
   *
   * @param expectedTable The expected table
   * @param actualTable The actual table
   * @param failureHandler Custom failure handler
   * @throws DatabaseUnitException if assertion fails
   */
  public static void assertEquals(
      final ITable expectedTable, final ITable actualTable, final FailureHandler failureHandler)
      throws DatabaseUnitException {
    INSTANCE.assertEquals(expectedTable, actualTable, failureHandler);
  }
}
