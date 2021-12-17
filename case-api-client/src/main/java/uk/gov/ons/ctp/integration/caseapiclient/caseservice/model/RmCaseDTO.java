package uk.gov.ons.ctp.integration.caseapiclient.caseservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RmCaseDTO {

  private String caseRef;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private LocalDateTime createdDateTime;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private LocalDateTime lastUpdated;

  private List<EventDTO> caseEvents;

  private UUID id;

  boolean invalid;

  RefusalType refusalReceived;

  Map<String, String> sample;

}
