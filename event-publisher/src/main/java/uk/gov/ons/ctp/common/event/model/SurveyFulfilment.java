package uk.gov.ons.ctp.common.event.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFulfilment {

  private String packCode;

  private String description;

  private Map<String, Object> metadata;
}
