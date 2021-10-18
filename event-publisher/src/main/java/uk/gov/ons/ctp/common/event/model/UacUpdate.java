package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UacUpdate implements EventPayload {

  private String caseId;

  private String active;

  @LoggingScope(scope = Scope.MASK)
  private String uacHash;

  private String qid;

  private boolean receiptReceived;

  private WaveMetadata metadata;

  private boolean eqLaunched;
}
