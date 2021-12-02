package uk.gov.ons.ctp.common.domain;

public enum SurveyType {
  SOCIAL("social.json"),
  SIS("sis.json");

  private String pattern;

  private SurveyType(String pattern) {
    this.pattern = pattern;
  }

  public String getPattern() {
    return pattern;
  }

  public static SurveyType fromSurveyDefinitionUrl(String url) {
    for (SurveyType type : values()) {
      if (url != null && url.endsWith(type.pattern)) {
        return type;
      }
    }
    return null;
  }
}
