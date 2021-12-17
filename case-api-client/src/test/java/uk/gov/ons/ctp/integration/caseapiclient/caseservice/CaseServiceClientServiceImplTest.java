package uk.gov.ons.ctp.integration.caseapiclient.caseservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.MultiValueMap;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RmCaseDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.QuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;

/**
 * This class contains unit tests for the CaseServiceClientServiceImpl class. It mocks out the Rest
 * calls and returns dummy responses to represent what would be returned by the case service.
 */
@ExtendWith(MockitoExtension.class)
public class CaseServiceClientServiceImplTest {
  private static final String ID_0 = "b7565b5e-1396-4965-91a2-918c0d3642ed";
  private static final String ID_1 = "b7565b5e-2222-2222-2222-918c0d3642ed";
  private static final String ID_2 = "603d440b-18a0-41a0-992a-e12ea858ed35";

  private static final List<String> IDS = Arrays.asList(ID_0, ID_1, ID_2);

  @Mock RestClient restClient;

  @InjectMocks
  CaseServiceClientServiceImpl caseServiceClientService =
      new CaseServiceClientServiceImpl(restClient);

  @Captor ArgumentCaptor<MultiValueMap<String, String>> queryParamsCaptor;

  @Test
  public void testGetCaseById_withCaseEvents() {
    doTestGetCaseById(true, 0);
  }

  @Test
  public void testGetCaseById_withNoCaseEvents() {
    doTestGetCaseById(false, 0);
  }

  @Test
  public void testGetReusableQid() throws Exception {
    doTestGetReusableQid();
  }

  private void doTestGetReusableQid() throws Exception {
    UUID testUuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");

    // Build results to be returned by the case service
    QuestionnaireIdDTO resultsFromCaseService =
        FixtureHelper.loadClassFixtures(QuestionnaireIdDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/cases/ccs/{caseId}/qid"),
                eq(QuestionnaireIdDTO.class),
                any(),
                any(),
                eq(testUuid.toString())))
        .thenReturn(resultsFromCaseService);

    // Run the request
    QuestionnaireIdDTO results = caseServiceClientService.getReusableQuestionnaireId(testUuid);

