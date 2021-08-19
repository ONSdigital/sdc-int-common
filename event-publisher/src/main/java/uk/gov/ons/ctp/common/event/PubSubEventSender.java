package uk.gov.ons.ctp.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.util.concurrent.ListenableFuture;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

public class PubSubEventSender implements EventSender {
  private static final ObjectMapper OBJECT_MAPPER = new CustomObjectMapper();
  private final PubSubTemplate pubSubTemplate;
  private int timeout;

  public PubSubEventSender(PubSubTemplate pubSubTemplate, int timeout) {
    this.pubSubTemplate = pubSubTemplate;
    this.timeout = timeout;
  }

  @Override
  public void sendEvent(EventTopic routingKey, GenericEvent genericEvent) {
    String body = convertObjectToJson(genericEvent);
    String topic = routingKey.getTopic();

    PubsubMessage pubsubMessage =
        PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(body)).build();

    ListenableFuture<String> future = pubSubTemplate.publish(topic, pubsubMessage);

    try {
      future.get(timeout, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  private String convertObjectToJson(Object obj) {
    try {
      return OBJECT_MAPPER.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed converting Object To Json", e);
    }
  }
}
