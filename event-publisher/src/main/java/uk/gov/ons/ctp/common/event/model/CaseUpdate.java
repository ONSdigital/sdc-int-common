package uk.gov.ons.ctp.common.event.model;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseUpdate implements EventPayload {

  /*
   caseId surveyId collectionExerciseId are all UUIDs however they are represented
   as Strings so they can be correctly Stored in Firestore
  */
  private String caseId;
  private String surveyId;
  private String collectionExerciseId;
  private boolean invalid;
  private String refusalReceived;
  private CaseUpdateSample sample;
  private CaseUpdateSampleSensitive sampleSensitive;
  private String caseRef;
  private Date createdAt;
  private Date lastUpdatedAt;
}
