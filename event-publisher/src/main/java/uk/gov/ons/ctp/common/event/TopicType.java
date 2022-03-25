package uk.gov.ons.ctp.common.event;

import lombok.Getter;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.EqLaunch;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.InvalidCase;
import uk.gov.ons.ctp.common.event.model.NewCasePayloadContent;
import uk.gov.ons.ctp.common.event.model.RefusalDetails;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacAuthentication;
import uk.gov.ons.ctp.common.event.model.UacUpdate;

@Getter
public enum TopicType {
  CASE_UPDATE(CaseUpdate.class, EventBuilder.CASE_UPDATE),
  FULFILMENT(FulfilmentRequest.class, EventBuilder.FULFILMENT),
  REFUSAL(RefusalDetails.class, EventBuilder.REFUSAL),
  UAC_AUTHENTICATION(UacAuthentication.class, EventBuilder.UAC_AUTHENTICATION),
  EQ_LAUNCH(EqLaunch.class, EventBuilder.EQ_LAUNCH),
  UAC_UPDATE(UacUpdate.class, EventBuilder.UAC_UPDATE),
  SURVEY_UPDATE(SurveyUpdate.class, EventBuilder.SURVEY_UPDATE),
  COLLECTION_EXERCISE_UPDATE(
      CollectionExerciseUpdate.class, EventBuilder.COLLECTION_EXCERSISE_UPDATE),
  NEW_CASE(NewCasePayloadContent.class, EventBuilder.NEW_CASE),
  INVALID_CASE(InvalidCase.class, EventBuilder.INVALID_CASE);

  private Class<? extends EventPayload> payloadType;
  private EventBuilder builder;

  private TopicType() {
    this.builder = EventBuilder.NONE;
  }

  private TopicType(Class<? extends EventPayload> payloadType, EventBuilder builder) {
    this.payloadType = payloadType;
    this.builder = builder;
  }
}
