package uk.gov.ons.ctp.integration.caseapiclient.caseservice;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RmCaseDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.QuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;

/** This class is responsible for communications with the Case Service. */
@Slf4j
public class CaseServiceClientServiceImpl {
  private static final String CASE_BY_ID_QUERY_PATH = "/cases/{case-id}";
  private static final String CASE_BY_UPRN_QUERY_PATH = "/cases/uprn/{uprn}";
  private static final String CASE_BY_CASE_REFERENCE_QUERY_PATH = "/cases/ref/{reference}";
  private static final String CASE_GET_REUSABLE_QUESTIONNAIRE_ID_PATH = "/cases/ccs/{caseId}/qid";
  private static final String CASE_CREATE_SINGLE_USE_QUESTIONNAIRE_ID_PATH = "/cases/{caseId}/qid";
  private static final String CCS_CASE_BY_POSTCODE_QUERY_PATH = "/cases/ccs/postcode/{postcode}";

  private RestClient caseServiceClient;

  public CaseServiceClientServiceImpl(RestClient caseServiceClient) {
    super();
    this.caseServiceClient = caseServiceClient;
  }

  public RmCaseDTO getCaseById(UUID caseId, Boolean listCaseEvents) {
    log.debug(
        "getCaseById() calling Case Service to find case details by ID", kv("caseId", caseId));

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    RmCaseDTO caseDetails =
        caseServiceClient.getResource(
            CASE_BY_ID_QUERY_PATH, RmCaseDTO.class, null, queryParams, caseId.toString());
    log.debug("getCaseById() found case details for case ID", kv("caseId", caseId));
    return caseDetails;
  }

  public List<RmCaseDTO> getCaseByUprn(Long uprn, Boolean listCaseEvents) {
    log.debug(
        "getCaseByUprn() calling Case Service to find case details by Uprn", kv("uprn", uprn));

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));
    queryParams.add("validAddressOnly", Boolean.TRUE.toString());

    // Ask Case Service to find case details
    List<RmCaseDTO> cases =
        caseServiceClient.getResources(
            CASE_BY_UPRN_QUERY_PATH,
            RmCaseDTO[].class,
            null,
            queryParams,
            Long.toString(uprn));

    log.debug("getCaseByUprn() found case details by Uprn", kv("uprn", uprn));
    return cases;
  }

  public List<RmCaseDTO> getCcsCaseByPostcode(String postcode) {
    log.debug(
        "getCcsCaseByPostcode() calling Case Service to find ccs case details by postcode",
        kv("postcode", postcode));

    // Ask Case Service to find ccs case details
    List<RmCaseDTO> cases =
        caseServiceClient.getResources(
            CCS_CASE_BY_POSTCODE_QUERY_PATH, RmCaseDTO[].class, null, null, postcode);

    log.debug("getCaseByPostcode() found ccs case details by postcode", kv("postcode", postcode));

    return cases;
  }

  public RmCaseDTO getCaseByCaseRef(Long caseReference, Boolean listCaseEvents) {
    log.debug(
        "getCaseByCaseReference() calling Case Service to find case details by case reference",
        kv("caseReference", caseReference));

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    RmCaseDTO caseDetails =
        caseServiceClient.getResource(
            CASE_BY_CASE_REFERENCE_QUERY_PATH,
            RmCaseDTO.class,
            null,
            queryParams,
            caseReference);

    log.debug(
        "getCaseByCaseReference() found case details by case reference",
        kv("caseReference", caseReference));
    return caseDetails;
  }

  public QuestionnaireIdDTO getReusableQuestionnaireId(UUID caseId) {
    log.debug(
        "getReusableQuestionnaireId() calling Case Service to find questionnaire id "
            + "by case ID",
        kv("caseId", caseId));

    QuestionnaireIdDTO questionnaireId = null;

    questionnaireId =
        caseServiceClient.getResource(
            CASE_GET_REUSABLE_QUESTIONNAIRE_ID_PATH,
            QuestionnaireIdDTO.class,
            null,
            null,
            caseId.toString());
    log.debug(
        "getReusableQuestionnaireId() found questionnaire id for case ID",
        kv("questionnaireId", questionnaireId));

    return questionnaireId;
  }

  public SingleUseQuestionnaireIdDTO getSingleUseQuestionnaireId(
      UUID caseId, boolean individual, UUID individualCaseId) {
    log.debug(
        "getNewQuestionnaireIdForCase() calling Case Service to get new questionnaire ID",
        kv("caseId", caseId),
        kv("individual", individual),
        kv("individualCaseId", individualCaseId));

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    if (individual) {
      queryParams.add("individual", "true");
      if (individualCaseId != null) {
        queryParams.add("individualCaseId", individualCaseId.toString());
      }
    }

    // Ask Case Service to find case details
    SingleUseQuestionnaireIdDTO newQuestionnaireId =
        caseServiceClient.getResource(
            CASE_CREATE_SINGLE_USE_QUESTIONNAIRE_ID_PATH,
            SingleUseQuestionnaireIdDTO.class,
            null,
            queryParams,
            caseId.toString());

    log.debug(
        "getNewQuestionnaireIdForCase() generated new questionnaireId",
        kv("caseId", caseId),
        kv("questionnaireId", newQuestionnaireId.getQuestionnaireId()),
        kv("formType", newQuestionnaireId.getFormType()),
        kv("questionnaireType", newQuestionnaireId.getQuestionnaireId()));

    return newQuestionnaireId;
  }
}
