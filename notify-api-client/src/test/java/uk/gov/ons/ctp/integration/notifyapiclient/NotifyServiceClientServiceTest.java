package uk.gov.ons.ctp.integration.notifyapiclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.notifyapiclient.model.NotifyRequestDTO;
import uk.gov.ons.ctp.integration.notifyapiclient.model.NotifyResponseDTO;

/**
 * This class contains unit tests for the CaseServiceClientServiceImpl class. It mocks out the Rest
 * calls and returns dummy responses to represent what would be returned by the case service.
 */
@ExtendWith(MockitoExtension.class)
public class NotifyServiceClientServiceTest {

  @Mock RestClient restClient;

  @InjectMocks
  NotifyServiceClientService notifyServiceClientService =
      new NotifyServiceClientService(restClient);

  @Captor ArgumentCaptor<NotifyRequestDTO> requestBodyCaptor;

  @Test
  public void testSendSmsRequest() throws Exception {
    UUID testUuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
    String testUser = "philip.whiles@ext.ons.gov.uk";
    String testTelNo = "447958583116";
    String testPackCode = "SMS123";
    UUID testCorrelationId = UUID.randomUUID();

    // Build results to be returned by the Notify service
    NotifyResponseDTO resultsFromNotifyService =
        FixtureHelper.loadClassFixtures(NotifyResponseDTO[].class).get(0);
    Mockito.when(
            restClient.postResource(
                eq("/sms-fulfilment/{case-id}"),
                any(),any(),
                eq(testUuid.toString())))
        .thenReturn(resultsFromNotifyService);

    // Run the request
    NotifyResponseDTO results = notifyServiceClientService.requestSmsFulfilment(testUuid, testTelNo, testPackCode, Channel.CC, testUser, testCorrelationId);

    assertEquals(resultsFromNotifyService.getQid(), results.getQid());
    assertEquals(resultsFromNotifyService.getUacHash(), results.getUacHash());

    Mockito.verify(restClient).postResource(any(), requestBodyCaptor.capture(), any(), any());
    NotifyRequestDTO requestBody = requestBodyCaptor.getValue();
    assertEquals(Channel.CC.name(), requestBody.getHeader().getChannel());
    assertEquals("Contact Centre", requestBody.getHeader().getSource());
    assertEquals(testUser, requestBody.getHeader().getOriginatingUser());

    assertEquals(testUuid, requestBody.getPayload().getSmsFulfilment().getCaseId());
    assertEquals(testTelNo, requestBody.getPayload().getSmsFulfilment().getPhoneNumber());
    assertEquals(testPackCode, requestBody.getPayload().getSmsFulfilment().getPackCode());
    assertEquals(testCorrelationId, requestBody.getHeader().getCorrelationId());

    assertEquals(null, requestBody.getPayload().getEmailFulfilment());
  }

  @Test
  public void testSendEmailRequest() throws Exception {
    UUID testUuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
    String testUser = "philip.whiles@ext.ons.gov.uk";
    String testEmail = "phil.whiles@gmail.com";
    String testPackCode = "SMS123";
    UUID testCorrelationId = UUID.randomUUID();

    // Build results to be returned by the Notify service
    NotifyResponseDTO resultsFromNotifyService =
        FixtureHelper.loadClassFixtures(NotifyResponseDTO[].class).get(0);
    Mockito.when(
            restClient.postResource(
                eq("/email-fulfilment/{case-id}"),
                any(),any(),
                eq(testUuid.toString())))
        .thenReturn(resultsFromNotifyService);

    // Run the request
    NotifyResponseDTO results = notifyServiceClientService.requestEmailFulfilment(testUuid, testEmail, testPackCode, Channel.CC, testUser, testCorrelationId);

    assertEquals(resultsFromNotifyService.getQid(), results.getQid());
    assertEquals(resultsFromNotifyService.getUacHash(), results.getUacHash());

    Mockito.verify(restClient).postResource(any(), requestBodyCaptor.capture(), any(), any());
    NotifyRequestDTO requestBody = requestBodyCaptor.getValue();
    assertEquals(Channel.CC.name(), requestBody.getHeader().getChannel());
    assertEquals("Contact Centre", requestBody.getHeader().getSource());
    assertEquals(testUser, requestBody.getHeader().getOriginatingUser());
    assertEquals(testCorrelationId, requestBody.getHeader().getCorrelationId());

    assertEquals(testUuid, requestBody.getPayload().getEmailFulfilment().getCaseId());
    assertEquals(testEmail, requestBody.getPayload().getEmailFulfilment().getEmail());
    assertEquals(testPackCode, requestBody.getPayload().getEmailFulfilment().getPackCode());

    assertEquals(null, requestBody.getPayload().getSmsFulfilment());
  }
}
