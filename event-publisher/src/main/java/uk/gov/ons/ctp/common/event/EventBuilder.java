package uk.gov.ons.ctp.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CasePayload;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdatePayload;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentEvent;
import uk.gov.ons.ctp.common.event.model.FulfilmentPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.NewCaseEvent;
import uk.gov.ons.ctp.common.event.model.NewCasePayload;
import uk.gov.ons.ctp.common.event.model.NewCasePayloadContent;
import uk.gov.ons.ctp.common.event.model.RefusalDetails;
import uk.gov.ons.ctp.common.event.model.RefusalEvent;
import uk.gov.ons.ctp.common.event.model.RefusalPayload;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.common.event.model.SurveyUpdatePayload;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateEvent;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateResponse;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.model.UacPayload;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

/**
 * Build objects ready for the publisher to send events. The subclasses of the event builder handle
 * the inconsistent structure of each event object.
 */
public abstract class EventBuilder {
  public static final EventBuilder NONE = new NullEventBuilder();
  public static final EventBuilder FULFILMENT = new FulfilmentBuilder();
  public static final EventBuilder SURVEY_LAUNCH = new SurveyLaunchBuilder();
  public static final EventBuilder SURVEY_UPDATE = new SurveyUpdateBuilder();
  public static final EventBuilder UAC_AUTHENTICATE = new UacAuthenticateBuilder();
  public static final EventBuilder CASE_UPDATE = new CaseUpdateBuilder();
  public static final EventBuilder REFUSAL = new RefusalBuilder();
  public static final EventBuilder UAC_UPDATE = new UacUpdateBuilder();
  public static final EventBuilder COLLECTION_EXCERSISE_UPDATE =
      new CollectionExerciseUpdateBuilder();
  public static final EventBuilder NEW_CASE = new NewCaseBuilder();

  private static final String EVENT_VERSION = "v0.3_DRAFT";

  ObjectMapper objectMapper = new CustomObjectMapper();

  /**
   * Create event ready for send.
   *
   * @param sendInfo object containing payload , source and channel.
   * @return event
   */
  abstract GenericEvent create(SendInfo sendInfo);

  /**
   * Create information required to send the event based on the serialised backup event supplied.
   *
   * @param json string of serialised event backup JSON.
   * @return object containing deserialised payload , source and channel.
   */
  abstract SendInfo create(String json);

  /**
   * Create event payload from json string.
   *
   * @param json string of serialised event payload JSON.
   * @return object containing deserialised payload
   */
  abstract EventPayload createPayload(String json);

  <T extends GenericEvent> T deserialiseEventJson(String json, Class<T> clazz) {
    try {
      return objectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new EventPublishException(e);
    }
  }

  <T extends EventPayload> T deserialisePayloadJson(String json, Class<T> clazz) {
    try {
      return objectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new EventPublishException(e);
    }
  }

  static Header buildHeader(EventTopic topic, Source source, Channel channel) {
    return Header.builder()
        .version(EVENT_VERSION)
        .topic(topic)
        .source(source)
        .channel(channel)
        .dateTime(new Date())
        .messageId(UUID.randomUUID().toString())
        .correlationId(UUID.randomUUID().toString()) // TODO: PMB. Do we create??
        .originatingUser("TBD") // TODO: PMB. Where do we get the user email address from
        .build();
  }

  @Data
  @AllArgsConstructor
  @Builder
  public static class SendInfo {
    private EventPayload payload;
    private Source source;
    private Channel channel;
  }

  SendInfo build(GenericEvent genericEvent, EventPayload payload) {
    SendInfo info =
        SendInfo.builder()
            .payload(payload)
            .source(genericEvent.getHeader().getSource())
            .channel(genericEvent.getHeader().getChannel())
            .build();
    return info;
  }

