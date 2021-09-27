package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCaseSample {

  private String schoolId;
  private String schoolName;
  private boolean consentGivenTest;
  private boolean consentGivenSurvey;
}
