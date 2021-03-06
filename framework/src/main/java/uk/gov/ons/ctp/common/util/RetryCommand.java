package uk.gov.ons.ctp.common.util;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;

/**
 * A class which allows for the repeated execution of a provided lambda. If the lambda throws an
 * exception the RetryCommand sleeps, and attempts again, until the max retries is exceeded, upon
 * which it will throw the exception to the caller.
 *
 * @param <T> the type that the lambda will supply as return
 */
@Slf4j
public class RetryCommand<T> {

  public static final String ERROR_HANDLER_ERROR =
      "FAILED - Command aborted on advice of errorHandler";
  public static final String MAX_RETRIES_EXCEEDED = "Max retries exceeded";

  private int maxRetries;
  private int retryPause;

  /**
   * Step 1 - create the command up front with its retry and sleep config params
   *
   * @param maxRetries how many times should we try?
   * @param retryPause how long should we sleep when we catch an exception?
   */
  public RetryCommand(int maxRetries, int retryPause) {
    this.maxRetries = maxRetries;
    this.retryPause = retryPause;
  }

  /**
   * Step 2 - run the lambda Sleep and Retry when the call fails, until we meet the max retry value
   *
   * <p>Run the supplied Supplier and always retry on any failure. To be more selective about
   * whether to retry or not, use the overloaded run(Supplier, Predicate)
   *
   * @param function the thing to run
   * @return the value returned from the lambda
   * @throws CTPException if issues
   */
  public T run(Supplier<T> function) throws CTPException {
    return run(
        function,
        new Predicate<Exception>() {
          public boolean test(Exception ex) {
            return true;
          }
        });
  }

  /**
   * Step 2 - run the lambda Sleep and Retry when the call fails, until we meet the max retry value
   *
   * @param function the lambda that is the doing the work we wish to retry
   * @param errorHandler error handler
   * @return the value returned from the lambda
   * @throws CTPException if issues
   */
  public T run(Supplier<T> function, Predicate<Exception> errorHandler) throws CTPException {
    int retryCount = 0;
    T response = null;

    while (retryCount < maxRetries) {
      try {
        response = function.get();
        break;
      } catch (Exception ex) {
        if (errorHandler.test(ex)) {
          retryCount++;
          log.warn(
              "FAILED - Command failed on retry",
              kv("retry_count", retryCount),
              kv("max_retries", maxRetries));
          if (retryCount >= maxRetries) {
            log.error(
                MAX_RETRIES_EXCEEDED,
                kv("retry_count", retryCount),
                kv("max_retries", maxRetries),
                ex);
            throw new CTPException(CTPException.Fault.SYSTEM_ERROR, ex, MAX_RETRIES_EXCEEDED);
          }

          try {
            Thread.sleep(retryPause);
          } catch (InterruptedException ie) {
            log.warn(
                "Unexpected retry pause interrupted, in the interests of resilience, carrying on.");
          }
        } else {
          log.error(ERROR_HANDLER_ERROR, ex);
          throw new CTPException(CTPException.Fault.SYSTEM_ERROR, ex, ERROR_HANDLER_ERROR);
        }
      }
    }

    return response;
  }
}
