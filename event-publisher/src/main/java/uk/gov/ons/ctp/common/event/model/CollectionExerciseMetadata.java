package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionExerciseMetadata {

  private Integer numberOfWaves;
  private Integer waveLength;
  private Integer cohorts;
  private Integer cohortSchedule;
}
