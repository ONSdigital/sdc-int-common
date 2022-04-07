package uk.gov.ons.ctp.common.event.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvalidCase implements EventPayload {

  private UUID caseId;
  private String reason;
}
