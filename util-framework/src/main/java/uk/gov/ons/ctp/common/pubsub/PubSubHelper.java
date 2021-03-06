package uk.gov.ons.ctp.common.pubsub;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.pubsub.v1.SeekRequest;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.threeten.bp.Duration;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventTopic;
import uk.gov.ons.ctp.common.event.NativePubSubEventSender;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.EventPayload;

@Slf4j
public class PubSubHelper {
  private static final long DEFAULT_TIMEOUT_MS = 3000;

  private static PubSubHelper instance = null;
  private static ManagedChannel channel;
  private static TransportChannelProvider channelProvider;
  private static CredentialsProvider credentialsProvider;

  private SubscriberStubSettings defaultSubscriberStubSettings;
  private EventPublisher eventPublisher;
  private String projectId;
  private boolean useEmulatorPubSub;
  private ObjectMapper mapper = new ObjectMapper();

  private PubSubHelper(
      String projectId,
      boolean addRmProperties,
      boolean useEmulatorPubSub,
      String emulatorPubSubHost)
      throws CTPException {
    try {
      this.useEmulatorPubSub = useEmulatorPubSub;

      log.info("is configured for PubSub Emulator?: " + emulatorPubSubHost);
      if (useEmulatorPubSub) {
        channel = ManagedChannelBuilder.forTarget(emulatorPubSubHost).usePlaintext().build();
        channelProvider =
            FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        credentialsProvider = NoCredentialsProvider.create();
      }
      NativePubSubEventSender sender =
          new NativePubSubEventSender(
              projectId, channelProvider, credentialsProvider, useEmulatorPubSub);
      eventPublisher = EventPublisher.createWithoutEventPersistence(sender);

      defaultSubscriberStubSettings =
          buildSubscriberStubSettings(useEmulatorPubSub, emulatorPubSubHost);

      this.projectId = projectId;
    } catch (IOException e) {
      String errorMessage = "Failed to create subscription";
      log.error(errorMessage, e);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, e, errorMessage);
    }
  }

  /**
   * Create instance of PubSubHelper.
   *
   * @param projectId produc id
   * @param addRmProperties true to add RM properties
   * @param useEmulatorPubSub true if using emulator
   * @param emulatorPubSubHost emulator host
   * @return instance
   * @throws CTPException on error
   */
  public static synchronized PubSubHelper instance(
      String projectId,
      boolean addRmProperties,
      boolean useEmulatorPubSub,
      String emulatorPubSubHost)
      throws CTPException {

    if (instance == null) {
      instance =
          new PubSubHelper(projectId, addRmProperties, useEmulatorPubSub, emulatorPubSubHost);
    }
    return instance;
  }

  /** Cleans up and destroys instance of PubSubHelper. */
  public static synchronized void destroy() {
    if (channel != null) {
      channel.shutdown();
    }
    instance = null;
  }

  /**
   * Creates the Subscription that PubSubHelper is using to listen to the given topicType.
   *
   * @param topicType the type of the event that PubSubHelper has a Subscription listening to.
   * @return subscription ID
   * @throws CTPException on error
   */
  public synchronized String createSubscription(
      TopicType topicType, SubscriptionSuffix subscriptionSuffix) throws CTPException {
    EventTopic eventTopic = EventTopic.forType(topicType);
    String subscriptionId = buildSubscriberId(topicType, subscriptionSuffix);
    try {
      SubscriberStub subscriberStub = GrpcSubscriberStub.create(defaultSubscriberStubSettings);
      SubscriptionAdminClient subscriptionAdminClient =
          SubscriptionAdminClient.create(subscriberStub);

      verifyAndCreateSubscription(
          subscriptionAdminClient, projectId, eventTopic.getTopic(), subscriptionId);
      return subscriptionId;
    } catch (IOException e) {
      String errorMessage = "Failed to create subscription";
      log.error(errorMessage, e);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, e, errorMessage);
    }
  }

  /**
   * Flushes the Subscription that PubSubHelper is using to listen to the given topicType.
   *
   * @param topicType is the type of the event that PubSubHelper has a Subscription listening to.
   * @throws CTPException on error
   */
  public synchronized void flushSubscription(
      TopicType topicType, SubscriptionSuffix subscriptionSuffix) throws CTPException {
    try {
      // Creates the subscription only if it doesnt exist
      createSubscription(topicType, subscriptionSuffix);

      SubscriberStub subscriberStub = GrpcSubscriberStub.create(defaultSubscriberStubSettings);
      SubscriptionAdminClient subscriptionAdminClient =
          SubscriptionAdminClient.create(subscriberStub);

      String subscriptionId = buildSubscriberId(topicType, subscriptionSuffix);
      String subscriptionName =
          ProjectSubscriptionName.of(this.projectId, subscriptionId).toString();

      // Googles method to purge Subscriptions
      // ref: https://cloud.google.com/pubsub/docs/replay-overview
      // ref:
      // https://stackoverflow.com/questions/39398173/best-practices-for-draining-or-clearing-a-google-cloud-pubsub-topic/39398346
      Instant now = Instant.now();
      Timestamp timestamp =
          Timestamp.newBuilder().setSeconds(now.getEpochSecond()).setNanos(now.getNano()).build();
      SeekRequest seekRequest =
          SeekRequest.newBuilder().setSubscription(subscriptionName).setTime(timestamp).build();
      subscriptionAdminClient.seek(seekRequest);
    } catch (IOException e) {
      String errorMessage = "Failed to flush subscription";
      log.error(errorMessage, e);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, e, errorMessage);
    }
  }

  /**
   * Deletes Subscription that PubSubHelper is using to listen to the given topicType.
   *
   * @param topicType is the type of the event that PubSubHelper has a Subscription listening to.
   * @return subscripton ID
   * @throws CTPException on error
   */
  public synchronized String deleteSubscription(
      TopicType topicType, SubscriptionSuffix subscriptionSuffix) throws CTPException {
    String subscriptionId = buildSubscriberId(topicType, subscriptionSuffix);
    deleteSubscription(subscriptionId);
    return subscriptionId;
  }

  private void deleteSubscription(String subscriptionId) throws CTPException {
    try {
      SubscriberStub subscriberStub = GrpcSubscriberStub.create(defaultSubscriberStubSettings);
      SubscriptionAdminClient subscriptionAdminClient =
          SubscriptionAdminClient.create(subscriberStub);

      if (subscriptionExists(subscriptionAdminClient, projectId, subscriptionId)) {
        ProjectSubscriptionName subscriptionName =
            ProjectSubscriptionName.of(projectId, subscriptionId);
        subscriptionAdminClient.deleteSubscription(subscriptionName);
      }
    } catch (IOException e) {
      String errorMessage = "Failed to delete subscription";
      log.error(errorMessage, e);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, e, errorMessage);
    }
  }

  /**
   * Publish a message to a pubsub topic.
   *
   * @param topicType is the type of the event that is being sent.
   * @param source states who is sending, or pretending, to set the message.
   * @param channel holds a channel identifier.
   * @param payload is the object to be sent.
   * @return the message id generated for the published message.
   * @throws CTPException if anything went wrong.
   */
  public synchronized UUID sendEvent(
      TopicType topicType, Source source, Channel channel, EventPayload payload)
      throws CTPException {
    try {
      UUID messageId = eventPublisher.sendEvent(topicType, source, channel, payload);
      return messageId;

    } catch (Exception e) {
      String errorMessage = "Failed to send message. Cause: " + e.getMessage();
      log.error(
          errorMessage,
          kv("topicType", topicType),
          kv("source", source),
          kv("channel", channel),
          e);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, errorMessage, e);
    }
  }

  /**
   * Reads a message from the named subscription and convert it to a Java object. This method will
   * wait for up to the specified number of milliseconds for a message to appear on the
   * subscription.
   *
   * @param <T> is the class of object we are expected to recieve.
   * @param topicType is the name of the topic to read from.
   * @param clazz is the class that the message should be converted to.
   * @param maxWaitTimeMillis is the maximum amount of time the caller is prepared to wait for the
   *     message to appear.
   * @return an object of the specified type, or null if no message was found before the timeout
   *     expired.
   * @throws CTPException if PubSub threw an exception when we attempted to read a message.
   */
  public <T> T getMessage(
      TopicType topicType,
      Class<T> clazz,
      long maxWaitTimeMillis,
      SubscriptionSuffix subscriptionSubstring)
      throws CTPException {
    String subscriberName = buildSubscriberId(topicType, subscriptionSubstring);

    String message = getMessage(subscriberName, maxWaitTimeMillis);

    // Return to caller if nothing read from subscription
    if (message == null) {
      log.info(
          "PubSub getMessage. Message is null. Unable to convert to class '"
              + clazz.getName()
              + "'");
      return null;
    }
    // Use Jackson to convert from a Json message to a Java object
    try {
      log.info("Rabbit getMessage. Converting result into class '" + clazz.getName() + "'");
      return mapper.readValue(message, clazz);

    } catch (IOException e) {
      String errorMessage = "Failed to convert message to object of type '" + clazz.getName() + "'";
      log.error(errorMessage, kv("subscription", subscriberName), e);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, e, errorMessage);
    }
  }

  /**
   * Reads a message from the named subscription. This method will wait for up to the specified
   * number of milliseconds for a message to appear on the subscription.
   *
   * @param subscription is the name of the subscription to read from.
   * @param maxWaitTimeMillis is the maximum amount of time the caller is prepared to wait for the
   *     message to appear.
   * @return a String containing the content of the message body, or null if no message was found
   *     before the timeout expired.
   * @throws CTPException if PubSub threw an exception when we attempted to read a message.
   */
  public String getMessage(String subscription, long maxWaitTimeMillis) throws CTPException {
    final long startTime = System.currentTimeMillis();
    final long timeoutLimit = startTime + maxWaitTimeMillis;

    log.info(
        "PubSub getMessage. Reading from subscription '"
            + subscription
            + "'"
            + " within "
            + maxWaitTimeMillis
            + "ms");

    // Keep trying to read a message from pubsub, or we timeout waiting
    String messageBody;
    do {
      messageBody = retrieveMessage(subscription, maxWaitTimeMillis);
      if (messageBody != null) {
        log.info("Message read from subscription");
        break;
      }

      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        break;
      }
    } while (messageBody == null && System.currentTimeMillis() < timeoutLimit);

    return messageBody;
  }

  private synchronized String retrieveMessage(String subscription, long wait) throws CTPException {
    try {
      SubscriberStubSettings subscriberStubSettings = buildSubscriberStubSettings(wait);
      String result = syncPullMessage(subscriberStubSettings, projectId, subscription);
      return result == null ? null : new String(result);
    } catch (IOException e) {
      String errorMessage = "Failed to create subscription";
      log.error(errorMessage, e);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, e, errorMessage);
    }
  }

  // ref: https://cloud.google.com/pubsub/docs/pull#synchronous_pull
  private static String syncPullMessage(
      SubscriberStubSettings subscriberStubSettings, String projectId, String subscription)
      throws CTPException {
    String msg = null;
    try {
      try (SubscriberStub subscriber = GrpcSubscriberStub.create(subscriberStubSettings)) {
        String subscriptionName = ProjectSubscriptionName.format(projectId, subscription);
        PullRequest pullRequest =
            PullRequest.newBuilder().setMaxMessages(1).setSubscription(subscriptionName).build();

        // Use pullCallable().futureCall to asynchronously perform this operation.
        PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);

        if (!pullResponse.getReceivedMessagesList().isEmpty()) {
          ReceivedMessage rm = pullResponse.getReceivedMessagesList().get(0);
          String ackId = rm.getAckId();
          msg = rm.getMessage().getData().toString("UTF-8");

          // Acknowledge received messages.
          AcknowledgeRequest acknowledgeRequest =
              AcknowledgeRequest.newBuilder()
                  .setSubscription(subscriptionName)
                  .addAllAckIds(Collections.singleton(ackId))
                  .build();
          // Use acknowledgeCallable().futureCall to asynchronously perform this operation.
          subscriber.acknowledgeCallable().call(acknowledgeRequest);
        }
      }
      return msg;
    } catch (IOException e) {
      String errorMessage = "Failed to flush subscription '" + subscription + "'";
      log.error(errorMessage, kv("subscription", subscription), e);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, e, errorMessage);
    }
  }

  public static void verifyAndCreateSubscription(
      SubscriptionAdminClient subscriptionAdminClient,
      String projectId,
      String topic,
      String subscriptionId)
      throws IOException {
    if (!subscriptionExists(subscriptionAdminClient, projectId, subscriptionId)) {
      TopicName topicName = TopicName.of(projectId, topic);
      ProjectSubscriptionName subscriptionName =
          ProjectSubscriptionName.of(projectId, subscriptionId);
      subscriptionAdminClient.createSubscription(
          subscriptionName, topicName, PushConfig.getDefaultInstance(), 10);
    }
  }

  private static boolean subscriptionExists(
      SubscriptionAdminClient subscriptionAdminClient, String projectId, String subscriptionId) {
    ProjectName pn = ProjectName.of(projectId);
    SubscriptionAdminClient.ListSubscriptionsPagedResponse listSubscriptionsPagedResponse =
        subscriptionAdminClient.listSubscriptions(pn);
    boolean exists = false;
    for (Subscription subscription : listSubscriptionsPagedResponse.iterateAll()) {
      if (subscription.getName().endsWith("/" + subscriptionId)) {
        exists = true;
        break;
      }
    }
    return exists;
  }

  private static String buildSubscriberId(
      TopicType topicType, SubscriptionSuffix subscriptionSuffix) {
    EventTopic eventTopic = EventTopic.forType(topicType);
    if (eventTopic == null) {
      String errorMessage = "Topic for topicType '" + topicType + "' not configured";
      log.error(errorMessage, kv("topicType", topicType));
      throw new UnsupportedOperationException(errorMessage);
    }

    return eventTopic.getTopic() + subscriptionSuffix.getSuffix();
  }

  private SubscriberStubSettings buildSubscriberStubSettings(long wait) throws IOException {
    if (useEmulatorPubSub) {
      return buildEmulatorSubscriberStubSettings(wait);
    } else {
      return buildCloudSubscriberStubSettings(wait);
    }
  }

  private SubscriberStubSettings buildSubscriberStubSettings(
      boolean isForEmulator, String emulatorPubSubHost) throws IOException {
    return buildSubscriberStubSettings(DEFAULT_TIMEOUT_MS);
  }

  private SubscriberStubSettings buildCloudSubscriberStubSettings(long wait) throws IOException {
    Duration timeout = Duration.ofMillis(wait);

    SubscriberStubSettings.Builder builder = SubscriberStubSettings.newBuilder();
    builder.pullSettings().setSimpleTimeoutNoRetries(timeout);
    SubscriberStubSettings subscriberStubSettings =
        builder
            .setTransportChannelProvider(
                SubscriberStubSettings.defaultGrpcTransportProviderBuilder()
                    .setKeepAliveTime(Duration.ofSeconds(1))
                    .build())
            .build();
    return subscriberStubSettings;
  }

  private SubscriberStubSettings buildEmulatorSubscriberStubSettings(long wait) throws IOException {
    Duration timeout = Duration.ofMillis(wait);
    SubscriberStubSettings.Builder builder = SubscriberStubSettings.newBuilder();
    builder.pullSettings().setSimpleTimeoutNoRetries(timeout);
    SubscriberStubSettings subscriberStubSettings =
        builder
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();

    return subscriberStubSettings;
  }
}
