package uk.gov.ons.ctp.integration.caseapiclient.caseservice;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.UUID;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.QuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;

/** This class is responsible for communications with the Case Service. */
public class CaseServiceClientServiceImpl {
  private static final Logger log = LoggerFactory.getLogger(CaseServiceClientServiceImpl.class);
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

  public CaseContainerDTO getCaseById(UUID caseId, Boolean listCaseEvents) {
    log.with("caseId", caseId)
        .debug("getCaseById() calling Case Service to find case details by ID");

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    CaseContainerDTO caseDetails =
        caseServiceClient.getResource(
            CASE_BY_ID_QUERY_PATH, CaseContainerDTO.class, null, queryParams, caseId.toString());
    log.with("caseId", caseId).debug("getCaseById() found case details for case ID");

    return caseDetails;
  }

  public List<CaseContainerDTO> getCaseByUprn(Long uprn, Boolean listCaseEvents) {
    log.with("uprn", uprn)
        .debug("getCaseByUprn() calling Case Service to find case details by Uprn");

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));
    queryParams.add("validAddressOnly", Boolean.TRUE.toString());

    // Ask Case Service to find case details
    List<CaseContainerDTO> cases =
        caseServiceClient.getResources(
            CASE_BY_UPRN_QUERY_PATH,
            CaseContainerDTO[].class,
            null,
            queryParams,
            Long.toString(uprn));

    log.with("uprn", uprn).debug("getCaseByUprn() found case details by Uprn");

    return cases;
  }

  public List<CaseContainerDTO> getCcsCaseByPostcode(String postcode) {
    log.with("postcode", postcode)
        .debug("getCcsCaseByPostcode() calling Case Service to find ccs case details by postcode");

    // Ask Case Service to find ccs case details
    List<CaseContainerDTO> cases =
        caseServiceClient.getResources(
            CCS_CASE_BY_POSTCODE_QUERY_PATH, CaseContainerDTO[].class, null, null, postcode);

    log.with("postcode", postcode).debug("getCaseByPostcode() found ccs case details by postcode");

    return cases;
  }

  public CaseContainerDTO getCaseByCaseRef(Long caseReference, Boolean listCaseEvents) {
    log.with("caseReference", caseReference)
        .debug(
            "getCaseByCaseReference() calling Case Service to find case details by case reference");

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    CaseContainerDTO caseDetails =
        caseServiceClient.getResource(
            CASE_BY_CASE_REFERENCE_QUERY_PATH,
            CaseContainerDTO.class,
            null,
            queryParams,
            caseReference);

    log.with("caseReference", caseReference)
        .debug("getCaseByCaseReference() found case details by case reference");

    return caseDetails;
  }

  public QuestionnaireIdDTO getReusableQuestionnaireId(UUID caseId) {
    log.with("caseId", caseId)
        .debug(
            "getReusableQuestionnaireId() calling Case Service to find questionnaire id "
                + "by case ID");

    QuestionnaireIdDTO questionnaireId = null;

    questionnaireId =
        caseServiceClient.getResource(
            CASE_GET_REUSABLE_QUESTIONNAIRE_ID_PATH,
            QuestionnaireIdDTO.class,
            null,
            null,
            caseId.toString());
    log.with("questionnaireId", questionnaireId)
        .debug("getReusableQuestionnaireId() found questionnaire id for case ID");

    return questionnaireId;
  }

  public SingleUseQuestionnaireIdDTO getSingleUseQuestionnaireId(
      UUID caseId, boolean individual, UUID individualCaseId) {
    log.with("caseId", caseId)
        .with("individual", individual)
        .with("individualCaseId", individualCaseId)
        .debug("getNewQuestionnaireIdForCase() calling Case Service to get new questionnaire ID");

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

    log.with("caseId", caseId)
        .with("questionnaireId", newQuestionnaireId.getQuestionnaireId())
        .with("formType", newQuestionnaireId.getFormType())
        .with("questionnaireType", newQuestionnaireId.getQuestionnaireId())
        .debug("getNewQuestionnaireIdForCase() generated new questionnaireId");

    return newQuestionnaireId;
  }
}
