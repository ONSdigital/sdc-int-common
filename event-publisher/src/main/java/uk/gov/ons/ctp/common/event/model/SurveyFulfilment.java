package uk.gov.ons.ctp.common.event.model;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFulfilment {

  private String packCode;

  private String description;

  private HashMap<String, Object> metadata;
}
