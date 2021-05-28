package uk.gov.ons.ctp.integration.caseapiclient.caseservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseContainerDTO {

  private String caseRef;

  private String arid;

  private String estabArid;

  private String estabType;

  private String uprn;

  private String estabUprn;

  private Date createdDateTime;

  private String addressLine1;

  private String addressLine2;

  private String addressLine3;

  private String townName;

  private String postcode;

  private String organisationName;

  private String addressLevel;

  private String abpCode;

  private String latitude;

  private String longitude;

  private String oa;

  private String lsoa;

  private String msoa;

  private String lad;

  private List<EventDTO> caseEvents;

  private UUID id;

  private UUID collectionExerciseId;

  private String caseType;

  private String addressType;

  private String region;

  private String surveyType;

  private boolean handDelivery;

  private boolean secureEstablishment;
}
