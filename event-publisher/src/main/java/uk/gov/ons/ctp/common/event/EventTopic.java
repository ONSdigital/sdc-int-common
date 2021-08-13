package uk.gov.ons.ctp.common.event;

import lombok.Getter;

@Getter
public enum EventTopic {
  FULFILMENT("event_fulfilment", EventType.FULFILMENT),
  UAC_AUTHENTICATE("event_uac-authenticate", EventType.UAC_AUTHENTICATE),
  SURVEY_LAUNCH("event_survey-launch", EventType.SURVEY_LAUNCH),
  REFUSAL("event_refusal", EventType.REFUSAL),
  UAC_UPDATE("event_uac-update", EventType.UAC_UPDATE),
  CASE_UPDATE("event_case-update", EventType.CASE_UPDATE);

  private String topic;
  private EventType type;

  private EventTopic(String topic, EventType type) {
    this.topic = topic;
    this.type = type;
  }

  public static EventTopic forType(EventType type) {
    for (EventTopic routingKey : values()) {
      if (routingKey.type == type) {
        return routingKey;
      }
    }
    return null;
  }
}
