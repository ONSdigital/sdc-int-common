package uk.gov.ons.ctp.integration.eqlaunch.service;

import static uk.gov.ons.ctp.common.event.model.CaseUpdate.ATTRIBUTE_ADDRESS_LINE_1;
import static uk.gov.ons.ctp.common.event.model.CaseUpdate.ATTRIBUTE_ADDRESS_LINE_2;
import static uk.gov.ons.ctp.common.event.model.CaseUpdate.ATTRIBUTE_ADDRESS_LINE_3;
import static uk.gov.ons.ctp.common.event.model.CaseUpdate.ATTRIBUTE_POSTCODE;
import static uk.gov.ons.ctp.common.event.model.CaseUpdate.ATTRIBUTE_TOWN_NAME;
import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bouncycastle.util.encoders.Hex;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JweEncryptor;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;

@Slf4j
public class EqLaunchService {
  private static final String ROLE_FLUSHER = "flusher";

  // TODO : FLEXIBLE CASE
  private Map<SurveyType, String[]> surveyTypeAddressAttribs = new HashMap<>();
  
  private static final String [] socialAddressAttribs = {
		  ATTRIBUTE_ADDRESS_LINE_1,
		  ATTRIBUTE_ADDRESS_LINE_2,
		  ATTRIBUTE_ADDRESS_LINE_3,
		  ATTRIBUTE_TOWN_NAME,
		  ATTRIBUTE_POSTCODE
  };
  private JweEncryptor codec;

  public EqLaunchService(KeyStore keyStore) throws CTPException {
    this.codec = new JweEncryptor(keyStore, "authentication");
    this.surveyTypeAddressAttribs.put(SurveyType.SOCIAL, socialAddressAttribs);
  }

  public String getEqLaunchJwe(EqLaunchData launchData) throws CTPException {
    EqLaunchCoreData coreLaunchData = launchData.coreCopy();

    Map<String, Object> payload =
        createPayloadMap(
            coreLaunchData,
            launchData.getSurveyType(),
            launchData.getCollectionExercise(),
            launchData.getCaseUpdate(),
            launchData.getUserId(),
            null,
            launchData.getAccountServiceUrl(),
            launchData.getAccountServiceLogoutUrl());

    return codec.encrypt(payload);
  }

  public String getEqFlushLaunchJwe(EqLaunchCoreData launchData) throws CTPException {

    Map<String, Object> payload =
        createPayloadMap(launchData, null, null, null, null, ROLE_FLUSHER, null, null);

    return codec.encrypt(payload);
  }

