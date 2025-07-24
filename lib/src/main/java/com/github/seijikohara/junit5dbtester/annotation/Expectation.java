package com.github.seijikohara.junit5dbtester.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for validating database state after test method execution.
 *
 * <p>This annotation instructs the {@link
 * com.github.seijikohara.junit5dbtester.extension.DatabaseTestExtension} to validate the database
 * state by comparing actual data with expected data loaded from Excel files. It provides
 * comprehensive assertion capabilities for database testing scenarios.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Convention-based Excel file resolution with "-expected" suffix
 *   <li>Column-specific data comparison and validation
 *   <li>Pattern-based filtering for scenario-specific validation
 *   <li>Multi-table and multi-dataset validation support
 * </ul>
 *
 * <p>Validation Process:
 *
 * <ol>
 *   <li>Load expected data from Excel files
 *   <li>Query actual database state for corresponding tables
 *   <li>Compare actual vs expected data row by row, column by column
 *   <li>Report detailed differences if validation fails
 * </ol>
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Basic expectation with convention-based file resolution
 * // Loads from classpath:[TestClassName]-expected.xls using test method name as pattern
 * @Test
 * @Expectation
 * void testUserCreation() { }
 *
 * // Expectation with specific pattern filtering
 * @Test
 * @Expectation(@DataSet(patternNames = {"final_users", "updated_roles"}))
 * void testUserRoleUpdate() { }
 *
 * // Multiple datasets for complex validation scenarios
 * @Test
 * @Expectation({
 *   @DataSet(resourceLocation = "classpath:users-expected.xlsx", patternNames = "admin"),
 *   @DataSet(resourceLocation = "classpath:audit-expected.xlsx", patternNames = "operations")
 * })
 * void testAdminOperationsWithAudit() { }
 *
 * // Custom data source for multi-database validation
 * @Test
 * @Expectation(@DataSet(dataSourceName = "secondary", patternNames = "replicated"))
 * void testDataReplication() { }
 * }</pre>
 *
 * <p>File Resolution Convention:
 *
 * <p>When no explicit {@code resourceLocation} is specified in {@code @DataSet}, the system uses
 * convention-based resolution: {@code classpath:[TestClassName]-expected.xls} or {@code
 * classpath:[TestClassName]-expected.xlsx}
 *
 * @see com.github.seijikohara.junit5dbtester.annotation.DataSet
 * @see com.github.seijikohara.junit5dbtester.annotation.Preparation
 * @see com.github.seijikohara.junit5dbtester.extension.DatabaseTestExtension
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Expectation {

  /**
   * Array of dataset configurations for this expectation validation.
   *
   * <p>Each {@link DataSet} in this array specifies:
   *
   * <ul>
   *   <li>Excel file location containing expected data (or uses convention-based resolution)
   *   <li>Pattern names for filtering specific validation scenarios
   *   <li>Data source configuration for multi-database validation
   * </ul>
   *
   * <p>If not specified (empty array), uses convention-based configuration:
   *
   * <ul>
   *   <li>File: {@code classpath:[TestClassName]-expected.xls} or {@code .xlsx}
   *   <li>Pattern: test method name
   *   <li>Data source: default from context
   * </ul>
   *
   * <p>Multiple datasets are validated in array order, allowing for complex validation scenarios
   * involving multiple tables, databases, or different expected data sets.
   *
   * <p>Note: The {@code operation} attribute of {@link DataSet} is ignored for expectations as
   * validation is always a read-only comparison operation.
   *
   * @return array of dataset configurations for validation
   */
  DataSet[] dataSets() default {};
}
