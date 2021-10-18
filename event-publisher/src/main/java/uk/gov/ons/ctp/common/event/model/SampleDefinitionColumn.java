package uk.gov.ons.ctp.common.event.model;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SampleDefinitionColumn {

  private String columnName;
  private List<Map<String, Object>> rules;
}
