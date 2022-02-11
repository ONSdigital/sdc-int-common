package uk.gov.ons.ctp.integration.notifyapiclient.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class NotifyRequestPayloadDTO {
  private SmsFulfilment smsFulfilment;
  private EmailFulfilment emailFulfilment;
}
