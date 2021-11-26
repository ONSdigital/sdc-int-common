package uk.gov.ons.ctp.common.event;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum EventTopic {
  FULFILMENT("event_fulfilment", TopicType.FULFILMENT),
  UAC_AUTHENTICATION("event_uac-authentication", TopicType.UAC_AUTHENTICATION),
  EQ_LAUNCH("event_eq-launch", TopicType.EQ_LAUNCH),
  REFUSAL("event_refusal", TopicType.REFUSAL),
  UAC_UPDATE("event_uac-update", TopicType.UAC_UPDATE),
  CASE_UPDATE("event_case-update", TopicType.CASE_UPDATE),
  SURVEY_UPDATE("event_survey-update", TopicType.SURVEY_UPDATE),
  COLLECTION_EXERCISE_UPDATE(
      "event_collection-exercise-update", TopicType.COLLECTION_EXERCISE_UPDATE),
  NEW_CASE("event_new-case", TopicType.NEW_CASE);

  @JsonValue private String topic;
  private TopicType type;

  private EventTopic(String topic, TopicType type) {
    this.topic = topic;
    this.type = type;
  }

  public static EventTopic forType(TopicType type) {
    for (EventTopic eventTopic : values()) {
      if (eventTopic.type == type) {
        return eventTopic;
      }
    }
    return null;
  }
}
