package uk.gov.ons.ctp.common.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;

/** EventPublisher tests specific to the scenario in which event persistence is turned off. */
@ExtendWith(MockitoExtension.class)
public class EventPublisherWithoutPersistanceTest {

  @InjectMocks private EventPublisher eventPublisher;
  @Mock private EventSender sender;

  @Test
  public void eventSendingFailsWithException() throws CTPException {
    SurveyLaunchResponse surveyLaunchedResponse = loadJson(SurveyLaunchResponse[].class);

    Mockito.doThrow(new RuntimeException("Failed to send")).when(sender).sendEvent(any(), any());

    Exception e =
        assertThrows(
            Exception.class,
            () -> {
              eventPublisher.sendEvent(
                  TopicType.SURVEY_LAUNCH,
                  Source.RESPONDENT_HOME,
                  Channel.RH,
                  surveyLaunchedResponse);
            });
    assertTrue(e.getMessage().matches("Failed to publish .*"), e.getMessage());
  }

  private <T> T loadJson(Class<T[]> clazz) {
    return FixtureHelper.loadPackageFixtures(clazz).get(0);
  }
}
