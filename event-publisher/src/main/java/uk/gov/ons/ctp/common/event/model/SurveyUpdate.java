package uk.gov.ons.ctp.common.event.model;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.SurveyType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyUpdate implements EventPayload {

  // surveyId is a UUID but is represented as a String so it can be stored correctly in Firestore
  private String surveyId;

  private String name;

  private String sampleDefinitionUrl;

  @JsonRawValue private String sampleDefinition;

  private Map<String, Object> metadata;

  private List<SurveyFulfilment> allowedPrintFulfilments;

  private List<SurveyFulfilment> allowedSmsFulfilments;

  private List<SurveyFulfilment> allowedEmailFulfilments;

  @JsonSetter("sampleDefinition")
  void setSampleDefinitionFromJson(JsonNode data) {
    this.sampleDefinition = data.toString();
  }

  public SurveyType surveyType() {
    return SurveyType.fromSampleDefinitionUrl(sampleDefinitionUrl);
  }
}