    assertEquals(resultsFromCaseService.getQuestionnaireId(), results.getQuestionnaireId());
    assertEquals(resultsFromCaseService.getFormType(), results.getFormType());
    assertEquals(resultsFromCaseService.isActive(), results.isActive());
  }

  @Test
  public void doTestGetSingleUseQid() throws Exception {
    UUID testUuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");

    // Build results to be returned by the case service
    SingleUseQuestionnaireIdDTO resultsFromCaseService =
        FixtureHelper.loadClassFixtures(SingleUseQuestionnaireIdDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/cases/{caseId}/qid"),
                eq(SingleUseQuestionnaireIdDTO.class),
                any(),
                any(),
                eq(testUuid.toString())))
        .thenReturn(resultsFromCaseService);

    // Run the request
    SingleUseQuestionnaireIdDTO results =
        caseServiceClientService.getSingleUseQuestionnaireId(testUuid, true, UUID.randomUUID());

    assertEquals(resultsFromCaseService.getQuestionnaireId(), results.getQuestionnaireId());
    assertEquals(resultsFromCaseService.getUac(), results.getUac());
    assertEquals(resultsFromCaseService.getFormType(), results.getFormType());
    assertEquals(resultsFromCaseService.getQuestionnaireType(), results.getQuestionnaireType());
  }

  @SneakyThrows
  private RmCaseDTO doTestGetCaseById(boolean requireCaseEvents, int index) {
    UUID testUuid = UUID.fromString(IDS.get(index));

    // Build results to be returned by the case service
    RmCaseDTO resultsFromCaseService =
        FixtureHelper.loadClassFixtures(RmCaseDTO[].class).get(index);
    Mockito.when(
            restClient.getResource(
                eq("/cases/{case-id}"),
                eq(RmCaseDTO.class),
                any(),
                any(),
                eq(testUuid.toString())))
        .thenReturn(resultsFromCaseService);

    // Run the request
    RmCaseDTO results = caseServiceClientService.getCaseById(testUuid, requireCaseEvents);

    // Sanity check the response
    assertEquals(testUuid, results.getId());
    assertNotNull(results.getCaseEvents()); // Response will have events as not removed at this
    // level
    verifyRequestUsedCaseEventsQueryParam(requireCaseEvents);
    return results;
  }

  @Test
  public void testGetCaseByUprn_withCaseEvents() throws Exception {
    doTestGetCaseByUprn(true);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseEvents() throws Exception {
    doTestGetCaseByUprn(false);
  }

  @Test
  public void testGetCcsCaseByPostcode() {
    doTestGetCcsCaseByPostcode();
  }

  private void doTestGetCaseByUprn(boolean requireCaseEvents) throws Exception {
    String caseId1 = "b7565b5e-1396-4965-91a2-918c0d3642ed";
    String caseId2 = "b7565b5e-2222-2222-2222-918c0d3642ed";
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(334999999999L);

    // Build results to be returned by the case service
    List<RmCaseDTO> caseData = FixtureHelper.loadClassFixtures(RmCaseDTO[].class);
    Mockito.when(
            restClient.getResources(
                eq("/cases/uprn/{uprn}"),
                eq(RmCaseDTO[].class),
                any(),
                any(),
                eq(Long.toString(uprn.getValue()))))
        .thenReturn(caseData);

    // Run the request
    List<RmCaseDTO> results =
        caseServiceClientService.getCaseByUprn(uprn.getValue(), requireCaseEvents);

    // Sanity check the response
    assertEquals(UUID.fromString(caseId1), results.get(0).getId());
    assertEquals(Long.toString(uprn.getValue()), results.get(0).getSample().get(CaseUpdate.ATTRIBUTE_UPRN));
    assertNotNull(results.get(0).getCaseEvents()); // Events not removed yet

    assertEquals(UUID.fromString(caseId2), results.get(1).getId());
    assertEquals(Long.toString(uprn.getValue()), results.get(1).getSample().get(CaseUpdate.ATTRIBUTE_UPRN));
    assertNotNull(results.get(1).getCaseEvents()); // Events not removed yet

    // Make sure the caseEvents arg was passed through correctly
    Mockito.verify(restClient)
        .getResources(any(), any(), any(), queryParamsCaptor.capture(), any());
    MultiValueMap<String, String> queryParams = queryParamsCaptor.getValue();
    assertEquals(2, queryParams.keySet().size());
    assertEquals("[" + requireCaseEvents + "]", queryParams.get("caseEvents").toString());
    assertEquals("[true]", queryParams.get("validAddressOnly").toString());
  }

  private void doTestGetCcsCaseByPostcode() {
    String caseId1 = "b7565b5e-1396-4965-91a2-918c0d3642ed";
    String caseId2 = "b7565b5e-2222-2222-2222-918c0d3642ed";
    String postcode = "G1 2AA";

    // Build results to be returned by the case service
    List<RmCaseDTO> caseData = FixtureHelper.loadClassFixtures(RmCaseDTO[].class);
    Mockito.when(
            restClient.getResources(
                eq("/cases/ccs/postcode/{postcode}"),
                eq(RmCaseDTO[].class),
                any(),
                any(),
                eq(postcode)))
        .thenReturn(caseData);

    // Run the request
    List<RmCaseDTO> results = caseServiceClientService.getCcsCaseByPostcode(postcode);

    // Sanity check the response
    assertEquals(UUID.fromString(caseId1), results.get(0).getId());
    assertEquals(postcode, results.get(0).getSample().get(CaseUpdate.ATTRIBUTE_POSTCODE));

    assertEquals(UUID.fromString(caseId2), results.get(1).getId());
    assertEquals(postcode, results.get(1).getSample().get(CaseUpdate.ATTRIBUTE_POSTCODE));
  }

  @Test
  public void testGetCaseByCaseRef_withCaseEvents() throws Exception {
    doTestGetCaseByCaseRef(true);
  }

  @Test
  public void testGetCaseByCaseRef_withNoCaseEvents() throws Exception {
    doTestGetCaseByCaseRef(false);
  }

  private void doTestGetCaseByCaseRef(boolean requireCaseEvents) throws Exception {
    UUID testUuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
    Long testCaseRef = 52224L;

    // Build results to be returned by the case service
    RmCaseDTO resultsFromCaseService =
        FixtureHelper.loadClassFixtures(RmCaseDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/cases/ref/{reference}"),
                eq(RmCaseDTO.class),
                any(),
                any(),
                eq(testCaseRef)))
        .thenReturn(resultsFromCaseService);

    // Run the request
    RmCaseDTO results =
        caseServiceClientService.getCaseByCaseRef(testCaseRef, requireCaseEvents);

    // Sanity check the response
    assertEquals(Long.toString(testCaseRef), results.getCaseRef());
    assertEquals(testUuid, results.getId());
    assertNotNull(results.getCaseEvents()); // Response will have events as not removed at this
    // level
    verifyRequestUsedCaseEventsQueryParam(requireCaseEvents);
  }

  private void verifyRequestUsedCaseEventsQueryParam(boolean expectedCaseEventsValue) {
    Mockito.verify(restClient).getResource(any(), any(), any(), queryParamsCaptor.capture(), any());
    MultiValueMap<String, String> queryParams = queryParamsCaptor.getValue();
    assertEquals("[" + expectedCaseEventsValue + "]", queryParams.get("caseEvents").toString());
    assertEquals(1, queryParams.keySet().size());
  }
}
