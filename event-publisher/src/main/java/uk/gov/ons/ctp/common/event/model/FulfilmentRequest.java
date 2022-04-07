package uk.gov.ons.ctp.common.event.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FulfilmentRequest implements EventPayload {

  private String caseId;
  private String packCode;
  private Map<String, Object> uacMetadata;
  private Map<String, Object> personalisation;
}
