package uk.gov.ons.ctp.integration.notifyapiclient.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/** Application Config bean */
@EnableRetry
@Configuration
@ConfigurationProperties
@Data
public class NotifyApiAppConfig {
  private NotifyServiceSettings caseServiceSettings;
  private Logging logging;
}
