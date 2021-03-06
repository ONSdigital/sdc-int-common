package uk.gov.ons.ctp.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.ctp.common.event.EventPublisherTestUtil.assertHeader;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.model.EqLaunch;
import uk.gov.ons.ctp.common.event.model.EqLaunchEvent;
import uk.gov.ons.ctp.common.event.persistence.FirestoreEventPersistence;

/** EventPublisher tests with circuit breaker */
@ExtendWith(MockitoExtension.class)
public class EventPublisherWithCircuitBreakerTest {

  @InjectMocks private EventPublisher eventPublisher;
  @Mock private EventSender sender;
  @Mock private FirestoreEventPersistence eventPersistence;
  @Mock private CircuitBreaker circuitBreaker;

  @Captor private ArgumentCaptor<EqLaunchEvent> eqLaunchedEventCaptor;

  private void mockCircuitBreakerRun() {
    doAnswer(
            new Answer<Object>() {
              @SuppressWarnings("unchecked")
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Supplier<Object> runner = (Supplier<Object>) args[0];
                return runner.get();
              }
            })
        .when(circuitBreaker)
        .run(any(), any());
  }

  private void mockCircuitBreakerFail() {
    doAnswer(
            new Answer<Object>() {
              @SuppressWarnings("unchecked")
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Supplier<Object> runner = (Supplier<Object>) args[0];
                Function<Throwable, Object> fallback = (Function<Throwable, Object>) args[1];

                try {
                  runner.get();
                } catch (Exception e) {
                  fallback.apply(e);
                }
                return null;
              }
            })
        .when(circuitBreaker)
        .run(any(), any());
  }

  @Test
  public void shouldSendEventThroughCircuitBreaker() throws Exception {
    mockCircuitBreakerRun();
    EqLaunch eqLaunch = loadJson(EqLaunch[].class);

    UUID messageId =
        eventPublisher.sendEvent(TopicType.EQ_LAUNCH, Source.RESPONDENT_HOME, Channel.RH, eqLaunch);

    EventTopic eventTopic = EventTopic.forType(TopicType.EQ_LAUNCH);
    verify(sender, times(1)).sendEvent(eq(eventTopic), eqLaunchedEventCaptor.capture());
    EqLaunchEvent event = eqLaunchedEventCaptor.getValue();
    assertHeader(
        event, messageId.toString(), EventTopic.EQ_LAUNCH, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(eqLaunch, event.getPayload().getEqLaunch());

    // since it succeeded, the event is NOT sent to firestore
    verify(eventPersistence, never()).persistEvent(eq(TopicType.EQ_LAUNCH), any());
  }

  @Test
  public void shouldNotPublishThroughCircuitBreakerWhenPubsubFails() throws Exception {
    mockCircuitBreakerFail();
    Mockito.doThrow(new RuntimeException("Publish fail")).when(sender).sendEvent(any(), any());

    EqLaunch eqLaunchedResponse = loadJson(EqLaunch[].class);

    eventPublisher.sendEvent(
        TopicType.EQ_LAUNCH, Source.RESPONDENT_HOME, Channel.RH, eqLaunchedResponse);

    EventTopic eventTopic = EventTopic.forType(TopicType.EQ_LAUNCH);
    verify(sender).sendEvent(eq(eventTopic), eqLaunchedEventCaptor.capture());

    // since it failed, the event is sent to firestore
    verify(eventPersistence).persistEvent(eq(TopicType.EQ_LAUNCH), any());
  }

  private <T> T loadJson(Class<T[]> clazz) {
    return FixtureHelper.loadPackageFixtures(clazz).get(0);
  }
}
