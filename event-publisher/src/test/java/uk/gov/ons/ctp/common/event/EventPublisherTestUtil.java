package uk.gov.ons.ctp.common.event;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventPublisherTestUtil {

  static void assertHeader(
      GenericEvent event,
      String messageId,
      EventTopic expectedTopic,
      Source expectedSource,
      Channel expectedChannel) {
    assertEquals(messageId, event.getHeader().getMessageId().toString());
    assertThat(event.getHeader().getMessageId(), instanceOf(UUID.class));
    assertEquals(expectedTopic, event.getHeader().getTopic());
    assertEquals(expectedSource.name(), event.getHeader().getSource());
    assertEquals(expectedChannel, event.getHeader().getChannel());
    assertThat(event.getHeader().getDateTime(), instanceOf(Date.class));
    // PMB: extend checks
  }
}
