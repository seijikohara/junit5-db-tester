package com.github.seijikohara.junit5dbtester.dataset.excel;

import com.github.seijikohara.junit5dbtester.dataset.PatternTable;
import com.google.common.base.Strings;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultTableMetaData;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.jspecify.annotations.Nullable;

/**
 * Excel-based pattern table implementation with sophisticated pattern filtering capabilities.
 * Supports pattern-based row filtering for selective data loading from Excel sheets.
 */
public final class ExcelPatternTable extends PatternTable {

  private static final String PATTERN_MARKER = "[Pattern]";

  private final ITableMetaData metaData;
  private final Data data;

  /**
   * Internal data structure for managing table data with column indexing.
   *
   * <p>This class provides efficient storage and retrieval of tabular data by maintaining both
   * column name-to-index mappings and row data. It supports dynamic row addition and provides O(1)
   * column lookup performance through pre-computed indexes.
   *
   * <p>Key features:
   *
   * <ul>
   *   <li>Column name-based data access with index mapping
   *   <li>Immutable row storage using defensive copying
   *   <li>Ordered column management through TreeMap
   *   <li>Safe null handling in data retrieval
   * </ul>
   *
   * <p>Data is organized as:
   *
   * <pre>{@code
   * Column Names: ["id", "name", "email"]
   * Column Indexes: {id: 0, name: 1, email: 2}
   * Row Data: [[1, "John", "john@..."], [2, "Jane", "jane@..."]]
   * }</pre>
   */
  protected static final class Data {
    private final Map<String, Integer> columnIndexes;
    private final List<List<Object>> data = new ArrayList<>();

    /**
     * Creates a new Data instance with the specified column names.
     *
     * @param columns the list of column names
     */
    public Data(final List<String> columns) {
      columnIndexes =
          IntStream.range(0, columns.size())
              .boxed()
              .collect(
                  Collectors.toMap(columns::get, index -> index, (i1, i2) -> i2, TreeMap::new));
    }

    /**
     * Adds a new row of data.
     *
     * @param rowData the collection of row data values
     */
    public void addRow(final Collection<Object> rowData) {
      data.add(List.copyOf(rowData));
    }

    /**
     * Gets the value at the specified row and column.
     *
     * @param rowIndex the row index (0-based)
     * @param columnName the column name
     * @return an Optional containing the value, or empty if not found
     */
    public Optional<Object> getValue(final int rowIndex, final String columnName) {
      final var rowData = data.get(rowIndex);
      final var columnIndex = columnIndexes.get(columnName);
      return Optional.ofNullable(columnIndex).map(rowData::get);
    }

    /**
     * Returns the list of column names.
     *
     * @return the list of column names
     */
    public List<String> getColumns() {
      return List.copyOf(columnIndexes.keySet());
    }

    /**
     * Returns the number of rows in the data.
     *
     * @return the row count
     */
    public int getRowCount() {
      return data.size();
    }

    @Override
    public String toString() {
      return super.toString() + data;
    }
  }

  /**
   * Creates a pattern table from an Excel sheet with specified pattern names.
   *
   * @param sheet Excel sheet to process
   * @param patternNames Array of pattern names to filter (empty means all patterns)
   * @throws DataSetException if data processing fails
   */
  public ExcelPatternTable(final Sheet sheet, final String... patternNames)
      throws DataSetException {
    final var sheetName = sheet.getSheetName();
    final var rowCount = sheet.getLastRowNum();

    // Column header initialization
    if (rowCount >= 0 && sheet.getRow(0) != null) {
      metaData = createMetaData(sheetName, sheet.getRow(0));
    } else {
      metaData = new DefaultTableMetaData(sheetName, List.<Column>of().toArray(new Column[0]));
    }

    // Data loading with pattern filtering
    data = loadData(sheet, patternNames);
  }

  /** Loads data from Excel sheet with pattern-based filtering. */
  Data loadData(final Sheet sheet, final String... patternNames) throws DataSetException {
    final var processor = createRowProcessor(sheet, patternNames);
    return processor.processAllRows();
  }

  /** Creates a row processor for the given sheet and pattern names. */
  private RowProcessor createRowProcessor(final Sheet sheet, final String... patternNames)
      throws DataSetException {
    final var columns = metaData.getColumns();
    final var columnNames = Arrays.stream(columns).map(Column::getColumnName).toList();
    return new RowProcessor(sheet, columnNames, patternNames);
  }

  /** Internal processor for handling sheet rows with pattern filtering. */
  private final class RowProcessor {
    private final Sheet sheet;
    private final Data data;
    private final Set<String> patternNameSet;
    private final boolean patterned;
    private final int columnIndexOffset;
    private final int columnCount;
    private final AtomicReference<String> currentPatternName;

    private RowProcessor(
        final Sheet sheet, final List<String> columnNames, final String... patternNames)
        throws DataSetException {
      this.sheet = sheet;
      this.data = new Data(columnNames);
      this.patternNameSet = Arrays.stream(patternNames).collect(Collectors.toSet());
      this.patterned = isPatternedSheet(sheet);
      this.columnIndexOffset = patterned ? 1 : 0;
      this.columnCount = metaData.getColumns().length;
      this.currentPatternName = new AtomicReference<>("!_DUMMY_!");
    }

