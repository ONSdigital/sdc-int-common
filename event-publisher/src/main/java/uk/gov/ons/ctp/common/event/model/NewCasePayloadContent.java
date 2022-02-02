package uk.gov.ons.ctp.common.event.model;

import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCasePayloadContent implements EventPayload {

  //TODO : FLEXIBLE CASE
  // Sample attributes
  public static final String ATTRIBUTE_SCHOOL_ID = "schoolId";
  public static final String ATTRIBUTE_SCHOOL_NAME = "schoolName";
  public static final String ATTRIBUTE_CONSENT_GIVEN_TEST = "consentGivenTest";
  public static final String ATTRIBUTE_CONSENT_GIVEN_SURVEY = "consentGivenSurvey";

  // Sample sensitive attributes
  public static final String ATTRIBUTE_FIRST_NAME = "firstName";
  public static final String ATTRIBUTE_LAST_NAME = "lastName";
  public static final String ATTRIBUTE_CHILD_FIRST_NAME = "childFirstName";
  public static final String ATTRIBUTE_CHILD_MIDDLE_NAMES = "childMiddleNames";
  public static final String ATTRIBUTE_CHILD_LAST_NAME = "childLastName";
  public static final String ATTRIBUTE_CHILD_DOB = "childDob";
  public static final String ATTRIBUTE_MOBILE_NUMBER = "mobileNumber";
  public static final String ATTRIBUTE_EMAIL_ADDRESS = "emailAddress";

  private UUID caseId;
  private UUID collectionExerciseId;

  private Map<String, String> sample;
  private Map<String, String> sampleSensitive;
}
