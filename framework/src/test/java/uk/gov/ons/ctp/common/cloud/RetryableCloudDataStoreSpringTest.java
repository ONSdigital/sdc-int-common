package uk.gov.ons.ctp.common.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

@EnableRetry
@EnableConfigurationProperties
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {RetryableCloudDataStoreImpl.class, CloudRetryListener.class, RetryConfig.class})
@TestPropertySource(
    properties = {
      "cloud-storage.backoff.initial=10",
      "cloud-storage.backoff.multiplier=1.2",
      "cloud-storage.backoff.max=300",
      "cloud-storage.backoff.max-attempts=3",
    })
public class RetryableCloudDataStoreSpringTest extends CloudTestBase {

  @MockBean private CloudDataStore cloudDataStore;

  @Autowired private RetryableCloudDataStore retryDataStore;

  @Test
  public void shouldAutowire() {
    assertNotNull(retryDataStore);
  }

  @Test
  public void shouldStore() throws Exception {
    retryDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1, "a case");
    verify(cloudDataStore).storeObject(eq(TEST_SCHEMA), eq(CASE1.getId()), eq(CASE1));
  }

  @Test
  public void shouldRetryStoreOnce() throws Exception {
    doThrow(new DataStoreContentionException("argh", new Exception()))
        .doNothing()
        .when(cloudDataStore)
        .storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);

    retryDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1, "a case");
    verify(cloudDataStore, times(2)).storeObject(eq(TEST_SCHEMA), eq(CASE1.getId()), eq(CASE1));
  }

  @Test
  public void shouldRetryStoreTwice() throws Exception {
    doThrow(new DataStoreContentionException("argh", new Exception()))
        .doThrow(new DataStoreContentionException("argh", new Exception()))
        .doNothing()
        .when(cloudDataStore)
        .storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);

    retryDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1, "a case");
    verify(cloudDataStore, times(3)).storeObject(eq(TEST_SCHEMA), eq(CASE1.getId()), eq(CASE1));
  }

  @Test
  public void shouldRetryWithTillExhaustion() throws Exception {
    doThrow(new DataStoreContentionException("argh", new Exception()))
        .when(cloudDataStore)
        .storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);
    try {
      retryDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1, "a case");
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.SYSTEM_ERROR, e.getFault());
      assertEquals("Retries exhausted for storage of DummyCase: a case", e.getMessage());
    }
    verify(cloudDataStore, times(3)).storeObject(eq(TEST_SCHEMA), eq(CASE1.getId()), eq(CASE1));
  }
}
