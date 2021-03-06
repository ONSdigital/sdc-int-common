package uk.gov.ons.ctp.integration.caseapiclient.caseservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RmCaseDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.TelephoneCaptureDTO;

/**
 * This class contains unit tests for the CaseServiceClientServiceImpl class. It mocks out the Rest
 * calls and returns dummy responses to represent what would be returned by the case service.
 */
@ExtendWith(MockitoExtension.class)
public class CaseServiceClientServiceTest {
  private static final String ID_0 = "b7565b5e-1396-4965-91a2-918c0d3642ed";
  private static final String ID_1 = "b7565b5e-2222-2222-2222-918c0d3642ed";
  private static final String ID_2 = "603d440b-18a0-41a0-992a-e12ea858ed35";

  private static final List<String> IDS = Arrays.asList(ID_0, ID_1, ID_2);

  @Mock RestClient restClient;

  @InjectMocks
  CaseServiceClientService caseServiceClientService = new CaseServiceClientService(restClient);

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
  public void testGetSingleUseQid() throws Exception {
    UUID testUuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");

    // Build results to be returned by the case service
    TelephoneCaptureDTO expectedResponse =
        FixtureHelper.loadClassFixtures(TelephoneCaptureDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/cases/{caseId}/telephone-capture"),
                eq(TelephoneCaptureDTO.class),
                any(),
                any(),
                eq(testUuid.toString())))
        .thenReturn(expectedResponse);

    // Run the request
    TelephoneCaptureDTO actualResponse =
        caseServiceClientService.getSingleUseQuestionnaireId(testUuid);

    assertEquals(expectedResponse.getQId(), actualResponse.getQId());
    assertEquals(expectedResponse.getCaseId(), actualResponse.getCaseId());
  }

  @SneakyThrows
  private RmCaseDTO doTestGetCaseById(boolean requireCaseEvents, int index) {
    UUID testUuid = UUID.fromString(IDS.get(index));

    // Build results to be returned by the case service
    RmCaseDTO expectedResponse = FixtureHelper.loadClassFixtures(RmCaseDTO[].class).get(index);
    Mockito.when(
            restClient.getResource(
                eq("/cases/{case-id}"), eq(RmCaseDTO.class), any(), any(), eq(testUuid.toString())))
        .thenReturn(expectedResponse);

    // Run the request
    RmCaseDTO actualResponse = caseServiceClientService.getCaseById(testUuid, requireCaseEvents);

    // Sanity check the response
    assertEquals(testUuid, actualResponse.getId());
    assertNotNull(
        actualResponse.getCaseEvents()); // Response will have events as not removed at this
    // level
    verifyRequestUsedCaseEventsQueryParam(requireCaseEvents);
    return actualResponse;
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
    RmCaseDTO expectedResponse = FixtureHelper.loadClassFixtures(RmCaseDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/cases/ref/{reference}"), eq(RmCaseDTO.class), any(), any(), eq(testCaseRef)))
        .thenReturn(expectedResponse);

    // Run the request
    RmCaseDTO actualResponse =
        caseServiceClientService.getCaseByCaseRef(testCaseRef, requireCaseEvents);

    // Sanity check the response
    assertEquals(Long.toString(testCaseRef), actualResponse.getCaseRef());
    assertEquals(testUuid, actualResponse.getId());
    assertNotNull(
        actualResponse.getCaseEvents()); // Response will have events as not removed at this
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
