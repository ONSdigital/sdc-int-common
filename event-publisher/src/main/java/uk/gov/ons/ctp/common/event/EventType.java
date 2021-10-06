package uk.gov.ons.ctp.common.event;

import lombok.Getter;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.RefusalDetails;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateResponse;

@Getter
public enum EventType {
  CASE_UPDATE(CaseUpdate.class, EventBuilder.CASE_UPDATE),
  FULFILMENT(FulfilmentRequest.class, EventBuilder.FULFILMENT),
  REFUSAL(RefusalDetails.class, EventBuilder.REFUSAL),
  UAC_AUTHENTICATE(UacAuthenticateResponse.class, EventBuilder.UAC_AUTHENTICATE),
  SURVEY_LAUNCH(SurveyLaunchResponse.class, EventBuilder.SURVEY_LAUNCH),
  SURVEY_UPDATE(SurveyUpdate.class, EventBuilder.SURVEY_UPDATE),
  UAC_UPDATE(UAC.class, EventBuilder.UAC_UPDATE),
  COLLECTION_EXERCISE_UPDATE(CollectionExercise.class, EventBuilder.COLLECTION_EXCERSISE_UPDATE);

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
