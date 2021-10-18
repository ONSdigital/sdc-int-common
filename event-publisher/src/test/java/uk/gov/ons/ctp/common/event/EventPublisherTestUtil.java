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
import uk.gov.ons.ctp.common.event.model.Header;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventPublisherTestUtil {

  static void assertHeader(
      GenericEvent event,
      String messageId,
      EventTopic expectedTopic,
      Source expectedSource,
      Channel expectedChannel) {
    
    Header header = event.getHeader();
    
    assertEquals("v0.3_RELEASE", header.getVersion());
    assertEquals(expectedTopic, header.getTopic());
    assertEquals(expectedSource, header.getSource());
    assertEquals(expectedChannel, header.getChannel());
    assertThat(header.getDateTime(), instanceOf(Date.class));
    assertThat(header.getMessageId(), instanceOf(UUID.class));
    assertEquals(messageId, header.getMessageId().toString());
    assertThat(header.getCorrelationId(), instanceOf(UUID.class));
    assertEquals(messageId, header.getCorrelationId().toString());
    assertEquals("TBD", header.getOriginatingUser());  
  }
}
