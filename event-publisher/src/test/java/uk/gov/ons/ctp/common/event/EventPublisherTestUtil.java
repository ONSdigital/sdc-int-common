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

    assertEquals("0.5.0", header.getVersion());
    assertEquals(expectedTopic, header.getTopic());
    assertEquals(expectedSource.name(), header.getSource());
    assertEquals(expectedChannel, header.getChannel());
    assertThat(header.getDateTime(), instanceOf(Date.class));
    assertThat(header.getMessageId(), instanceOf(UUID.class));
    assertEquals(messageId, header.getMessageId().toString());
    assertThat(header.getCorrelationId(), instanceOf(UUID.class));
    assertEquals(messageId, header.getCorrelationId().toString());

    String expectedOriginatingUser;
    if (expectedSource == Source.CONTACT_CENTRE_API) {
      expectedOriginatingUser = "TBD";
    } else {
      expectedOriginatingUser = "RESPONDENT_HOME";
    }
    assertEquals(expectedOriginatingUser, header.getOriginatingUser());
  }
}
