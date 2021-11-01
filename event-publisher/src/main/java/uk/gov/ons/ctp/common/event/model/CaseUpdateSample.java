package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseUpdateSample {
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;
  private String region;
  private String uprn;

  private String questionnaire;
  private String sampleUnitRef;
  private int cohort;
  private String gor9d;
  private String laCode;
  private String uprnLatitude;
  private String uprnLongitude;
}
