package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UAC implements EventPayload {
  private String uacHash;
  private String active;
  private String questionnaireId;
  private String caseId;
  private String formType;
}
