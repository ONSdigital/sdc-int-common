package uk.gov.ons.ctp.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.ctp.common.event.EventPublisherTestUtil.assertHeader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.common.event.persistence.FirestoreEventPersistence;

/**
 * EventPublisher tests specific to failure scenarios when running with event persistence enabled.
 */
@ExtendWith(MockitoExtension.class)
public class EventPublisherWithPersistenceTest {

  @InjectMocks private EventPublisher eventPublisher;
  @Mock private EventSender sender;
  @Mock private FirestoreEventPersistence eventPersistence;

  @Test
  public void eventPersistedWhenRabbitFails() throws CTPException {
    SurveyLaunchResponse surveyLaunchedResponse = loadJson(SurveyLaunchResponse[].class);

    ArgumentCaptor<SurveyLaunchEvent> eventCapture =
        ArgumentCaptor.forClass(SurveyLaunchEvent.class);

    Mockito.doThrow(new RuntimeException("Failed to send")).when(sender).sendEvent(any(), any());

    String transactionId =
        eventPublisher.sendEvent(
            EventType.SURVEY_LAUNCH, Source.RESPONDENT_HOME, Channel.RH, surveyLaunchedResponse);

    // Verify that the event was persistent following simulated Rabbit failure
    verify(eventPersistence, times(1))
        .persistEvent(eq(EventType.SURVEY_LAUNCH), eventCapture.capture());
    SurveyLaunchEvent event = eventCapture.getValue();
    assertHeader(event, transactionId, EventType.SURVEY_LAUNCH, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(surveyLaunchedResponse, event.getPayload().getResponse());
  }

  @Test
  public void exceptionThrownWhenRabbitAndFirestoreFail() throws CTPException {
    SurveyLaunchResponse surveyLaunchedResponse = loadJson(SurveyLaunchResponse[].class);

    Mockito.doThrow(new RuntimeException("Failed to send")).when(sender).sendEvent(any(), any());
    Mockito.doThrow(new CTPException(Fault.SYSTEM_ERROR, "Firestore broken"))
        .when(eventPersistence)
        .persistEvent(any(), any());

    Exception e =
        assertThrows(
            Exception.class,
            () ->
                eventPublisher.sendEvent(
                    EventType.SURVEY_LAUNCH,
                    Source.RESPONDENT_HOME,
                    Channel.RH,
                    surveyLaunchedResponse));
    assertTrue(
        e.getMessage().matches(".* event persistence failed following Rabbit failure"),
        e.getMessage());
  }

  private <T> T loadJson(Class<T[]> clazz) {
    return FixtureHelper.loadPackageFixtures(clazz).get(0);
  }
}
