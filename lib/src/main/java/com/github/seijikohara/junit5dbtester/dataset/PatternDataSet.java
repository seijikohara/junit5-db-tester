package com.github.seijikohara.junit5dbtester.dataset;

import java.util.Optional;
import javax.sql.DataSource;
import org.dbunit.dataset.AbstractDataSet;
import org.jspecify.annotations.Nullable;

/**
 * Abstract base class for pattern-based datasets with DataSource integration.
 *
 * <p>This class extends DBUnit's AbstractDataSet to provide:
 *
 * <ul>
 *   <li>Optional DataSource association for multi-database testing scenarios
 *   <li>Foundation for pattern-based data filtering implementations
 *   <li>JUnit 5 integration capabilities through DatabaseTestExtension
 * </ul>
 *
 * <p>Pattern-based datasets allow filtering of test data based on pattern markers, enabling
 * multiple test scenarios to coexist in the same data source. Subclasses should implement the
 * actual dataset loading and pattern filtering logic.
 *
 * <p>DataSource integration enables:
 *
 * <ul>
 *   <li>Multi-database testing with named data sources
 *   <li>Connection management by the testing framework
 *   <li>Proper resource cleanup after test execution
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // DataSource is typically set by DatabaseTestExtension
 * PatternDataSet dataset = new ExcelPatternDataSet(file, "setup", "cleanup");
 * dataset.setDataSource(dataSource);
 *
 * // Access DataSource for database operations
 * dataset.getDataSource().ifPresent(ds -> {
 *     // Perform database operations
 * });
 * }</pre>
 *
 * @see com.github.seijikohara.junit5dbtester.dataset.excel.ExcelPatternDataSet
 * @see com.github.seijikohara.junit5dbtester.extension.DatabaseTestExtension
 */
public abstract class PatternDataSet extends AbstractDataSet {

  private @Nullable DataSource dataSource;

  /** Default constructor for PatternDataSet. */
  protected PatternDataSet() {
    // Default constructor required for proper inheritance
  }

  /**
   * Gets the DataSource associated with this dataset.
   *
   * <p>The DataSource is typically set by the DatabaseTestExtension during test execution to enable
   * database operations. Returns empty if no DataSource has been configured.
   *
   * @return Optional containing the DataSource, or empty if not configured
   */
  public final Optional<DataSource> getDataSource() {
    return Optional.ofNullable(dataSource);
  }

  /**
   * Sets the DataSource for this dataset.
   *
   * <p>This method is typically called by the DatabaseTestExtension to associate the dataset with a
   * specific database connection. The DataSource can be null to indicate no database association.
   *
   * @param dataSource the DataSource to associate with this dataset, or null to clear
   */
  public final void setDataSource(final @Nullable DataSource dataSource) {
    this.dataSource = dataSource;
  }
}
