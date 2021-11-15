package uk.gov.ons.ctp.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.common.reflect.ClassPath;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.lang.reflect.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.concurrent.ListenableFuture;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

@ExtendWith(MockitoExtension.class)
public class PubSubEventSenderTest {

  @Mock PubSubTemplate template;

  private PubSubEventSender sender;

  private ObjectMapper mapper = new CustomObjectMapper();

  private int count;
  private static final EventTopic ANY_TOPIC = EventTopic.CASE_UPDATE;
  private static final int ANY_TIMEOUT = 3;

  @Captor private ArgumentCaptor<PubsubMessage> pubsubMsgCaptor;

  @BeforeEach
  public void setup() {
    sender = new PubSubEventSender(template, ANY_TIMEOUT);

    ListenableFuture<String> futureAck = new AsyncResult<String>("ACK");
    lenient().when(template.publish(any(), any())).thenReturn(futureAck);
  }

  /**
   * Loop over all the model Event items, and for each one test that the fixture JSON file matches
   * that sent.
   *
   * <p>The test will fail on mismatches in expectation of types (eg boolean to string implicit
   * conversion). See <b>SOCINT-243</b> for description of problem.
   *
   * <p>Note: this test requires that each fixture JSON is NOT in an array, since the {@link
   * FixtureHelper#loadPackageObjectNode} method expects that restriction. It also requires a
   * fixture JSON file for each and every subclass of {@link GenericEvent}.
   *
   * @throws Exception on test failure
   */
  @Test
  public void shouldSendAllEventsAsExpected() throws Exception {
    ClassPath cp = ClassPath.from(this.getClass().getClassLoader());
    var classInfos = cp.getTopLevelClasses(this.getClass().getPackageName() + ".model");
    for (var inf : classInfos) {
      String className = inf.getSimpleName();
      if (className.endsWith("Event") && !className.equals("GenericEvent")) {
        @SuppressWarnings("unchecked")
        var clazz = (Class<? extends GenericEvent>) inf.load();
        count++;
        verifyEventSentIsExpectedJson(clazz);
      }
    }
  }

  private <T extends GenericEvent> void verifyEventSentIsExpectedJson(Class<T> clazz)
      throws Exception {
    @SuppressWarnings("unchecked")
    var arrClazz = (Class<T[]>) Array.newInstance(clazz, 0).getClass();

    GenericEvent event = FixtureHelper.loadPackageFixtures(arrClazz).get(0);

    sender.sendEvent(ANY_TOPIC, event);

    verify(template, times(count)).publish(eq(ANY_TOPIC.getTopic()), pubsubMsgCaptor.capture());

    PubsubMessage pubsubMessage = pubsubMsgCaptor.getValue();
    ByteString data = pubsubMessage.getData();
    String json = data.toStringUtf8();

    JsonNode node = mapper.readTree(json);
    var origNode = FixtureHelper.loadPackageObjectNode(clazz.getSimpleName());

    assertEquals(
        origNode, node, "Failed to send expected JSON for class: " + clazz.getSimpleName());
  }
}
