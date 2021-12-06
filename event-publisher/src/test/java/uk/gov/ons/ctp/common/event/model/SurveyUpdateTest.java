package uk.gov.ons.ctp.common.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.SurveyType;

public class SurveyUpdateTest {

  private SurveyUpdate surveyUpdate;

  @BeforeEach
  public void setup() {
    surveyUpdate = FixtureHelper.loadPackageFixtures(SurveyUpdate[].class).get(0);
  }

  @Test
  public void shouldIdentifySocialType() {
    surveyUpdate.setSampleDefinitionUrl("https://some.domain/path1/path2/social.json");
    assertEquals(SurveyType.SOCIAL, surveyUpdate.surveyType());
  }

  @Test
  public void shouldIdentifySisType() {
    surveyUpdate.setSampleDefinitionUrl("https://some.domain/path1/path2/sis.json");
    assertEquals(SurveyType.SIS, surveyUpdate.surveyType());
  }

  @Test
  public void shouldNotRecogniseRandomUrlSuffix() {
    surveyUpdate.setSampleDefinitionUrl("https://some.domain/path1/path2/random.json");
    assertNull(surveyUpdate.surveyType());
  }

  @Test
  public void shouldNotRecogniseNullSampleDefinitionUrl() {
    surveyUpdate.setSampleDefinitionUrl(null);
    assertNull(surveyUpdate.surveyType());
  }
}
