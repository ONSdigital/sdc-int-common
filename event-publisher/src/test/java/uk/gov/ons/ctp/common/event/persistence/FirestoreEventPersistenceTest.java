package uk.gov.ons.ctp.common.event.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.FulfilmentEvent;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

@ExtendWith(MockitoExtension.class)
public class FirestoreEventPersistenceTest {

  private ObjectMapper objectMapper = new CustomObjectMapper();

  @InjectMocks private FirestoreEventPersistence persistence;

  @Mock RetryableCloudDataStore cloudDataStore;

  @BeforeEach
  public void setup() {
    ReflectionTestUtils.setField(persistence, "gcpProject", "testing");
    ReflectionTestUtils.setField(persistence, "eventBackupSchemaName", "backupcollection");
    ReflectionTestUtils.setField(persistence, "objectMapper", objectMapper);
    persistence.init();
  }

  @Test
  public void testPersistEvent() throws Exception {
    long startTime = System.currentTimeMillis();

    FulfilmentEvent event = FixtureHelper.loadClassFixtures(FulfilmentEvent[].class).get(0);

    ArgumentCaptor<EventBackupData> eventBackupCapture =
        ArgumentCaptor.forClass(EventBackupData.class);

    persistence.persistEvent(TopicType.UAC_AUTHENTICATION, event);

    String expectedMessageId = event.getHeader().getMessageId().toString();
    Mockito.verify(cloudDataStore, times(1))
        .storeObject(
            eq("testing-backupcollection"),
            eq(expectedMessageId),
            eventBackupCapture.capture(),
            eq(expectedMessageId));

    EventBackupData storedData = eventBackupCapture.getValue();
    assertEquals(TopicType.UAC_AUTHENTICATION, storedData.getTopicType());
    assertTrue(storedData.getMessageFailureDateTimeInMillis() >= startTime, storedData.toString());
    assertTrue(
        storedData.getMessageFailureDateTimeInMillis() <= System.currentTimeMillis(),
        storedData.toString());
    assertNull(storedData.getMessageSentDateTimeInMillis(), storedData.toString());
    assertEquals(expectedMessageId, storedData.getId());

    String eventJson = storedData.getEvent();

    FulfilmentEvent sentEvent = objectMapper.readValue(eventJson, FulfilmentEvent.class);

    assertEquals(event, sentEvent);
  }
}
