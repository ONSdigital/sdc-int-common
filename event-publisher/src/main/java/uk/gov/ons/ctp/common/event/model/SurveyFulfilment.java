package uk.gov.ons.ctp.common.event.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFulfilment {

  private String packCode;

  private String description;

  private Map<String, ?> metadata;
  
  @JsonSetter("metadata")
  void setMetadataFromJson(JsonNode data) {
    String dataAsStr = data.asText();

    ObjectMapper mapper = new ObjectMapper();
    this.metadata = mapper.convertValue(data, new TypeReference<Map<String, Object>>(){});
  }
}
