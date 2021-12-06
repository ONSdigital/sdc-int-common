package uk.gov.ons.ctp.common.domain;

public enum SurveyType {
  SOCIAL("social"),
  SIS("sis");

  private String basename;
  private String suffix;

  private SurveyType(String basename) {
    this.basename = basename;
    this.suffix = basename + ".json";
  }

  public String getBasename() {
    return basename;
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
