package uk.gov.ons.ctp.integration.caseapiclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import lombok.Data;

/** Application Config bean */
@EnableRetry
@Configuration
@ConfigurationProperties
@Data
public class CaseApiAppConfig {
  private CaseServiceSettings caseServiceSettings;
}
