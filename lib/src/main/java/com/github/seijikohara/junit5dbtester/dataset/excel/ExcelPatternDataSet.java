package com.github.seijikohara.junit5dbtester.dataset.excel;

import com.github.seijikohara.junit5dbtester.dataset.PatternDataSet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;

/**
 * Excel-based pattern dataset implementation. Provides pattern-based data loading from Excel files
 * with support for multiple worksheets.
 */
public final class ExcelPatternDataSet extends PatternDataSet {

  private final SequencedMap<String, ITable> tables;
  private final List<String> tableNames;

  /**
   * Creates pattern dataset from Excel file.
   *
   * @param file Excel file to load
   * @param patternNames Array of pattern names to filter
   * @throws DataSetException if dataset creation fails
   * @throws IOException if file reading fails
   */
  public ExcelPatternDataSet(final File file, final String... patternNames)
      throws DataSetException, IOException {
    try (final var inputStream = java.nio.file.Files.newInputStream(file.toPath())) {
      final var result = createTables(inputStream, patternNames);
      this.tables = result.tables;
      this.tableNames = result.names;
    }
  }

  /**
   * Creates pattern dataset from InputStream.
   *
   * @param inputStream Excel data stream
   * @param patternNames Array of pattern names to filter
   * @throws DataSetException if dataset creation fails
   * @throws IOException if stream reading fails
   */
  public ExcelPatternDataSet(final InputStream inputStream, final String... patternNames)
      throws DataSetException, IOException {
    final var result = createTables(inputStream, patternNames);
    this.tables = result.tables;
    this.tableNames = result.names;
  }

  /** Creates tables from Excel workbook. */
  private TableResult createTables(final InputStream inputStream, final String... patternNames)
      throws IOException {
    final var tableMap = new LinkedHashMap<String, ITable>();
    final var nameList = new ArrayList<String>();

    try (final var workbook = WorkbookFactory.create(inputStream)) {
      IntStream.range(0, workbook.getNumberOfSheets())
          .mapToObj(workbook::getSheetAt)
          .forEach(
              sheet -> {
                try {
                  final var table = new ExcelPatternTable(sheet, patternNames);
                  final var tableName = table.getTableMetaData().getTableName();
                  tableMap.put(tableName, table);
                  nameList.add(tableName);
                } catch (DataSetException e) {
                  throw new RuntimeException("Failed to create pattern table", e);
                }
              });
    }

    return new TableResult(tableMap, nameList);
  }

  @Override
  protected ITableIterator createIterator(final boolean reversed) throws DataSetException {
    return new OrderedTableIterator(tables, tableNames, reversed);
  }

  @Override
  public String[] getTableNames() throws DataSetException {
    return tableNames.toArray(new String[0]);
  }

  @Override
  public ITable getTable(final String tableName) throws DataSetException {
    final var table = tables.get(tableName);
    if (table == null) {
      throw new DataSetException("Table not found: " + tableName);
    }
    return table;
  }

  /**
   * Result container for table creation operations.
   *
   * <p>This record encapsulates the results of Excel workbook processing, containing both the
   * created tables mapped by name and the ordered list of table names for maintaining worksheet
   * order during iteration.
   *
   * @param tables map of table names to ITable instances, preserving insertion order
   * @param names ordered list of table names matching the original worksheet sequence
   */
  private record TableResult(SequencedMap<String, ITable> tables, List<String> names) {}

  /**
   * Ordered implementation of ITableIterator with support for reversed iteration.
   *
   * <p>This iterator maintains the original worksheet order from Excel files while providing the
   * ability to iterate in reverse order for cleanup operations. The iterator is stateful and tracks
   * the current position for sequential access.
   *
   * <p>Key features:
   *
   * <ul>
   *   <li>Preserves original Excel worksheet order
   *   <li>Supports both forward and reverse iteration
   *   <li>Thread-safe for single-threaded access patterns
   *   <li>Provides proper error handling for invalid state access
   * </ul>
   */
  private static final class OrderedTableIterator implements ITableIterator {
    private final SequencedMap<String, ITable> tables;
    private final List<String> tableNames;
    private int currentIndex = 0;

    public OrderedTableIterator(
        final SequencedMap<String, ITable> tables,
        final List<String> tableNames,
        final boolean reversed) {
      this.tables = tables;
      this.tableNames = new ArrayList<>(tableNames);

      if (reversed) {
        Collections.reverse(this.tableNames);
      }
    }

    @Override
    public boolean next() throws DataSetException {
      if (currentIndex < tableNames.size()) {
        currentIndex++;
        return true;
      }
      return false;
    }

    @Override
    public ITable getTable() throws DataSetException {
      return getCurrentTable();
    }

    @Override
    public org.dbunit.dataset.ITableMetaData getTableMetaData() throws DataSetException {
      return getCurrentTable().getTableMetaData();
    }

    private ITable getCurrentTable() throws DataSetException {
      validateCurrentIndex();
      final var tableName = tableNames.get(currentIndex - 1);
      final var table = tables.get(tableName);
      if (table == null) {
        throw new DataSetException("Table not found: " + tableName);
      }
      return table;
    }

    private void validateCurrentIndex() throws DataSetException {
      if (currentIndex <= 0 || currentIndex > tableNames.size()) {
        throw new DataSetException("No current table");
      }
    }
  }
}
