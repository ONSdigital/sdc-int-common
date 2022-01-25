package uk.gov.ons.ctp.common.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Language {
  WELSH("cy"),
  ENGLISH("en");

  private String isoLikeCode;

  private static final Map<String, Language> languageByIsoLikeCode =
      Arrays.stream(Language.values())
          .collect(Collectors.toMap(Language::getIsoLikeCode, Function.identity()));

  private Language(String isoLikeCode) {
    this.isoLikeCode = isoLikeCode;
  }

  public String getIsoLikeCode() {
    return isoLikeCode;
  }

  /**
   * Discover the Language enum for a given iso code.
   *
   * @param isoLikeCode is the isoLikeCode for which the caller wants the Language enum.
   * @return the Language enum, or null if the isoLikeCode doesn't correspond to a currently known
   *     language.
   */
  public static Language lookup(String isoLikeCode) {
    return languageByIsoLikeCode.get(isoLikeCode);
  }
}
