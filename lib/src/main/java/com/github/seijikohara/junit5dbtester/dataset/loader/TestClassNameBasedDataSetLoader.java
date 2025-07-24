package com.github.seijikohara.junit5dbtester.dataset.loader;

import com.github.seijikohara.junit5dbtester.annotation.DataSet;
import com.github.seijikohara.junit5dbtester.annotation.Expectation;
import com.github.seijikohara.junit5dbtester.annotation.Preparation;
import com.github.seijikohara.junit5dbtester.context.DatabaseTesterContext;
import com.github.seijikohara.junit5dbtester.dataset.PatternDataSet;
import com.github.seijikohara.junit5dbtester.dataset.excel.ExcelPatternDataSet;
import com.google.common.base.Strings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.dbunit.dataset.DataSetException;
import org.jspecify.annotations.Nullable;

/**
 * Convention-based dataset loader that resolves Excel files based on test class names. Supports
 * pattern-based data filtering and multiple datasource configurations.
 */
public final class TestClassNameBasedDataSetLoader implements DataSetLoader {

  private static final String CLASSPATH_PREFIX = "classpath:";

  /** Creates a new instance of TestClassNameBasedDataSetLoader. */
  public TestClassNameBasedDataSetLoader() {
    // Default constructor
  }

  @Override
  public Collection<PatternDataSet> loadPreparationDataSets(
      final DatabaseTesterContext context, final Class<?> testClass, final Method testMethod) {
    return Optional.ofNullable(testMethod.getAnnotation(Preparation.class))
        .map(Preparation::dataSets)
        .map(
            dataSetAnnotations ->
                loadDataSets(context, dataSetAnnotations, testClass, testMethod, ""))
        .orElse(List.of());
  }

  @Override
  public Collection<PatternDataSet> loadExpectationDataSets(
      final DatabaseTesterContext context, final Class<?> testClass, final Method testMethod) {
    return Optional.ofNullable(testMethod.getAnnotation(Expectation.class))
        .map(Expectation::dataSets)
        .map(
            dataSetAnnotations ->
                loadDataSets(
                    context,
                    dataSetAnnotations,
                    testClass,
                    testMethod,
                    context.getExpectFileSuffix()))
        .orElse(List.of());
  }

  /** Loads datasets from annotations with pattern filtering and datasource assignment. */
  private Collection<PatternDataSet> loadDataSets(
      final DatabaseTesterContext context,
      final DataSet[] dataSetAnnotations,
      final Class<?> testClass,
      final Method testMethod,
      final String suffix) {
    final var processor = new DataSetProcessor(context, testClass, testMethod, suffix);
    return Arrays.stream(dataSetAnnotations).map(processor::createPatternDataSet).toList();
  }

  /**
   * Processor for creating pattern datasets from DataSet annotations.
   *
   * <p>This inner class encapsulates the complex logic for converting DataSet annotations into
   * PatternDataSet instances, handling file resolution, pattern filtering, and DataSource
   * configuration.
   *
   * <p>Processing workflow:
   *
   * <ol>
   *   <li>Resolve Excel file location using convention-based or explicit paths
   *   <li>Create ExcelPatternDataSet with specified pattern filters
   *   <li>Configure DataSource association for multi-database testing
   *   <li>Apply naming conventions and suffix transformations
   * </ol>
   *
   * <p>The processor supports:
   *
   * <ul>
   *   <li>Convention-based file naming (TestClassName.xlsx)
   *   <li>Explicit file path specifications
   *   <li>Pattern-based data filtering
   *   <li>Multiple DataSource configurations
   *   <li>File suffix transformations for expectation datasets
   * </ul>
   */
  private final class DataSetProcessor {
    private final DatabaseTesterContext context;
    private final Class<?> testClass;
    private final Method testMethod;
    private final String suffix;

    private DataSetProcessor(
        final DatabaseTesterContext context,
        final Class<?> testClass,
        final Method testMethod,
        final String suffix) {
      this.context = context;
      this.testClass = testClass;
      this.testMethod = testMethod;
      this.suffix = suffix;
    }

    private PatternDataSet createPatternDataSet(final DataSet annotation) {
      try {
        final var file = resolveFile(annotation.resourceLocation(), testClass, suffix);
        final var patternNames = resolvePatternNames(annotation.patternNames());
        final var dataSet = loadDataSet(file, patternNames);
        configureDataSource(dataSet, annotation.dataSourceName());
        return dataSet;
      } catch (final FileNotFoundException e) {
        throw new IllegalStateException("Failed to load dataset file", e);
      }
    }

