package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseUpdate implements EventPayload {

  private String caseId;
  private String surveyId;
  private String collectionExerciseId;
  private boolean invalid;
  private String refusalReceived;
  private Sample sample;

  @LoggingScope(scope = Scope.MASK)
  private SampleSensitive sampleSensitive;
}
