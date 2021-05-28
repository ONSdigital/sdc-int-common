package uk.gov.ons.ctp.integration.caseapiclient.caseservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.MultiValueMap;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.QuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;

/**
 * This class contains unit tests for the CaseServiceClientServiceImpl class. It mocks out the Rest
 * calls and returns dummy responses to represent what would be returned by the case service.
 */
@RunWith(MockitoJUnitRunner.class)
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
  public void shouldFindNonSecureEstablishment() {
    CaseContainerDTO result = doTestGetCaseById(true, 0);
    assertFalse(result.isSecureEstablishment());
  }

  @Test
  public void shouldFindSecureEstablishment() {
    CaseContainerDTO result = doTestGetCaseById(true, 2);
    assertTrue(result.isSecureEstablishment());
  }

  @Test
  public void shouldFindEstablishmentUprn() {
    CaseContainerDTO result = doTestGetCaseById(true, 2);
    assertEquals("334111111111", result.getEstabUprn());
  }

  @Test
  public void shouldNotFindEstablishmentUprn() {
    CaseContainerDTO result = doTestGetCaseById(true, 0);
    assertNull(result.getEstabUprn());
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
  private CaseContainerDTO doTestGetCaseById(boolean requireCaseEvents, int index) {
    UUID testUuid = UUID.fromString(IDS.get(index));

    // Build results to be returned by the case service
    CaseContainerDTO resultsFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(index);
    Mockito.when(
            restClient.getResource(
                eq("/cases/{case-id}"),
                eq(CaseContainerDTO.class),
                any(),
                any(),
                eq(testUuid.toString())))
        .thenReturn(resultsFromCaseService);

    // Run the request
    CaseContainerDTO results = caseServiceClientService.getCaseById(testUuid, requireCaseEvents);

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
    List<CaseContainerDTO> caseData = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
    Mockito.when(
            restClient.getResources(
                eq("/cases/uprn/{uprn}"),
                eq(CaseContainerDTO[].class),
                any(),
                any(),
                eq(Long.toString(uprn.getValue()))))
        .thenReturn(caseData);

    // Run the request
    List<CaseContainerDTO> results =
        caseServiceClientService.getCaseByUprn(uprn.getValue(), requireCaseEvents);

    // Sanity check the response
    assertEquals(UUID.fromString(caseId1), results.get(0).getId());
    assertEquals(Long.toString(uprn.getValue()), results.get(0).getUprn());
    assertNotNull(results.get(0).getCaseEvents()); // Events not removed yet

    assertEquals(UUID.fromString(caseId2), results.get(1).getId());
    assertEquals(Long.toString(uprn.getValue()), results.get(1).getUprn());
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
    List<CaseContainerDTO> caseData = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
    Mockito.when(
            restClient.getResources(
                eq("/cases/ccs/postcode/{postcode}"),
                eq(CaseContainerDTO[].class),
                any(),
                any(),
                eq(postcode)))
        .thenReturn(caseData);

    // Run the request
    List<CaseContainerDTO> results = caseServiceClientService.getCcsCaseByPostcode(postcode);

    // Sanity check the response
    assertEquals(UUID.fromString(caseId1), results.get(0).getId());
    assertEquals(postcode, results.get(0).getPostcode());

    assertEquals(UUID.fromString(caseId2), results.get(1).getId());
    assertEquals(postcode, results.get(1).getPostcode());
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
    CaseContainerDTO resultsFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/cases/ref/{reference}"),
                eq(CaseContainerDTO.class),
                any(),
                any(),
                eq(testCaseRef)))
        .thenReturn(resultsFromCaseService);

    // Run the request
    CaseContainerDTO results =
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
