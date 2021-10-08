package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyUpdate implements EventPayload {

  // surveyId is a UUID but is represented as a String so it can be stored correctly in Firestore
  private String surveyId;

  private String name;
}
