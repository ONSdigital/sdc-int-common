package uk.gov.ons.ctp.common.event.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCaseSampleSensitive {

  private String firstName;
  private String lastName;

  private String childFirstName;
  private String childMiddleNames;
  private String childLastName;
  private LocalDate childDob;

  @LoggingScope(scope = Scope.SENSITIVE)
  private String mobileNumber;

  @LoggingScope(scope = Scope.SENSITIVE)
  private String emailAddress;
}
