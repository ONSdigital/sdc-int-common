package uk.gov.ons.ctp.common.pubsub;

import lombok.Getter;

@Getter
public enum SubscriptionSuffix {
  RH("_rh"),
  CC("_cc"),
  CUC("_cuc");

  private String suffix;

  SubscriptionSuffix(String suffix) {
    this.suffix = suffix;
  }
}
