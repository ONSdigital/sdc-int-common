package uk.gov.ons.ctp.common.event.model;

import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCasePayloadContent implements EventPayload {

  //TODO : FLEXIBLE CASE
  
  private UUID caseId;
  private UUID collectionExerciseId;

  private Map<String, String> sample;
  private Map<String, String> sampleSensitive;
}