    private Data processAllRows() {
      final var rowCount = sheet.getLastRowNum();
      IntStream.rangeClosed(1, rowCount)
          .mapToObj(sheet::getRow)
          .filter(Objects::nonNull)
          .forEach(this::processRow);
      return data;
    }

    private void processRow(final Row row) {
      if (!shouldIncludeRow(row)) {
        return;
      }
      final var rowData = extractRowData(row);
      data.addRow(rowData);
    }

    private boolean shouldIncludeRow(final Row row) {
      if (!patterned) {
        return true;
      }
      updateCurrentPattern(row);
      return patternNameSet.isEmpty() || patternNameSet.contains(currentPatternName.get());
    }

    private void updateCurrentPattern(final Row row) {
      extractPatternName(row)
          .filter(name -> !Strings.isNullOrEmpty(name))
          .ifPresent(currentPatternName::set);
    }

    private Optional<String> extractPatternName(final Row row) {
      return Optional.ofNullable(row.getCell(0))
          .filter(cell -> cell.getCellType() == CellType.STRING)
          .map(Cell::getStringCellValue);
    }

    private List<Object> extractRowData(final Row row) {
      final var rowIndex = row.getRowNum();
      return IntStream.range(columnIndexOffset, columnIndexOffset + columnCount)
          .mapToObj(columnIndex -> getCellValueSafely(rowIndex, columnIndex).orElse(null))
          .toList();
    }

    private Optional<Object> getCellValueSafely(final int rowIndex, final int columnIndex) {
      try {
        return getValue(sheet, rowIndex, columnIndex);
      } catch (final DataSetException e) {
        throw new RuntimeException(
            "Failed to get cell value at row=" + rowIndex + ", column=" + columnIndex, e);
      }
    }
  }

  /** Determines if a sheet uses pattern-based filtering by checking for pattern marker. */
  boolean isPatternedSheet(final Sheet sheet) {
    return Optional.ofNullable(sheet.getRow(0))
        .filter(row -> row.getLastCellNum() >= 1)
        .map(row -> row.getCell(0))
        .filter(cell -> cell.getCellType() == CellType.STRING)
        .map(Cell::getStringCellValue)
        .map(value -> Objects.equals(PATTERN_MARKER, value))
        .orElse(false);
  }

  /** Creates table metadata from column header row. */
  ITableMetaData createMetaData(final String tableName, final Row columnHeaderRow) {
    final var columnList =
        IntStream.range(0, columnHeaderRow.getLastCellNum())
            .mapToObj(columnHeaderRow::getCell)
            .filter(Objects::nonNull)
            .filter(cell -> cell.getCellType() == CellType.STRING)
            .map(Cell::getStringCellValue)
            .map(String::trim)
            .filter(columnName -> !Objects.equals(columnName, PATTERN_MARKER))
            .filter(columnName -> !columnName.isEmpty())
            .map(columnName -> new Column(columnName, DataType.UNKNOWN))
            .toList();

    return new DefaultTableMetaData(tableName, columnList.toArray(new Column[0]));
  }

  /**
   * Extracts value from Excel cell with comprehensive type handling.
   *
   * @param sheet the Excel sheet to extract value from
   * @param rowIndex the row index (0-based)
   * @param columnIndex the column index (0-based)
   * @return an Optional containing the extracted value, or empty if cell is null/empty
   * @throws DataSetException if there's an error extracting the cell value
   */
  public Optional<Object> getValue(final Sheet sheet, final int rowIndex, final int columnIndex)
      throws DataSetException {
    return getCell(sheet, rowIndex, columnIndex)
        .map(
            (Cell cell) -> {
              try {
                return CellValueExtractor.extractValue(cell, rowIndex, columnIndex);
              } catch (DataSetException e) {
                throw new RuntimeException(e);
              }
            })
        .flatMap(opt -> opt);
  }

  /** Gets cell from sheet at specified coordinates. */
  private Optional<Cell> getCell(final Sheet sheet, final int rowIndex, final int columnIndex) {
    return Optional.ofNullable(sheet.getRow(rowIndex)).map(row -> row.getCell(columnIndex));
  }

