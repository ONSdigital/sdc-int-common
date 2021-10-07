package uk.gov.ons.ctp.common.event.persistence;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

public interface EventPersistence {
  void persistEvent(TopicType topicType, GenericEvent genericEvent) throws CTPException;
}