  public static class NullEventBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      return null;
    }

    @Override
    SendInfo create(String json) {
      return null;
    }

    @Override
    EventPayload createPayload(String json) {
      return null;
    }
  }

  public static class FulfilmentBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      FulfilmentEvent fulfilmentRequestedEvent = new FulfilmentEvent();
      fulfilmentRequestedEvent.setHeader(
          buildHeader(EventTopic.FULFILMENT, sendInfo.getSource(), sendInfo.getChannel()));
      FulfilmentPayload fulfilmentPayload =
          new FulfilmentPayload((FulfilmentRequest) sendInfo.getPayload());
      fulfilmentRequestedEvent.setPayload(fulfilmentPayload);
      return fulfilmentRequestedEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, FulfilmentEvent.class);
      EventPayload payload = ((FulfilmentEvent) genericEvent).getPayload().getFulfilmentRequest();
      return build(genericEvent, payload);
    }

    @Override
    EventPayload createPayload(String json) {
      return deserialisePayloadJson(json, FulfilmentRequest.class);
    }
  }

  public static class SurveyLaunchBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      SurveyLaunchEvent surveyLaunchedEvent = new SurveyLaunchEvent();
      surveyLaunchedEvent.setHeader(
          buildHeader(EventTopic.SURVEY_LAUNCH, sendInfo.getSource(), sendInfo.getChannel()));
      surveyLaunchedEvent.getPayload().setResponse((SurveyLaunchResponse) sendInfo.getPayload());
      return surveyLaunchedEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, SurveyLaunchEvent.class);
      EventPayload payload = ((SurveyLaunchEvent) genericEvent).getPayload().getResponse();
      return build(genericEvent, payload);
    }

    @Override
    EventPayload createPayload(String json) {
      return deserialisePayloadJson(json, SurveyLaunchResponse.class);
    }
  }

  public static class UacAuthenticateBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      UacAuthenticateEvent respondentAuthenticatedEvent = new UacAuthenticateEvent();
      respondentAuthenticatedEvent.setHeader(
          buildHeader(EventTopic.UAC_AUTHENTICATE, sendInfo.getSource(), sendInfo.getChannel()));
      respondentAuthenticatedEvent
          .getPayload()
          .setResponse((UacAuthenticateResponse) sendInfo.getPayload());
      return respondentAuthenticatedEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, UacAuthenticateEvent.class);
      EventPayload payload = ((UacAuthenticateEvent) genericEvent).getPayload().getResponse();
      return build(genericEvent, payload);
    }

    @Override
    EventPayload createPayload(String json) {
      return deserialisePayloadJson(json, UacAuthenticateResponse.class);
    }
  }

  public static class CaseUpdateBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      CaseEvent caseEvent = new CaseEvent();
      caseEvent.setHeader(
          buildHeader(EventTopic.CASE_UPDATE, sendInfo.getSource(), sendInfo.getChannel()));
      CasePayload casePayload = new CasePayload((CollectionCase) sendInfo.getPayload());
      caseEvent.setPayload(casePayload);
      return caseEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, CaseEvent.class);
      EventPayload payload = ((CaseEvent) genericEvent).getPayload().getCollectionCase();
      return build(genericEvent, payload);
    }

    @Override
    EventPayload createPayload(String json) {
      return deserialisePayloadJson(json, CollectionCase.class);
    }
  }

  public static class RefusalBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      RefusalEvent respondentRefusalEvent = new RefusalEvent();
      respondentRefusalEvent.setHeader(
          buildHeader(EventTopic.REFUSAL, sendInfo.getSource(), sendInfo.getChannel()));
      RefusalPayload respondentRefusalPayload =
          new RefusalPayload((RefusalDetails) sendInfo.getPayload());
      respondentRefusalEvent.setPayload(respondentRefusalPayload);
      return respondentRefusalEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, RefusalEvent.class);
      EventPayload payload = ((RefusalEvent) genericEvent).getPayload().getRefusal();
      return build(genericEvent, payload);
    }

    @Override
    EventPayload createPayload(String json) {
      return deserialisePayloadJson(json, RefusalDetails.class);
    }
  }

  public static class UacUpdateBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      UacEvent uacEvent = new UacEvent();
      uacEvent.setHeader(
          buildHeader(EventTopic.UAC_UPDATE, sendInfo.getSource(), sendInfo.getChannel()));
      UacPayload uacPayload = new UacPayload((UAC) sendInfo.getPayload());
      uacEvent.setPayload(uacPayload);
      return uacEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, UacEvent.class);
      EventPayload payload = ((UacEvent) genericEvent).getPayload().getUac();
      return build(genericEvent, payload);
    }

    @Override
    EventPayload createPayload(String json) {
      return deserialisePayloadJson(json, UAC.class);
    }
  }

  public static class SurveyUpdateBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      SurveyUpdateEvent surveyUpdateEvent = new SurveyUpdateEvent();
      surveyUpdateEvent.setHeader(
          buildHeader(EventTopic.SURVEY_UPDATE, sendInfo.getSource(), sendInfo.getChannel()));
      SurveyUpdatePayload surveyUpdatePayload =
          new SurveyUpdatePayload((SurveyUpdate) sendInfo.getPayload());
      surveyUpdateEvent.setPayload(surveyUpdatePayload);
      return surveyUpdateEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, SurveyUpdateEvent.class);
      EventPayload payload = ((SurveyUpdateEvent) genericEvent).getPayload().getSurveyUpdate();
      return build(genericEvent, payload);
    }

    @Override
    EventPayload createPayload(String json) {
      return deserialisePayloadJson(json, SurveyUpdate.class);
    }
  }

  public static class CollectionExerciseUpdateBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      CollectionExerciseUpdateEvent collectionExerciseUpdateEvent =
          new CollectionExerciseUpdateEvent();
      collectionExerciseUpdateEvent.setHeader(
          buildHeader(
              EventTopic.COLLECTION_EXERCISE_UPDATE, sendInfo.getSource(), sendInfo.getChannel()));
      CollectionExerciseUpdatePayload collectionExerciseUpdateEventPayload =
          new CollectionExerciseUpdatePayload((CollectionExercise) sendInfo.getPayload());
      collectionExerciseUpdateEvent.setPayload(collectionExerciseUpdateEventPayload);
      return collectionExerciseUpdateEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, CollectionExerciseUpdateEvent.class);
      EventPayload payload =
          ((CollectionExerciseUpdateEvent) genericEvent).getPayload().getCollectionExerciseUpdate();
      return build(genericEvent, payload);
    }

    @Override
    EventPayload createPayload(String json) {
      return deserialisePayloadJson(json, CollectionExercise.class);
    }
  }

  public static class NewCaseBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      NewCaseEvent newCaseEvent = new NewCaseEvent();
      newCaseEvent.setHeader(
          buildHeader(EventTopic.NEW_CASE, sendInfo.getSource(), sendInfo.getChannel()));
      NewCasePayload newCasePayload =
          new NewCasePayload((NewCasePayloadContent) sendInfo.getPayload());
      newCaseEvent.setPayload(newCasePayload);
      return newCaseEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, NewCaseEvent.class);
      EventPayload payload = ((NewCaseEvent) genericEvent).getPayload().getNewCase();
      return build(genericEvent, payload);
    }

    @Override
    EventPayload createPayload(String json) {
      return deserialisePayloadJson(json, NewCasePayloadContent.class);
    }
  }
}
