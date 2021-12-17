package uk.gov.ons.ctp.integration.eqlaunch.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RmCaseDTO;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JweEncryptor;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchCoreData;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

@Slf4j
public class EqLaunchServiceImpl implements EqLaunchService {
  private static final String ROLE_FLUSHER = "flusher";
  private JweEncryptor codec;

  public EqLaunchServiceImpl(KeyStore keyStore) throws CTPException {
    this.codec = new JweEncryptor(keyStore, "authentication");
  }

  @Override
  public String getEqLaunchJwe(EqLaunchData launchData) throws CTPException {
    EqLaunchCoreData coreLaunchData = launchData.coreCopy();

    Map<String, Object> payload =
        createPayloadMap(
            coreLaunchData,
            launchData.getCaseContainer(),
            launchData.getUserId(),
            null,
            launchData.getAccountServiceUrl(),
            launchData.getAccountServiceLogoutUrl());

    return codec.encrypt(payload);
  }

  @Override
  public String getEqFlushLaunchJwe(EqLaunchCoreData launchData) throws CTPException {

    Map<String, Object> payload =
        createPayloadMap(launchData, null, null, ROLE_FLUSHER, null, null);

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
      RmCaseDTO caseContainer,
      String userId,
      String role,
      String accountServiceUrl,
      String accountServiceLogoutUrl)
      throws CTPException {

    String questionnaireId = coreData.getQuestionnaireId();
    Source source = coreData.getSource();

    long currentTimeInSeconds = System.currentTimeMillis() / 1000;

    LinkedHashMap<String, Object> payload = new LinkedHashMap<>();

    payload.computeIfAbsent("jti", (k) -> UUID.randomUUID().toString());
    payload.computeIfAbsent("tx_id", (k) -> UUID.randomUUID().toString());
    payload.computeIfAbsent("iat", (k) -> currentTimeInSeconds);
    payload.computeIfAbsent("exp", (k) -> currentTimeInSeconds + (5 * 60));

    //TODO there's a separate ticket to align this with the current version of EQ - SOCINT-334
    if (role == null || !role.equals(ROLE_FLUSHER)) {
      Objects.requireNonNull(
          caseContainer, "CaseContainer mandatory unless role is '" + ROLE_FLUSHER + "'");

      validateCase(source, caseContainer, questionnaireId);
      String caseIdStr = caseContainer.getId().toString();
      payload.computeIfAbsent(
          "collection_exercise_sid", (k) -> "foo"); //SOCINT-334
      String convertedRegionCode = convertRegionCode(null); //SOCINT-334
      payload.computeIfAbsent("region_code", (k) -> convertedRegionCode);
      payload.computeIfAbsent("ru_ref", (k) -> questionnaireId); //SOCINT-334
      payload.computeIfAbsent("case_id", (k) -> caseIdStr);
      payload.computeIfAbsent(
          "display_address",
          (k) ->
              buildDisplayAddress("")); //SOCINT-334
//                  caseContainer.getAddressLine1(),
//                  caseContainer.getAddressLine2(),
//                  caseContainer.getAddressLine3(),
//                  caseContainer.getTownName(),
//                  caseContainer.getPostcode()));
    }
    String responseId = encryptResponseId(questionnaireId, coreData.getSalt());
    payload.computeIfAbsent("language_code", (k) -> coreData.getLanguage().getIsoLikeCode());
    payload.computeIfAbsent("response_id", (k) -> responseId);
    payload.computeIfAbsent("account_service_url", (k) -> accountServiceUrl);
    payload.computeIfAbsent("account_service_log_out_url", (k) -> accountServiceLogoutUrl);
    payload.computeIfAbsent("channel", (k) -> coreData.getChannel().name().toLowerCase());
    payload.computeIfAbsent("user_id", (k) -> userId);
    payload.computeIfAbsent("roles", (k) -> role);
    payload.computeIfAbsent("questionnaire_id", (k) -> questionnaireId);

    payload.computeIfAbsent("eq_id", (k) -> "census");
    payload.computeIfAbsent("period_id", (k) -> "2021");
    payload.computeIfAbsent("form_type", (k) -> coreData.getFormType());

    log.debug("Payload for EQ", kv("payload", payload));

    return payload;
  }

  private void validateCase(Source source, RmCaseDTO caseContainer, String questionnaireId)
      throws CTPException {
    UUID caseId = caseContainer.getId();

    verifyNotNull(caseContainer.getId(), "case id", caseId);
//    verifyNotNull(caseContainer.getCollectionExerciseId(), "collection id", caseId); //SOCINT-334
    verifyNotNull(questionnaireId, "questionnaireId", caseId);
//    if (source != FIELD_SERVICE) { //SOCINT-334
//      verifyNotNull(caseContainer.getUprn(), "address uprn", caseId);
//    }
//    verifyNotNull(caseContainer.getSurveyType(), "survey type", caseId); //SOCINT-334
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
  private String buildDisplayAddress(String... addressElements) {
    String displayAddress =
        Arrays.stream(addressElements)
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
