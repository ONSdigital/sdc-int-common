package uk.gov.ons.ctp.integration.caseapiclient.caseservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RmCaseDTO {

  private String caseRef;

  private Date createdDateTime;

  private Date lastUpdated;

  private List<EventDTO> caseEvents;

  private UUID id;

  boolean invalid;

  RefusalType refusalReceived;

  Map<String, String> sample;

}
