package com.github.seijikohara.junit5dbtester.annotation;

import com.github.seijikohara.junit5dbtester.operation.Operation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for setting up test data before test method execution.
 *
 * <p>This annotation instructs the {@link
 * com.github.seijikohara.junit5dbtester.extension.DatabaseTestExtension} to load and apply test
 * data from Excel files to the database before the test method runs. It supports multiple datasets,
 * pattern-based filtering, and various database operations for comprehensive test data preparation.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Convention-based Excel file resolution
 *   <li>Multiple dataset support for complex test scenarios
 *   <li>Pattern-based data filtering for scenario-specific data
 *   <li>Configurable database operations (INSERT, UPDATE, DELETE, etc.)
 * </ul>
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Basic preparation with convention-based file resolution
 * // Loads from classpath:[TestClassName].xls using test method name as pattern
 * @Test
 * @Preparation
 * void testUserCreation() { }
 *
 * // Preparation with specific pattern filtering
 * @Test
 * @Preparation(@DataSet(patternNames = {"initial_users", "roles"}))
 * void testUserWithRoles() { }
 *
 * // Multiple datasets for complex scenarios
 * @Test
 * @Preparation({
 *   @DataSet(resourceLocation = "classpath:users.xlsx", patternNames = "admin"),
 *   @DataSet(resourceLocation = "classpath:products.xlsx", patternNames = "catalog")
 * })
 * void testAdminProductManagement() { }
 *
 * // Custom operation for specific preparation needs
 * @Test
 * @Preparation(@DataSet(operation = Operation.INSERT))
 * void testIncrementalData() { }
 * }</pre>
 *
 * <p>File Resolution Convention:
 *
 * <p>When no explicit {@code resourceLocation} is specified in {@code @DataSet}, the system uses
 * convention-based resolution: {@code classpath:[TestClassName].xls} or {@code
 * classpath:[TestClassName].xlsx}
 *
 * @see com.github.seijikohara.junit5dbtester.annotation.DataSet
 * @see com.github.seijikohara.junit5dbtester.annotation.Expectation
 * @see com.github.seijikohara.junit5dbtester.extension.DatabaseTestExtension
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Preparation {

  /**
   * Array of dataset configurations for this preparation.
   *
   * <p>Each {@link DataSet} in this array specifies:
   *
   * <ul>
   *   <li>Excel file location (or uses convention-based resolution)
   *   <li>Pattern names for filtering specific test scenarios
   *   <li>Data source configuration for multi-database testing
   *   <li>Database operation to perform
   * </ul>
   *
   * <p>If not specified (empty array), uses convention-based configuration:
   *
   * <ul>
   *   <li>File: {@code classpath:[TestClassName].xls} or {@code .xlsx}
   *   <li>Pattern: test method name
   *   <li>Operation: {@link Operation#CLEAN_INSERT}
   * </ul>
   *
   * <p>Multiple datasets are processed in array order, allowing for complex test data setup
   * scenarios involving multiple files or databases.
   *
   * @return array of dataset configurations
   */
  DataSet[] dataSets() default {};

  /**
   * Default database operation for all datasets in this preparation.
   *
   * <p>This operation applies to datasets that don't explicitly specify their own operation.
   * Individual {@link DataSet} configurations can override this default by specifying their own
   * {@code operation}.
   *
   * <p>Common operations for preparation:
   *
   * <ul>
   *   <li>{@link Operation#CLEAN_INSERT} - Most common, ensures clean state
   *   <li>{@link Operation#INSERT} - Add data without removing existing
   *   <li>{@link Operation#UPDATE} - Modify existing data
   * </ul>
   *
   * @return the default operation for datasets without explicit operation
   */
  Operation operation() default Operation.CLEAN_INSERT;
}
