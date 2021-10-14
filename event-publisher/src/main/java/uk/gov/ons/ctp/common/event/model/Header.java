package uk.gov.ons.ctp.common.event.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.event.EventTopic;
import uk.gov.ons.ctp.common.jackson.CustomDateSerialiser;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Header {

  private String version;

  private EventTopic topic;

  private String source;

  private Channel channel;

  @JsonSerialize(using = CustomDateSerialiser.class)
  private Date dateTime;

  private UUID messageId;

  private UUID correlationId;

  private String originatingUser;
}
