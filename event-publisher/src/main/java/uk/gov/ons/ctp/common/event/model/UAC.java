package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UAC implements EventPayload {
  @LoggingScope(scope = Scope.MASK)
  private String uacHash;

  private String active;
  private String questionnaireId;
  private String caseId;
  private String formType;
}
