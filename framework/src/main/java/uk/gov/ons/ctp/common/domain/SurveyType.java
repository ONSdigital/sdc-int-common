package uk.gov.ons.ctp.common.domain;

public enum SurveyType {
  SOCIAL("social"),
  SIS("sis");

  private String suffix;

  private SurveyType(String pattern) {
    this.suffix = pattern + ".json";
  }

  public static SurveyType fromSampleDefinitionUrl(String url) {
    for (SurveyType type : values()) {
      if (url != null && url.endsWith(type.suffix)) {
        return type;
      }
    }
    return null;
  }
}
