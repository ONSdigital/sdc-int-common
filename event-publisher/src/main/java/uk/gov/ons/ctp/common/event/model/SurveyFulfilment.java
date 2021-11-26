package uk.gov.ons.ctp.common.event.model;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFulfilment {

  private String packCode;

  private String description;

  @JsonRawValue private String metadata;
  
  @JsonSetter("metadata")
  void setMetadataFromJson(JsonNode data) {
    this.metadata = data.toString();
  }
}
