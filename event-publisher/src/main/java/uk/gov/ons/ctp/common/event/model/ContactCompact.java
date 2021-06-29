package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactCompact {

  @LoggingScope(scope = Scope.MASK)
  private String title;

  @LoggingScope(scope = Scope.MASK)
  private String forename;

  @LoggingScope(scope = Scope.MASK)
  private String surname;
}
