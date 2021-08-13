package uk.gov.ons.ctp.common.event;

import uk.gov.ons.ctp.common.event.model.GenericEvent;

public interface EventSender {

  void sendEvent(EventTopic routingKey, GenericEvent genericEvent);

  default void close() throws Exception {}
}
