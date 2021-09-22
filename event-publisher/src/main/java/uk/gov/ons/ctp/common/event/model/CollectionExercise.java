package uk.gov.ons.ctp.common.event.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.jackson.CustomDateSerialiser;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionExercise implements EventPayload {
  private String id;
  private UUID surveyId;
  private String name;
  private String reference;

  @JsonSerialize(using = CustomDateSerialiser.class)
  private Date startDate;

  @JsonSerialize(using = CustomDateSerialiser.class)
  private Date endDate;

  private Map<String, String> collectionExerciseMetadata;

}
