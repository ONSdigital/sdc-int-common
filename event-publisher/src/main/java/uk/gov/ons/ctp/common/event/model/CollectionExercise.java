package uk.gov.ons.ctp.common.event.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.jackson.CustomDateSerialiser;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionExercise implements EventPayload {
  // collectionExerciseId and surveyId are UUIDs but are represented as Strings so they can be
  // correctly saved in Firestore
  private String collectionExerciseId;
  private String surveyId;
  private String name;
  private String reference;

  @JsonSerialize(using = CustomDateSerialiser.class)
  private Date startDate;

  @JsonSerialize(using = CustomDateSerialiser.class)
  private Date endDate;

  private CollectionExerciseMetadata metadata;
}
