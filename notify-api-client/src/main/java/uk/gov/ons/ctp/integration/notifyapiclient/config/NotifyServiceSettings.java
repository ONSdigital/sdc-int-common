package uk.gov.ons.ctp.integration.notifyapiclient.config;

import lombok.Data;
import uk.gov.ons.ctp.common.rest.RestClientConfig;

@Data
public class NotifyServiceSettings {
  private RestClientConfig restClientConfig;
}