  /**
   * Utility class for extracting values from Excel cells with comprehensive type handling.
   *
   * <p>This class provides type-safe extraction of values from Apache POI Cell objects, handling
   * the complexity of Excel's type system and converting values to appropriate Java types for
   * database operations.
   *
   * <p>Supported Excel cell types:
   *
   * <ul>
   *   <li><strong>NUMERIC:</strong> Converts to appropriate numeric types (Integer, Double,
   *       BigDecimal)
   *   <li><strong>STRING:</strong> Returns as String value
   *   <li><strong>BOOLEAN:</strong> Returns as Boolean value
   *   <li><strong>BLANK:</strong> Returns empty Optional
   *   <li><strong>FORMULA:</strong> Not supported, throws exception
   *   <li><strong>ERROR:</strong> Not supported, throws exception
   * </ul>
   *
   * <p>Special numeric handling:
   *
   * <ul>
   *   <li>Date values are detected and converted to java.sql.Timestamp
   *   <li>Time-only values are converted to java.sql.Time
   *   <li>Integer values are preserved as Integer when appropriate
   *   <li>Large numbers are handled as BigDecimal for precision
   * </ul>
   *
   * <p>This class is stateless and thread-safe, using only static methods for value extraction
   * operations.
   */
  @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  private static final class CellValueExtractor {

    /** Extracts value from cell based on cell type. */
    static Optional<Object> extractValue(final Cell cell, final int rowIndex, final int columnIndex)
        throws DataSetException {
      return switch (cell.getCellType()) {
        case NUMERIC -> extractNumericValue(cell);
        case STRING -> Optional.of(cell.getStringCellValue());
        case FORMULA ->
            throw createDataTypeException("Formula not supported", rowIndex, columnIndex);
        case BLANK -> Optional.empty();
        case BOOLEAN -> Optional.of(cell.getBooleanCellValue());
        case ERROR -> throw createDataTypeException("Error", rowIndex, columnIndex);
        default -> throw createDataTypeException("Unsupported type", rowIndex, columnIndex);
      };
    }

    /** Extracts numeric value, handling both dates and numbers. */
    private static Optional<Object> extractNumericValue(final Cell cell) {
      if (DateUtil.isCellDateFormatted(cell)) {
        return Optional.of(extractDateValue(cell));
      }
      return Optional.of(extractPlainNumericValue(cell));
    }

    /** Extracts date/time value from cell, handling both DATE and TIME types. */
    private static Object extractDateValue(final Cell cell) {
      final var numericValue = cell.getNumericCellValue();
      final var javaDate = DateUtil.getJavaDate(numericValue);

      // Check if this is a pure time value (no date component)
      if (isTimeOnlyValue(javaDate)) {
        final var localDateTime =
            javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return Time.valueOf(localDateTime.toLocalTime());
      }

      return Timestamp.from(javaDate.toInstant());
    }

    /** Determines if a Date represents a time-only value (date is epoch base date 1899-12-31). */
    private static boolean isTimeOnlyValue(final Date date) {
      final var localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      // Excel's base date for time-only values is 1899-12-31 (Excel's epoch for TIME values)
      return localDate.equals(LocalDate.of(1899, 12, 31));
    }

    /** Extracts plain numeric value from cell. */
    private static BigDecimal extractPlainNumericValue(final Cell cell) {
      final var cellValue = cell.getNumericCellValue();
      final var formatString = cell.getCellStyle().getDataFormatString();
      return formatNumericValue(cellValue, formatString);
    }

    /** Formats numeric value according to cell format. */
    private static BigDecimal formatNumericValue(
        final double cellValue, final @Nullable String formatString) {
      return Optional.ofNullable(formatString)
          .filter(format -> !format.equals("General"))
          .filter(format -> !format.equals("@"))
          .map(format -> formatWithDecimalFormat(cellValue, format))
          .orElse(toBigDecimal(cellValue));
    }

    /** Formats value using DecimalFormat. */
    private static BigDecimal formatWithDecimalFormat(final double cellValue, final String format) {
      try {
        final var symbols = createDecimalFormatSymbols();
        final var decimalFormat = new DecimalFormat(format, symbols);
        final var resultString = decimalFormat.format(cellValue);
        return new BigDecimal(resultString);
      } catch (final NumberFormatException e) {
        return toBigDecimal(cellValue);
      }
    }

    /** Creates decimal format symbols with dot as decimal separator. */
    private static DecimalFormatSymbols createDecimalFormatSymbols() {
      final var symbols = new DecimalFormatSymbols();
      symbols.setDecimalSeparator('.');
      return symbols;
    }

    /** Converts double to BigDecimal with proper formatting. */
    private static BigDecimal toBigDecimal(final double cellValue) {
      final var cellValueString = String.valueOf(cellValue);
      // Remove trailing .0 for integral numbers
      return new BigDecimal(
          cellValueString.endsWith(".0")
              ? cellValueString.substring(0, cellValueString.length() - 2)
              : cellValueString);
    }

    /** Creates a DataTypeException with location information. */
    private static DataTypeException createDataTypeException(
        final String message, final int rowIndex, final int columnIndex) {
      return new DataTypeException(
          message + " at rowIndex=" + rowIndex + ", columnIndex=" + columnIndex);
    }
  }

  // ITable interface implementation

  @Override
  public int getRowCount() {
    return data.getRowCount();
  }

  @Override
  public ITableMetaData getTableMetaData() {
    return metaData;
  }

  @Override
  public @Nullable Object getValue(final int row, final String column) throws DataSetException {
    assertValidRowIndex(row);
    return data.getValue(row, column).orElse(null);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "[name="
        + getTableMetaData().getTableName()
        + ",rows="
        + getRowCount()
        + "]";
  }
}
