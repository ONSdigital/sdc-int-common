package uk.gov.ons.ctp.integration.notifyapiclient.model;

import lombok.Data;

@Data
public class NotifyRequestDTO {
  private NotifyRequestHeaderDTO header;
  private NotifyRequestPayloadDTO payload;
}
