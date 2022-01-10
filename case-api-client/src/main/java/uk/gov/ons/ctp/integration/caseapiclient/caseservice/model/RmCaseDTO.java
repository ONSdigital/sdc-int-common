package uk.gov.ons.ctp.integration.caseapiclient.caseservice.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RmCaseDTO {

  private String caseRef;

  private LocalDateTime createdDateTime;

  private LocalDateTime lastUpdated;

  private List<EventDTO> caseEvents;

  private UUID id;

  boolean invalid;

  RefusalType refusalReceived;

  Map<String, String> sample;

}
