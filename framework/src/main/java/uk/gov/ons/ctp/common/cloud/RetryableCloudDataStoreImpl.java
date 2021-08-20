package uk.gov.ons.ctp.common.cloud;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

/**
 * Decorator for {@link CloudDataStore}. It is responsible for handling exponential backoffs when
 * the datastore is becoming overloaded.
 */
@Slf4j
@Service
public class RetryableCloudDataStoreImpl implements RetryableCloudDataStore {

  private CloudDataStore cloudDataStore;
  private Retrier retrier;

  @Autowired
  public RetryableCloudDataStoreImpl(CloudDataStore cloudDataStore, Retrier retrier) {
    this.cloudDataStore = cloudDataStore;
    this.retrier = retrier;
  }

  @Override
  public void storeObject(
      final String schema, final String key, final Object value, final String id)
      throws CTPException {
    try {
      retrier.store(schema, key, value);
    } catch (DataStoreContentionException e) {
      String identity = value.getClass().getSimpleName() + ": " + id;
      log.error(
          "Retries exhausted for storage",
          kv("key", key),
          kv("schema", schema),
          kv("indentity", identity),
          e);
      throw new CTPException(Fault.SYSTEM_ERROR, e, "Retries exhausted for storage of " + identity);
    }
  }

  @Override
  public <T> Optional<T> retrieveObject(Class<T> target, final String schema, final String key)
      throws CTPException {
    return cloudDataStore.retrieveObject(target, schema, key);
  }

  @Override
  public <T> List<T> search(
      Class<T> target, final String schema, String[] fieldPathElements, String searchValue)
      throws CTPException {
    return cloudDataStore.search(target, schema, fieldPathElements, searchValue);
  }

  @Override
  public Set<String> getCollectionNames() {
    return cloudDataStore.getCollectionNames();
  }

  /**
   * When attempts to retry object storage have been exhausted this method is invoked and it can
   * then throw the exception (possibly triggering calling code retries). If this is not done then
   * the message won't be eligible for another attempt or writing to the dead letter queue.
   *
   * @param e is the final exception in the storeObject retries.
   * @throws Exception the exception which caused the final attempt to fail.
   */
  @Recover
  public void doRecover(Exception e) throws Exception {
    log.debug("Datastore recovery throwing exception {}", e.getMessage());
    throw e;
  }

  /**
   * We need another class for the retryable annotation, since calling a retryable annotated within
   * the same class does not honour the annotations.
   */
  @Slf4j
  @Component
  static class Retrier {
    private CloudDataStore cloudDataStore;
    private RetryConfig retryConfig;

    @Autowired
    public Retrier(CloudDataStore cloudDataStore, RetryConfig retryConfig) {
      this.cloudDataStore = cloudDataStore;
      this.retryConfig = retryConfig;
      log.info("CloudDataStore retry configuration: {}", this.retryConfig);
    }

    @Retryable(
        label = "storeObject",
        include = DataStoreContentionException.class,
        backoff =
            @Backoff(
                delayExpression = "#{@retryConfig.getInitial()}",
                multiplierExpression = "#{@retryConfig.getMultiplier()}",
                maxDelayExpression = "#{@retryConfig.getMax()}"),
        maxAttemptsExpression = "#{@retryConfig.getMaxAttempts()}",
        listeners = "cloudRetryListener")
    public void store(final String schema, final String key, final Object value)
        throws CTPException, DataStoreContentionException {
      cloudDataStore.storeObject(schema, key, value);
    }
  }
}
