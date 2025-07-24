package com.github.seijikohara.junit5dbtester.operation;

import org.dbunit.operation.DatabaseOperation;

/**
 * Enumeration of database operations supported for test data manipulation.
 *
 * <p>Each operation maps to a corresponding DBUnit {@link DatabaseOperation} and provides different
 * strategies for managing test data:
 *
 * <table border="1">
 *   <caption>Database Operation Types</caption>
 *   <tr>
 *     <th>Operation</th>
 *     <th>Description</th>
 *     <th>Use Case</th>
 *   </tr>
 *   <tr>
 *     <td>{@link #CLEAN_INSERT}</td>
 *     <td>Delete all existing data, then insert new data</td>
 *     <td>Most common for test setup - ensures clean state</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #INSERT}</td>
 *     <td>Insert new rows only</td>
 *     <td>Adding test data without affecting existing data</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #UPDATE}</td>
 *     <td>Update existing rows</td>
 *     <td>Modifying existing test data</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #DELETE}</td>
 *     <td>Delete specified rows</td>
 *     <td>Removing specific test data</td>
 *   </tr>
 * </table>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @Preparation(@DataSet(operation = Operation.CLEAN_INSERT))
 * void testWithCleanData() {
 *   // Test with fresh data
 * }
 * }</pre>
 *
 * @see org.dbunit.operation.DatabaseOperation
 * @see com.github.seijikohara.junit5dbtester.annotation.DataSet#operation()
 */
public enum Operation {

  /** No operation performed - useful for scenarios where data loading is conditional. */
  NONE,

  /** Update existing rows in database tables. Fails if rows don't exist. */
  UPDATE,

  /**
   * Insert new rows into database tables. Fails if rows already exist (duplicate key constraint).
   */
  INSERT,

  /** Refresh existing rows with new data. Updates if exists, inserts if not. */
  REFRESH,

  /** Delete specific rows from database tables. Only removes rows that match the dataset. */
  DELETE,

  /** Delete all rows from the specified tables. Removes all data regardless of dataset content. */
  DELETE_ALL,

  /**
   * Truncate tables completely. Removes all data and resets auto-increment sequences for better
   * performance.
   */
  TRUNCATE_TABLE,

  /**
   * Clean insert operation - the recommended choice for most test scenarios.
   *
   * <p>This operation performs a complete data refresh:
   *
   * <ol>
   *   <li>Deletes all existing data from target tables
   *   <li>Inserts new data from the dataset
   * </ol>
   *
   * <p>Guarantees a predictable, clean state for each test execution.
   */
  CLEAN_INSERT;

  /**
   * Gets the corresponding DBUnit DatabaseOperation.
   *
   * @return DBUnit operation instance
   */
  public DatabaseOperation getDatabaseOperation() {
    return switch (this) {
      case NONE -> DatabaseOperation.NONE;
      case UPDATE -> DatabaseOperation.UPDATE;
      case INSERT -> DatabaseOperation.INSERT;
      case REFRESH -> DatabaseOperation.REFRESH;
      case DELETE -> DatabaseOperation.DELETE;
      case DELETE_ALL -> DatabaseOperation.DELETE_ALL;
      case TRUNCATE_TABLE -> DatabaseOperation.TRUNCATE_TABLE;
      case CLEAN_INSERT -> DatabaseOperation.CLEAN_INSERT;
    };
  }
}
