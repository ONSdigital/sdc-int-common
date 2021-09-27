package uk.gov.ons.ctp.common.event.model;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.jackson.CustomDateSerialiser;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Header {

  private String version;

  private EventType topic;
  
  private Source source;

  private Channel channel;
  
  @JsonSerialize(using = CustomDateSerialiser.class)
  private Date dateTime;
  
  private String messageId;  // TODO: PMB Make a UUID?
  private String correlationId;  // TODO: PMB Make a UUID?

  private String originatingUser;
}
