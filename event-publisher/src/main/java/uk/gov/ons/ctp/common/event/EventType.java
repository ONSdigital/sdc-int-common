package uk.gov.ons.ctp.common.event;

import lombok.Getter;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.RefusalDetails;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateResponse;

@Getter
public enum EventType { // TODO: PMB Rename to topicType??
  CASE_UPDATE(CollectionCase.class, EventBuilder.CASE_UPDATE),
  FULFILMENT(FulfilmentRequest.class, EventBuilder.FULFILMENT),
  REFUSAL(RefusalDetails.class, EventBuilder.REFUSAL),
  UAC_AUTHENTICATE(UacAuthenticateResponse.class, EventBuilder.UAC_AUTHENTICATE),
  SURVEY_LAUNCH(SurveyLaunchResponse.class, EventBuilder.SURVEY_LAUNCH),
  UAC_UPDATE(UAC.class, EventBuilder.UAC_UPDATE);

  private Class<? extends EventPayload> payloadType;
  private EventBuilder builder;

  private EventType() {
    this.builder = EventBuilder.NONE;
  }

  private EventType(Class<? extends EventPayload> payloadType, EventBuilder builder) {
    this.payloadType = payloadType;
    this.builder = builder;
  }
}
