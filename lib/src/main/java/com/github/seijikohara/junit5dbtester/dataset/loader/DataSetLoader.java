package com.github.seijikohara.junit5dbtester.dataset.loader;

import com.github.seijikohara.junit5dbtester.context.DatabaseTesterContext;
import com.github.seijikohara.junit5dbtester.dataset.PatternDataSet;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Interface for loading pattern-based datasets for database testing. Provides methods to load both
 * preparation and expectation datasets.
 */
public interface DataSetLoader {

  /**
   * Loads preparation datasets for setting up test data.
   *
   * @param context The database tester context
   * @param testClass The test class containing the test method
   * @param testMethod The test method being executed
   * @return Collection of pattern datasets for preparation
   * @throws FileNotFoundException if dataset files cannot be found
   */
  Collection<PatternDataSet> loadPreparationDataSets(
      DatabaseTesterContext context, Class<?> testClass, Method testMethod)
      throws FileNotFoundException;

  /**
   * Loads expectation datasets for validating test results.
   *
   * @param context The database tester context
   * @param testClass The test class containing the test method
   * @param testMethod The test method being executed
   * @return Collection of pattern datasets for expectation validation
   * @throws FileNotFoundException if dataset files cannot be found
   */
  Collection<PatternDataSet> loadExpectationDataSets(
      DatabaseTesterContext context, Class<?> testClass, Method testMethod)
      throws FileNotFoundException;
}
