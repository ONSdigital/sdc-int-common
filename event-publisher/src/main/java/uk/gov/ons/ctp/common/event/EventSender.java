package uk.gov.ons.ctp.common.event;

import uk.gov.ons.ctp.common.event.model.GenericEvent;

public interface EventSender {

  void sendEvent(EventTopic eventTopic, GenericEvent genericEvent);

  default void close() throws Exception {}
}
