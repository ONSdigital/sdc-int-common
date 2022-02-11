package uk.gov.ons.ctp.integration.notifyapiclient.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.Data;

/**
 * The request DTO to send to RM Notify API to request an SMS fulfilment
 *
 * @author philwhiles
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsFulfilment {

  private UUID caseId;
  private String phoneNumber;
  private String packCode;
  // use of metadata TBD
  private Object uacMetadata;
}
