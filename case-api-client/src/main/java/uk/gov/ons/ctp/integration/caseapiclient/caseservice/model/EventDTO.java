package uk.gov.ons.ctp.integration.caseapiclient.caseservice.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDTO {

  private String id;

  private String eventType;

  private String description;

  private LocalDateTime createdDateTime;
}
