package uk.gov.ons.ctp.common.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.EventBuilder.SendInfo;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.persistence.EventBackupData;
import uk.gov.ons.ctp.common.event.persistence.EventPersistence;

/** Service responsible for the publication of events. */
@Slf4j
public class EventPublisher {

  private EventSender sender;
  private CircuitBreaker circuitBreaker;

  private EventPersistence eventPersistence;

  private EventPublisher(
      EventSender eventSender, EventPersistence eventPersistence, CircuitBreaker circuitBreaker) {
    this.sender = eventSender;
    this.eventPersistence = eventPersistence;
    this.circuitBreaker = circuitBreaker;
  }

  /**
   * Create method for creating an EventPublisher that will not attempt to persist events following
   * underlying publish failure.
   *
   * @param eventSender the impl of EventSender that will be used to ... send the event.
   * @return an EventPubisher object.
   */
  public static EventPublisher createWithoutEventPersistence(EventSender eventSender) {
    return new EventPublisher(eventSender, null, null);
  }

  /**
   * Create method for creating an EventPublisher that will persist events following a publishing
   * failure. If publishing fails and the event is successfully persisted then all will appear well
   * to the caller, with the only indication of the failure being that an error is logged.
   *
   * @param eventSender the impl of EventSender that will be used to ... send the event.
   * @param eventPersistence is an EventPersistence implementation which does the actual event
   *     persistence.
   * @param circuitBreaker circuit breaker object, or null if not required.
   * @return an EventPubisher object.
   */
  public static EventPublisher createWithEventPersistence(
      EventSender eventSender, EventPersistence eventPersistence, CircuitBreaker circuitBreaker) {
    return new EventPublisher(eventSender, eventPersistence, circuitBreaker);
  }

  /**
   * Method to publish an event.
   *
   * <p>If no EventPersister has been set then a publishing failure results in an exception being
   * thrown.
   *
   * <p>If an EventPersister is set then in the event of a publish failure it will attempt to save
   * the event into a persistent store. If event is persisted then this method returns as normal
   * with no exception. If event persistence fails then an error is logged and an exception is
   * thrown.
   *
   * @param eventType the event type
   * @param source the source
   * @param channel the channel
   * @param payload message payload for event
   * @return String UUID transaction Id for event
   */
  public String sendEvent(
      EventType eventType, Source source, Channel channel, EventPayload payload) {
    log.debug(
        "Enter sendEvent",
        kv("eventType", eventType),
        kv("source", source),
        kv("channel", channel),
        kv("payload", payload));

    String transactionId = doSendEvent(eventType, new SendInfo(payload, source, channel));

    log.debug(
        "Exit sendEvent",
        kv("eventType", eventType),
        kv("source", source),
        kv("channel", channel),
        kv("payload", payload));

    return transactionId;
  }

  /**
   * Method to publish an event given EventPayload as a JSON string.
   *
   * <p>See javadoc for <code>sendEvent</code> with <code>EventPayload</code> object, which this
   * method calls.
   *
   * @param eventType the event type
   * @param source the source
   * @param channel the channel
   * @param jsonEventPayload message payload for event as JSON String.
   * @return String UUID transaction Id for event
   */
  public String sendEvent(
      EventType eventType, Source source, Channel channel, String jsonEventPayload) {
    EventPayload payload = eventType.getBuilder().createPayload(jsonEventPayload);
    return sendEvent(eventType, source, channel, payload);
  }

  /**
   * Send a backup event that would have previously been stored in cloud data storage.
   *
   * @param event backup event , typically recovered from firestore.
   * @return String UUID transaction Id for event
   */
  public String sendEvent(EventBackupData event) {
    EventType type = event.getEventType();
    SendInfo sendInfo = type.getBuilder().create(event.getEvent());
    if (sendInfo == null) {
      log.error("Unrecognised event type", kv("type", type));
      throw new UnsupportedOperationException("Unknown event: " + type);
    }
    String transactionId = doSendEvent(type, sendInfo);
    log.debug("Sent {} with transactionId {}", event.getEventType(), transactionId);
    return transactionId;
  }

  private String doSendEvent(EventType eventType, SendInfo sendInfo) {
    EventPayload payload = sendInfo.getPayload();

    if (!payload.getClass().equals(eventType.getPayloadType())) {
      log.error(
          "Payload incompatible for event type",
          kv("payloadType", payload.getClass()),
          kv("eventType", eventType));
      String errorMessage =
          "Payload type '"
              + payload.getClass()
              + "' incompatible for event type '"
              + eventType
              + "'";
      throw new IllegalArgumentException(errorMessage);
    }

    EventTopic eventTopic = EventTopic.forType(eventType);
    if (eventTopic == null) {
      log.error("Routing key for eventType not configured", kv("eventType", eventType));
      String errorMessage = "Routing key for eventType '" + eventType + "' not configured";
      throw new UnsupportedOperationException(errorMessage);
    }

    GenericEvent genericEvent = eventType.getBuilder().create(sendInfo);
    if (genericEvent == null) {
      log.error("Payload for eventType not configured", kv("eventType", eventType));
      String errorMessage =
          payload.getClass().getName() + " for EventType '" + eventType + "' not supported yet";
      throw new UnsupportedOperationException(errorMessage);
    }

    try {
      publish(eventTopic, genericEvent);
    } catch (Exception e) {
      boolean backup = eventPersistence != null;
      log.error(
          "Failed to send event but will now backup to firestore",
          kv("eventType", eventType),
          kv("eventTopic", eventTopic),
          kv("backup", backup),
          e);
      if (!backup) {
        throw new EventPublishException("Failed to publish event", e);
      }

      // Save event to persistent store
      try {
        eventPersistence.persistEvent(eventType, genericEvent);
        log.info(
            "Event data saved to persistent store",
            kv("eventType", eventType),
            kv("eventTopic", eventTopic));
      } catch (Exception epe) {
        // There is no hope. Neither pub/sub or Persistence are working
        log.error(
            "Backup event persistence failed following publish failure",
            kv("eventType", eventType),
            kv("eventTopic", eventTopic),
            epe);
        throw new EventPublishException(
            "Backup event persistence failed following publish failure", e);
      }
    }

    return genericEvent.getHeader().getMessageId().toString();
  }

  private void publish(EventTopic eventTopic, GenericEvent genericEvent) {
    if (circuitBreaker == null) {
      publish(eventTopic, genericEvent, "");
    } else {
      try {
        this.circuitBreaker.run(
            () -> {
              publish(eventTopic, genericEvent, "within circuit-breaker");
              return null;
            },
            throwable -> {
              throw new EventCircuitBreakerException(throwable);
            });
      } catch (EventCircuitBreakerException e) {
        log.debug("{}: {}", e.getMessage(), e.getCause().getMessage());
        throw e;
      }
    }
  }

  private void publish(EventTopic eventTopic, GenericEvent genericEvent, String loggingMsgSuffix) {
    EventType eventType = eventTopic.getType();

    log.info(
        "Publishing message " + loggingMsgSuffix,
        kv("eventType", eventType),
        kv("eventTopic", eventTopic));

    sender.sendEvent(eventTopic, genericEvent);

    log.info(
        "Message successfully published", kv("eventType", eventType), kv("eventTopic", eventTopic));
  }
}
