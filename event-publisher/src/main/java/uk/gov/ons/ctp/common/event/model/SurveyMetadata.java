package uk.gov.ons.ctp.common.event.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyMetadata {

  private List<String> suitableRegions;
}
