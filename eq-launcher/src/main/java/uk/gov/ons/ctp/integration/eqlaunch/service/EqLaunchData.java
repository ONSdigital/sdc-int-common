package uk.gov.ons.ctp.integration.eqlaunch.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
public class EqLaunchData extends EqLaunchCoreData {
  @NonNull private CaseContainerDTO caseContainer;
  @NonNull private String userId;
  private String accountServiceUrl;
  private String accountServiceLogoutUrl;
}
