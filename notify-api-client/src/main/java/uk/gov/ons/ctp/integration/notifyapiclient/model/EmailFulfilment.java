package uk.gov.ons.ctp.integration.notifyapiclient.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/** The request DTO to send to RM Notify API to request an email fulfilment
 * 
 * @author philwhiles
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailFulfilment {

  private UUID caseId;
  private String email;
  private String packCode;
  //use of metadata TBD
  private Object uacMetadata;
}
