package com.github.seijikohara.junit5dbtester.dataset;

import org.dbunit.dataset.AbstractTable;

/**
 * Abstract base class for pattern-based tables with filtering capabilities.
 *
 * <p>This class extends DBUnit's AbstractTable to provide foundation for pattern-based row
 * filtering, enabling selective data loading from complex datasets. Pattern filtering allows a
 * single data source to contain multiple test scenarios, with rows organized by pattern markers.
 *
 * <p>Key capabilities provided by subclasses:
 *
 * <ul>
 *   <li>Pattern-based row filtering using [Pattern] markers in data sources
 *   <li>Selective data loading for specific test scenarios
 *   <li>Multi-scenario test data management within single files
 *   <li>Dynamic filtering based on test method requirements
 * </ul>
 *
 * <p>Pattern Marker Format:
 *
 * <p>Pattern markers are typically formatted as {@code [PatternName]} and placed in data source
 * rows to indicate the beginning of a pattern section. For example:
 *
 * <pre>{@code
 * | [setup]     |             |             |
 * | id          | name        | email       |
 * | 1           | John Doe    | john@...    |
 * | 2           | Jane Smith  | jane@...    |
 * | [cleanup]   |             |             |
 * | id          | name        | email       |
 * | 1           |             |             |
 * | 2           |             |             |
 * }</pre>
 *
 * <p>This enables test methods to load only the data relevant to their specific test scenario by
 * specifying pattern names in annotations.
 *
 * <p>Subclasses should implement:
 *
 * <ul>
 *   <li>Pattern detection and parsing logic
 *   <li>Row filtering based on selected patterns
 *   <li>Data source specific loading mechanisms
 * </ul>
 *
 * @see com.github.seijikohara.junit5dbtester.dataset.excel.ExcelPatternTable
 * @see com.github.seijikohara.junit5dbtester.annotation.DataSet#patternNames()
 */
public abstract class PatternTable extends AbstractTable {

  /** Default constructor for PatternTable. */
  protected PatternTable() {
    // Default constructor required for proper inheritance
  }
}
