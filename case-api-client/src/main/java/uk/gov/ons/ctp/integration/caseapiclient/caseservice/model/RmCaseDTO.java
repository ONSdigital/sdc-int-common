package uk.gov.ons.ctp.integration.caseapiclient.caseservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import uk.gov.ons.ctp.common.domain.RefusalType;

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
