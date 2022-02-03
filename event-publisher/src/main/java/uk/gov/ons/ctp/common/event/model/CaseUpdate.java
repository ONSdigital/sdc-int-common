package uk.gov.ons.ctp.common.event.model;

import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseUpdate implements EventPayload {

  // Key names for expected sample attributes
  // TODO : FLEXIBLE CASE
  public static String ATTRIBUTE_QUESTIONNAIRE = "questionnaire";
  public static String ATTRIBUTE_SAMPLE_UNIT_REF = "sampleUnitRef";
  public static String ATTRIBUTE_COHORT = "cohort";
  public static String ATTRIBUTE_ADDRESS_LINE_1 = "addressLine1";
  public static String ATTRIBUTE_ADDRESS_LINE_2 = "addressLine2";
  public static String ATTRIBUTE_ADDRESS_LINE_3 = "addressLine3";
  public static String ATTRIBUTE_TOWN_NAME = "townName";
  public static String ATTRIBUTE_POSTCODE = "postcode";
  public static String ATTRIBUTE_REGION = "region";
  public static String ATTRIBUTE_GOR9D = "gor9d";
  public static String ATTRIBUTE_LA_CODE = "laCode";
  public static String ATTRIBUTE_UPRN = "uprn";
  public static String ATTRIBUTE_UPRN_LATITUDE = "uprnLatitude";
  public static String ATTRIBUTE_UPRN_LONGITUDE = "uprnLongitude";

  /*
   caseId surveyId collectionExerciseId are all UUIDs however they are represented
   as Strings so they can be correctly Stored in Firestore
  */
  private String caseId;
  private String surveyId;
  private String collectionExerciseId;
  private boolean invalid;
  private String refusalReceived;
  private Map<String, String> sample;
  private Map<String, String> sampleSensitive;
  private String caseRef;
  private Date createdAt;
  private Date lastUpdatedAt;
}
