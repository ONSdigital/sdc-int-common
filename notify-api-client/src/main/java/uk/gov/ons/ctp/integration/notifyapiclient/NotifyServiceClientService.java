package uk.gov.ons.ctp.integration.notifyapiclient;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.notifyapiclient.model.EmailFulfilment;
import uk.gov.ons.ctp.integration.notifyapiclient.model.NotifyRequestDTO;
import uk.gov.ons.ctp.integration.notifyapiclient.model.NotifyRequestHeaderDTO;
import uk.gov.ons.ctp.integration.notifyapiclient.model.NotifyRequestPayloadDTO;
import uk.gov.ons.ctp.integration.notifyapiclient.model.NotifyResponseDTO;
import uk.gov.ons.ctp.integration.notifyapiclient.model.SmsFulfilment;

/** This class is responsible for communications with the Case Service. */
@Slf4j
public class NotifyServiceClientService {
  private static final String REQUEST_SMS_FULFILMENT_PATH = "/sms-fulfilment/{case-id}";
  private static final String REQUEST_EMAIL_FULFILMENT_PATH = "/email-fulfilment/{case-id}";

  private RestClient notifyServiceClient;

  public NotifyServiceClientService(RestClient notifyServiceClient) {
    super();
    this.notifyServiceClient = notifyServiceClient;
  }

  public NotifyResponseDTO requestSmsFulfilment(UUID caseId, String telephoneNumber, String packCode, Channel channel, String userId, UUID correlationId) {
    log.debug(
        "requestSmsFulfilment() calling Notify Service", kv("caseId", caseId), kv("packCode", packCode), kv("userId", userId));

    NotifyRequestDTO request = constructRequestBody(channel, userId, correlationId);
    SmsFulfilment smsFulfilment = new SmsFulfilment();
    smsFulfilment.setCaseId(caseId);
    smsFulfilment.setPackCode(packCode);
    smsFulfilment.setPhoneNumber(telephoneNumber);
    NotifyRequestPayloadDTO payload = new NotifyRequestPayloadDTO();
    payload.setSmsFulfilment(smsFulfilment);
    request.setPayload(payload);
    
    NotifyResponseDTO response =
        notifyServiceClient.postResource(
            REQUEST_SMS_FULFILMENT_PATH, request, NotifyResponseDTO.class, caseId.toString());
    log.debug("requestSmsFulfilment() given questionnaire ID for case", kv("caseId", caseId), kv("questionnaireId", response.getQid()));
    return response;
  }
  
  public NotifyResponseDTO requestEmailFulfilment(UUID caseId, String email, String packCode, Channel channel, String userId, UUID correlationId) {
    log.debug(
        "requestEmailFulfilment() calling Notify Service", kv("caseId", caseId), kv("packCode", packCode), kv("userId", userId));

    NotifyRequestDTO request = constructRequestBody(channel, userId, correlationId);
    EmailFulfilment emailFulfilment = new EmailFulfilment();
    emailFulfilment.setCaseId(caseId);
    emailFulfilment.setPackCode(packCode);
    emailFulfilment.setEmail(email);
    NotifyRequestPayloadDTO payload = new NotifyRequestPayloadDTO();
    payload.setEmailFulfilment(emailFulfilment);
    request.setPayload(payload);
    
    NotifyResponseDTO response =
        notifyServiceClient.postResource(
            REQUEST_EMAIL_FULFILMENT_PATH, request, NotifyResponseDTO.class, caseId.toString());
    log.debug("requestEmailFulfilment() given questionnaire ID for case", kv("caseId", caseId), kv("questionnaireId", response.getQid()));
    return response;
  }

  private NotifyRequestDTO constructRequestBody(Channel channel, String userId, UUID correlationId) {
    NotifyRequestDTO request = new NotifyRequestDTO();
    NotifyRequestHeaderDTO header = new NotifyRequestHeaderDTO();
    header.setCorrelationId(correlationId);
    header.setChannel(channel.name());
    header.setOriginatingUser(userId);
    header.setSource(channel == Channel.RH ? "Respondent Home" : "Contact Centre");
    request.setHeader(header);
    return request;
  }


}
