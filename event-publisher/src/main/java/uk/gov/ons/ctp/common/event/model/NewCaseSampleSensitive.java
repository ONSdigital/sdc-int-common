package uk.gov.ons.ctp.common.event.model;

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
  private String childDob; // "2001-12-31", // TODO: PMB

  private String additionalInfo;

  @LoggingScope(scope = Scope.MASK)
  private String childMobileNumber;

  @LoggingScope(scope = Scope.MASK)
  private String childEmailAddress;

  @LoggingScope(scope = Scope.MASK)
  private String parentMobileNumber;

  @LoggingScope(scope = Scope.MASK)
  private String parentEmailAddress;
}
