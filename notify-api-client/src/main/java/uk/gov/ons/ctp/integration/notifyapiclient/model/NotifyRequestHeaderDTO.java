package uk.gov.ons.ctp.integration.notifyapiclient.model;

import java.util.UUID;

import lombok.Data;

@Data
public class NotifyRequestHeaderDTO {
  private String source;

  private String channel;

  private UUID correlationId;

  private String originatingUser;
}
