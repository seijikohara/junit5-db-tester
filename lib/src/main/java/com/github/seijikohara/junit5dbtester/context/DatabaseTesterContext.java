package com.github.seijikohara.junit5dbtester.context;

import com.github.seijikohara.junit5dbtester.dataset.loader.DataSetLoader;
import com.github.seijikohara.junit5dbtester.dataset.loader.TestClassNameBasedDataSetLoader;
import com.github.seijikohara.junit5dbtester.operation.Operation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;

/**
 * Singleton context configuration for database testing framework.
 *
 * <p>This class serves as the central configuration hub for the database testing framework,
 * managing data sources, default operations, file resolution conventions, and loading strategies.
 * It provides a singleton instance to ensure consistent configuration across all test executions.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li>Data source management with support for multiple named data sources
 *   <li>Configuration of default database operations for preparation and expectation
 *   <li>File naming conventions for Excel-based test data
 *   <li>Pattern marker configuration for Excel sheet filtering
 *   <li>Dataset loader strategy configuration
 * </ul>
 *
 * <p>Default Configuration:
 *
 * <ul>
 *   <li>Default preparation operation: {@link Operation#CLEAN_INSERT}
 *   <li>Default expectation operation: {@link Operation#NONE}
 *   <li>Expectation file suffix: {@code "-expected"}
 *   <li>Pattern marker: {@code "[Pattern]"}
 *   <li>Dataset loader: {@link TestClassNameBasedDataSetLoader}
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Get singleton instance
 * DatabaseTesterContext context = DatabaseTesterContext.getInstance();
 *
 * // Register data sources
 * context.registerDataSource("primary", primaryDataSource);
 * context.registerDataSource("secondary", secondaryDataSource);
 *
 * // Use in tests - automatically handled by DatabaseTestExtension
 * DataSource ds = context.getDataSource("primary");
 * }</pre>
 *
 * <p>Thread Safety:
 *
 * <p>This class is thread-safe using the initialization-on-demand holder pattern for singleton
 * initialization and concurrent hash maps for data source storage.
 *
 * @see com.github.seijikohara.junit5dbtester.extension.DatabaseTestExtension
 * @see com.github.seijikohara.junit5dbtester.dataset.loader.DataSetLoader
 */
public final class DatabaseTesterContext {

  private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
  private final String defaultFileDirectory;
  private final Operation defaultPreparationOperation;
  private final Operation defaultExpectationOperation;
  private final String expectFileSuffix;
  private final String patternMarker;
  private final DataSetLoader dataSetLoader;
  private @Nullable DataSource defaultDataSource;

  private DatabaseTesterContext() {
    this.defaultFileDirectory = "";
    this.defaultPreparationOperation = Operation.CLEAN_INSERT;
    this.defaultExpectationOperation = Operation.NONE;
    this.expectFileSuffix = "-expected";
    this.patternMarker = "[Pattern]";
    this.dataSetLoader = new TestClassNameBasedDataSetLoader();
  }

  /** Holder class for thread-safe lazy initialization using initialization-on-demand pattern. */
  private static final class InstanceHolder {
    private static final DatabaseTesterContext INSTANCE = new DatabaseTesterContext();
  }

  /**
   * Returns the singleton instance of DatabaseTesterContext.
   *
   * @return the singleton instance
   */
  public static DatabaseTesterContext getInstance() {
    return InstanceHolder.INSTANCE;
  }

  /**
   * Registers a data source with the specified name.
   *
   * <p>Data sources can be registered with custom names to support multi-database testing
   * scenarios. If the name is "dataSource", null, or empty, the data source becomes the default
   * data source for the framework.
   *
   * <p>The default data source is used when:
   *
   * <ul>
   *   <li>No data source name is specified in {@link
   *       com.github.seijikohara.junit5dbtester.annotation.DataSet}
   *   <li>A requested named data source is not found
   * </ul>
   *
   * @param name the name to associate with the data source
   * @param dataSource the data source instance to register
   * @throws NullPointerException if dataSource is null
   */
  public void registerDataSource(final String name, final DataSource dataSource) {
    if ("dataSource".equals(name) || name == null || name.isEmpty()) {
      this.defaultDataSource = dataSource;
    }
    dataSources.put(name, dataSource);
  }

