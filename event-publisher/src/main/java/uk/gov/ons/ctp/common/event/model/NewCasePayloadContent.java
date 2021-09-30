package uk.gov.ons.ctp.common.event.model;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCasePayloadContent implements EventPayload {

  private UUID caseId;
  private UUID collectionExerciseId;

  private NewCaseSample sample = new NewCaseSample();
  private NewCaseSampleSensitive sampleSensitive = new NewCaseSampleSensitive();
}
