package uk.gov.ons.ctp.common.utility;

public class ParallelTestLocks {
  // This string should be used as a ResourceLock value when running Spring tests.
  // Standardising on the same key forces JUnit to run Spring tests sequentially.
  public static final String SPRING_TEST = "Spring-test";
}
