package uk.gov.ons.ctp.integration.caseapiclient.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/** Application Config bean */
@EnableRetry
@Configuration
@ConfigurationProperties
@Data
public class CaseApiAppConfig {
  private CaseServiceSettings caseServiceSettings;
  private Logging logging;
}