  /**
   * This method builds the payload of a URL that will be used to launch EQ. This code replicates
   * the payload building done by the Python code in the census-rh-ui project for class /app/eq.py.
   *
   * <p>EQ requires a payload string formatted as a Python serialised dictionary, so this code has
   * to replicate all Python formatting quirks.
   *
   * <p>This code assumes that the channel is CC or field, and will need the user_id field to be
   * cleared if it is ever used from RH.
   *
   * @param coreData                core launch data
   * @param caseContainer           case container
   * @param userId                  user id
   * @param role                    role
   * @param accountServiceUrl       service url
   * @param accountServiceLogoutUrl logout url
   * @return
   * @throws CTPException on error
   */
  Map<String, Object> createPayloadMap(
      EqLaunchCoreData coreData,
      SurveyType surveyType,
      CollectionExerciseUpdate collectionExercise,
      CaseUpdate caseUpdate,
      String userId,
      String role,
      String accountServiceUrl,
      String accountServiceLogoutUrl)
      throws CTPException {

    UUID caseId = UUID.fromString(caseUpdate.getCaseId());
    String questionnaireId = coreData.getUacUpdate().getQid();
    Source source = coreData.getSource();

    long currentTimeInSeconds = System.currentTimeMillis() / 1000;

    LinkedHashMap<String, Object> payload = new LinkedHashMap<>();

    payload.computeIfAbsent("jti", (k) -> UUID.randomUUID().toString());
    payload.computeIfAbsent("tx_id", (k) -> UUID.randomUUID().toString());
    payload.computeIfAbsent("iat", (k) -> currentTimeInSeconds);
    payload.computeIfAbsent("exp", (k) -> currentTimeInSeconds + (5 * 60));
    payload.computeIfAbsent(
          "collection_exercise_sid", (k) -> caseUpdate.getCollectionExerciseId());

    String convertedRegionCode = convertRegionCode(caseUpdate.getSample().get("region"));
    payload.computeIfAbsent("region_code", (k) -> convertedRegionCode);
    
    if (role == null || !role.equals(ROLE_FLUSHER)) {
      Objects.requireNonNull(
          caseUpdate, "CaseContainer mandatory unless role is '" + ROLE_FLUSHER + "'");

	  verifyNotNull(caseUpdate.getCollectionExerciseId(), "collection id", caseId);
	  verifyNotNull(questionnaireId, "questionnaireId", caseId);

      payload.computeIfAbsent("ru_ref", (k) -> questionnaireId);
      payload.computeIfAbsent("user_id", (k) -> userId);
      String caseIdStr = caseUpdate.getCaseId();
      payload.computeIfAbsent("case_id", (k) -> caseIdStr);
	  payload.computeIfAbsent("language_code", (k) -> coreData.getLanguage().getIsoLikeCode());
	  payload.computeIfAbsent("eq_id", (k) -> "9999");
	  payload.computeIfAbsent("period_id", (k) -> caseUpdate.getCollectionExerciseId());
	  payload.computeIfAbsent("form_type", (k) -> "zzz");
	  payload.computeIfAbsent("schema_name", (k) -> "zzz_9999");
	  payload.computeIfAbsent("period_str", (k) -> collectionExercise.getName());
	  payload.computeIfAbsent("survey_url", (k) -> coreData.getUacUpdate().getCollectionInstrumentUrl());
      payload.computeIfAbsent("case_ref", (k) -> caseUpdate.getCaseRef());
	  payload.computeIfAbsent("ru_name", (k) -> "West Efford Cottage");
				

	  String [] displayAddressAttribs = surveyTypeAddressAttribs.get(surveyType);
	  verifyNotNull(displayAddressAttribs, "displayAddress", caseId);
	  
	  payload.computeIfAbsent(
          "display_address",
          (k) ->
              buildDisplayAddress(caseUpdate, displayAddressAttribs));
    }
    String responseId = encryptResponseId(questionnaireId, coreData.getSalt());
    payload.computeIfAbsent("response_id", (k) -> responseId);
    payload.computeIfAbsent("account_service_url", (k) -> accountServiceUrl);
    payload.computeIfAbsent("account_service_log_out_url", (k) -> accountServiceLogoutUrl);
    payload.computeIfAbsent("channel", (k) -> coreData.getChannel().name().toLowerCase());
    payload.computeIfAbsent("questionnaire_id", (k) -> questionnaireId);


    log.debug("Payload for EQ", kv("payload", payload));

    return payload;
  }

  private void verifyNotNull(Object fieldValue, String fieldName, UUID caseId) throws CTPException {
    if (fieldValue == null) {
      throw new CTPException(
          Fault.VALIDATION_FAILED,
          "No value supplied for " + fieldName + " field of case " + caseId);
    }
  }

  private String convertRegionCode(String caseRegionStr) throws CTPException {
    String regionValue = "GB-ENG";

    if (caseRegionStr != null) {
      char caseRegion = caseRegionStr.charAt(0);
      if (caseRegion == 'N') {
        regionValue = "GB-NIR";
      } else if (caseRegion == 'W') {
        regionValue = "GB-WLS";
      } else if (caseRegion == 'E') {
        regionValue = "GB-ENG";
      }
    }

    return regionValue;
  }

  // Create an address from the first 2 non-null parts of the address.
  // This replicates RHUI's creation of the display address.
  private String buildDisplayAddress(CaseUpdate caseUpdate, String... addressElements) {
    String displayAddress =
        Arrays.stream(addressElements)
            .map(a -> caseUpdate.getSample().get(a))
            .filter(a -> a != null)
            .limit(2)
            .collect(Collectors.joining(", "));
    return displayAddress;
  }

  // Creates encrypted response id from SALT and questionnaireId
  private String encryptResponseId(String questionnaireId, String salt) throws CTPException {
    StringBuilder responseId = new StringBuilder(questionnaireId);
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(salt.getBytes());
      byte[] bytes = md.digest(questionnaireId.getBytes());
      responseId.append((new String(Hex.encode(bytes)).substring(0, 16)));
    } catch (NoSuchAlgorithmException ex) {
      log.error(
          "No SHA-256 algorithm while encrypting questionnaire",
          kv("questionnaireId", questionnaireId),
          ex);
      throw new CTPException(Fault.SYSTEM_ERROR, ex);
    }
    return responseId.toString();
  }
}
