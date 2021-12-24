package uk.gov.ons.ctp.integration.caseapiclient.caseservice.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/** This class holds data for a newly allocated questionnaire ID. i
 *  The RM API currently returns the associated UAC, which we do not need, never did, and may soon be removed by RM
 *  so it not declared here to avoid later breakage.
 *  RM will also return an arbitrary 'uacMetadata' and until its content and use is determined, is also not declared here
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelephoneCaptureDTO {
  private UUID caseId;
  private String qId;
}
