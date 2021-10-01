package uk.gov.ons.ctp.common.event;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

@Getter
public enum EventTopic {
  FULFILMENT("event_fulfilment", EventType.FULFILMENT),
  UAC_AUTHENTICATE("event_uac-authenticate", EventType.UAC_AUTHENTICATE),
  SURVEY_LAUNCH("event_survey-launch", EventType.SURVEY_LAUNCH),
  REFUSAL("event_refusal", EventType.REFUSAL),
  UAC_UPDATE("event_uac-update", EventType.UAC_UPDATE),
  CASE_UPDATE("event_case-update", EventType.CASE_UPDATE),
  SURVEY_UPDATE("event_survey-update", EventType.SURVEY_UPDATE),
  COLLECTION_EXERCISE_UPDATE(
      "event_collection-exercise-update", EventType.COLLECTION_EXERCISE_UPDATE),
  NEW_CASE("event_case-update", EventType.NEW_CASE);

  @JsonValue
  private String topic;
  private EventType type;

  private EventTopic(String topic, EventType type) {
    this.topic = topic;
    this.type = type;
  }

  public static EventTopic forType(EventType type) {
    for (EventTopic eventTopic : values()) {
      if (eventTopic.type == type) {
        return eventTopic;
      }
    }
    return null;
  }
}