    private List<String> resolvePatternNames(final String[] annotationPatternNames) {
      return Optional.ofNullable(annotationPatternNames)
          .filter(names -> names.length > 0)
          .map(List::of)
          .orElseGet(() -> List.of(testMethod.getName()));
    }

    private void configureDataSource(final PatternDataSet dataSet, final String dataSourceName) {
      dataSet.setDataSource(context.getDataSource(dataSourceName));
    }
  }

  /** Resolves Excel file location using convention-based naming or explicit resource location. */
  File resolveFile(
      final @Nullable String resourceLocation, final Class<?> testClass, final String suffix)
      throws FileNotFoundException {
    final var resolver = new FileLocationResolver(testClass);
    return resolver.resolveFile(resourceLocation, suffix);
  }

  /**
   * Resolver for Excel file locations supporting both classpath and file system paths.
   *
   * <p>This record provides unified file resolution logic that handles both explicit resource
   * locations and convention-based file naming. It supports classpath resources and
   * absolute/relative file system paths with intelligent fallback mechanisms.
   *
   * <p>Resolution strategy:
   *
   * <ol>
   *   <li>If explicit location provided, resolve directly (classpath: prefix or file path)
   *   <li>If no location specified, use convention-based naming (TestClassName + suffix)
   *   <li>Try multiple file extensions (.xlsx, .xls) for maximum compatibility
   *   <li>Search in test class package location for classpath resources
   * </ol>
   *
   * <p>Supported location formats:
   *
   * <ul>
   *   <li><strong>Classpath:</strong> {@code classpath:data/test-data.xlsx}
   *   <li><strong>Absolute:</strong> {@code /path/to/test-data.xlsx}
   *   <li><strong>Relative:</strong> {@code test-data.xlsx}
   *   <li><strong>Convention:</strong> {@code null} → {@code TestClassName.xlsx}
   * </ul>
   *
   * <p>The resolver automatically handles file suffix transformations for expectation datasets
   * (e.g., adding "-expected" suffix) and provides detailed error messages when files cannot be
   * located.
   *
   * @param testClass the test class used for convention-based naming and package resolution
   */
  private record FileLocationResolver(Class<?> testClass) {
    File resolveFile(final @Nullable String resourceLocation, final String suffix)
        throws FileNotFoundException {
      final var effectiveLocation = determineEffectiveLocation(resourceLocation, suffix);
      return createFileFromLocation(effectiveLocation);
    }

    private String determineEffectiveLocation(
        final @Nullable String resourceLocation, final String suffix) {
      return !Strings.isNullOrEmpty(resourceLocation) ? resourceLocation : findExcelFile(suffix);
    }

    private File createFileFromLocation(final String location) throws FileNotFoundException {
      if (location.startsWith(CLASSPATH_PREFIX)) {
        return resolveClasspathFile(location);
      }
      return resolveFileSystemFile(location);
    }

    private File resolveClasspathFile(final String location) throws FileNotFoundException {
      final var resourcePath = location.substring(CLASSPATH_PREFIX.length());
      final var resourceUrl = testClass.getClassLoader().getResource(resourcePath);
      if (resourceUrl == null) {
        throw new FileNotFoundException("Resource not found: " + resourcePath);
      }
      return new File(resourceUrl.getFile());
    }

    private File resolveFileSystemFile(final String location) throws FileNotFoundException {
      final var file = new File(location);
      if (!file.exists()) {
        throw new FileNotFoundException("File not found: " + location);
      }
      return file;
    }

    /** Find Excel file with .xlsx or .xls extension based on test class name. */
    private String findExcelFile(final String suffix) {
      final var basePath = createBasePath(suffix);
      return findFirstExistingFormat(basePath);
    }

    private String createBasePath(final String suffix) {
      return CLASSPATH_PREFIX + testClass.getName().replace('.', '/') + suffix;
    }

    private String findFirstExistingFormat(final String basePath) {
      final var xlsxPath = basePath + ".xlsx";
      if (resourceExists(xlsxPath)) {
        return xlsxPath;
      }
      return basePath + ".xls";
    }

    private boolean resourceExists(final String resourcePath) {
      final var resourcePathWithoutPrefix = resourcePath.substring(CLASSPATH_PREFIX.length());
      return testClass.getClassLoader().getResource(resourcePathWithoutPrefix) != null;
    }
  }

  /** Loads pattern dataset from Excel file with specified pattern names. */
  PatternDataSet loadDataSet(final File file, final List<String> patternNames) {
    try {
      return new ExcelPatternDataSet(file, patternNames.toArray(new String[0]));
    } catch (DataSetException | IOException e) {
      throw new IllegalStateException("Failed to load Excel dataset: " + file.getAbsolutePath(), e);
    }
  }
}
