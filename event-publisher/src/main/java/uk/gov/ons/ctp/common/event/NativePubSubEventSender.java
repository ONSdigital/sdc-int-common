package uk.gov.ons.ctp.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

public class NativePubSubEventSender implements EventSender {

  private ObjectMapper objectMapper;
  private String projectId;
  private TransportChannelProvider channelProvider;
  private CredentialsProvider credentialsProvider;
  private boolean usePubSub;

  public NativePubSubEventSender(
      String projectId,
      TransportChannelProvider channelProvider,
      CredentialsProvider credentialsProvider, Boolean usePubSub)
      throws CTPException {
    this.projectId = projectId;
    this.channelProvider = channelProvider;
    this.credentialsProvider = credentialsProvider;
    this.usePubSub = usePubSub;
    objectMapper = new CustomObjectMapper();
  }

  @SneakyThrows
  @Override
  public void sendEvent(EventTopic topic, GenericEvent genericEvent) {

    TopicName topicName = TopicName.of(projectId, topic.getTopic());
    Publisher publisher = null;
    try {
      // Create a publisher instance with default settings bound to the topic
      if (usePubSub) {
        publisher =
            Publisher.newBuilder(topicName)
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
      } else {
        publisher = Publisher.newBuilder(topicName).build();
      }

      ByteString data = ByteString.copyFrom(objectMapper.writeValueAsString(genericEvent), "UTF-8");
      PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

      // Once published, returns a server-assigned message id (unique within the topic)
      ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
      String messageId = messageIdFuture.get();
      System.out.println("Published message ID: " + messageId);
    } catch (IOException | ExecutionException | InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (publisher != null) {
        // When finished with the publisher, shutdown to free up resources.
        publisher.shutdown();
        publisher.awaitTermination(1, TimeUnit.MINUTES);
      }
    }
  }
}
