package com.github.seijikohara.junit5dbtester.annotation;

import com.github.seijikohara.junit5dbtester.operation.Operation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for configuring database test datasets with Excel-based data loading.
 *
 * <p>This annotation specifies how test data should be loaded from Excel files and applied to
 * database tables. It supports pattern-based filtering, multiple data sources, and various database
 * operations for flexible test data management.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Convention-based Excel file resolution
 *   <li>Pattern-based data filtering using {@code [Pattern]} markers in Excel sheets
 *   <li>Multi-datasource support with configurable data source selection
 *   <li>Comprehensive database operation support (INSERT, UPDATE, DELETE, etc.)
 * </ul>
 *
 * <p>File Resolution Convention:
 *
 * <ul>
 *   <li>Preparation data: {@code classpath:[TestClassName].xls} or {@code .xlsx}
 *   <li>Expectation data: {@code classpath:[TestClassName]-expected.xls} or {@code .xlsx}
 * </ul>
 *
 * <p>Pattern Filtering:
 *
 * <p>Excel sheets can contain multiple test scenarios using pattern markers:
 *
 * <pre>
 * | ID | NAME     | EMAIL           |
 * |----|----------|-----------------|
 * | [setup]                        |
 * | 1  | John Doe | john@email.com  |
 * | 2  | Jane Doe | jane@email.com  |
 * | [update]                       |
 * | 1  | John Updated | john@new.com |
 * </pre>
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Basic usage with convention-based file resolution
 * @Preparation(@DataSet)
 * void testBasicOperation() { }
 *
 * // Specific pattern filtering
 * @Preparation(@DataSet(patternNames = {"setup", "additional"}))
 * void testWithMultiplePatterns() { }
 *
 * // Custom file location and operation
 * @DataSet(
 *   resourceLocation = "classpath:custom-data.xlsx",
 *   patternNames = "scenario1",
 *   operation = Operation.UPDATE
 * )
 *
 * // Multiple data sources
 * @DataSet(dataSourceName = "secondary", patternNames = "migration")
 * }</pre>
 *
 * @see com.github.seijikohara.junit5dbtester.annotation.Preparation
 * @see com.github.seijikohara.junit5dbtester.annotation.Expectation
 * @see com.github.seijikohara.junit5dbtester.operation.Operation
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSet {

  /**
   * Name of the data source to use for this dataset.
   *
   * <p>If not specified, uses the default data source configured in {@link
   * com.github.seijikohara.junit5dbtester.context.DatabaseTesterContext}. This allows tests to work
   * with multiple databases or different database configurations within the same test suite.
   *
   * @return the data source name, or empty string for default data source
   */
  String dataSourceName() default "";

  /**
   * Excel file resource location for loading test data.
   *
   * <p>If not specified, uses convention-based resolution:
   *
   * <ul>
   *   <li>Preparation data: {@code classpath:[TestClassName].xls} or {@code .xlsx}
   *   <li>Expectation data: {@code classpath:[TestClassName]-expected.xls} or {@code .xlsx}
   * </ul>
   *
   * <p>Supports both .xls and .xlsx file formats. The path should be a valid Spring resource
   * location (typically starting with {@code classpath:}).
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>{@code "classpath:test-data.xlsx"}
   *   <li>{@code "classpath:datasets/user-setup.xls"}
   *   <li>{@code "file:/absolute/path/to/data.xlsx"}
   * </ul>
   *
   * @return the resource location, or empty string for convention-based resolution
   */
  String resourceLocation() default "";

  /**
   * Array of pattern names for filtering specific test scenarios from Excel sheets.
   *
   * <p>Pattern names correspond to {@code [PatternName]} markers in Excel sheets, which act as
   * section headers to group related test data. This allows a single Excel file to contain multiple
   * test scenarios.
   *
   * <p>Behavior:
   *
   * <ul>
   *   <li>If not specified: uses the test method name as the pattern
   *   <li>Empty array: includes all data (no pattern filtering)
   *   <li>Multiple patterns: includes data from all specified patterns
   * </ul>
   *
   * <p>Excel sheet structure example:
   *
   * <pre>
   * | ID | NAME     |
   * |----|----------|
   * | [setup]       |
   * | 1  | Initial  |
   * | [teardown]    |
   * | 1  | Final    |
   * </pre>
   *
   * @return array of pattern names to filter, or empty array for no filtering
   */
  String[] patternNames() default {};

  /**
   * Database operation to perform when applying this dataset.
   *
   * <p>Different operations provide different strategies for managing test data:
   *
   * <ul>
   *   <li>{@link Operation#CLEAN_INSERT} - Delete existing data, then insert new data (default)
   *   <li>{@link Operation#INSERT} - Insert new rows only
   *   <li>{@link Operation#UPDATE} - Update existing rows
   *   <li>{@link Operation#DELETE} - Delete specified rows
   * </ul>
   *
   * <p>The default {@code CLEAN_INSERT} is recommended for most test scenarios as it ensures a
   * predictable, clean state for each test execution.
   *
   * @return the database operation to perform
   * @see Operation for detailed operation descriptions
   */
  Operation operation() default Operation.CLEAN_INSERT;
}