  /**
   * Retrieves a data source by name, with fallback to default data source.
   *
   * <p>This method supports flexible data source resolution:
   *
   * <ul>
   *   <li>If name is null or empty: returns the default data source
   *   <li>If named data source exists: returns the named data source
   *   <li>If named data source doesn't exist: falls back to default data source
   * </ul>
   *
   * <p>This fallback behavior ensures that tests can run even when specific data source names are
   * not configured, using the default data source instead.
   *
   * @param name the name of the data source to retrieve, or null/empty for default
   * @return the data source associated with the name, or default data source
   * @throws IllegalStateException if no default data source is registered
   */
  public DataSource getDataSource(final @Nullable String name) {
    return Optional.ofNullable(name)
        .filter(n -> !n.isEmpty())
        .map(dataSources::get)
        .or(() -> Optional.ofNullable(defaultDataSource))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    name == null || name.isEmpty()
                        ? "No default data source registered"
                        : "No data source registered for name: " + name));
  }

  /**
   * Gets the default file directory for Excel test resources.
   *
   * <p>This directory is used as a base path for convention-based file resolution when no explicit
   * resource location is specified in dataset configurations. Currently defaults to empty string
   * (classpath root).
   *
   * @return the default file directory path
   */
  public String getDefaultFileDirectory() {
    return defaultFileDirectory;
  }

  /**
   * Gets the default database operation for preparation datasets.
   *
   * <p>This operation is applied to preparation datasets when no explicit operation is specified in
   * the {@link com.github.seijikohara.junit5dbtester.annotation.Preparation} or {@link
   * com.github.seijikohara.junit5dbtester.annotation.DataSet} annotations.
   *
   * @return the default preparation operation (typically {@link Operation#CLEAN_INSERT})
   */
  public Operation getDefaultPreparationOperation() {
    return defaultPreparationOperation;
  }

  /**
   * Gets the default database operation for expectation datasets.
   *
   * <p>This operation is used for expectation validation. Typically set to {@link Operation#NONE}
   * since expectations perform read-only comparisons rather than database modifications.
   *
   * @return the default expectation operation (typically {@link Operation#NONE})
   */
  public Operation getDefaultExpectationOperation() {
    return defaultExpectationOperation;
  }

  /**
   * Gets the file suffix used for expectation dataset files.
   *
   * <p>This suffix is appended to the base test class name when resolving expectation dataset files
   * using convention-based naming. For example, if the test class is {@code UserServiceTest} and
   * the suffix is {@code "-expected"}, the expectation file would be {@code
   * UserServiceTest-expected.xls}.
   *
   * @return the expectation file suffix (e.g., "-expected")
   */
  public String getExpectFileSuffix() {
    return expectFileSuffix;
  }

  /**
   * Gets the pattern marker format used in Excel sheets for pattern-based filtering.
   *
   * <p>This marker format is used to identify pattern sections in Excel sheets. Pattern names are
   * enclosed within this marker format. For example, with the default marker "[Pattern]", a pattern
   * named "setup" would appear as "[setup]" in the Excel sheet.
   *
   * <p>The actual pattern matching replaces "Pattern" with the specific pattern name.
   *
   * @return the pattern marker format (e.g., "[Pattern]")
   */
  public String getPatternMarker() {
    return patternMarker;
  }

  /**
   * Gets the dataset loader responsible for loading preparation and expectation datasets.
   *
   * <p>The dataset loader handles:
   *
   * <ul>
   *   <li>Convention-based file resolution from test class names
   *   <li>Excel file parsing and data extraction
   *   <li>Pattern-based filtering of test data
   *   <li>Data source association with loaded datasets
   * </ul>
   *
   * @return the configured dataset loader instance
   * @see com.github.seijikohara.junit5dbtester.dataset.loader.TestClassNameBasedDataSetLoader
   */
  public DataSetLoader getDataSetLoader() {
    return dataSetLoader;
  }
}
