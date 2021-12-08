package uk.gov.ons.ctp.common.event.persistence;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import com.fasterxml.jackson.core.JsonProcessingException;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublishException;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

/** Save details into Firestore, about an event which failed to publish. */
@Slf4j
@Service
public class FirestoreEventPersistence implements EventPersistence {
  private RetryableCloudDataStore cloudDataStore;
  private CustomObjectMapper objectMapper;

  @Value("${spring.cloud.gcp.firestore.project-id}")
  String gcpProject;

  @Value("${cloud-storage.event-backup-schema-name}")
  String eventBackupSchemaName;

  String eventBackupSchema;

  @PostConstruct
  public void init() {
    eventBackupSchema = gcpProject + "-" + eventBackupSchemaName.toLowerCase();
  }

  @Autowired
  public FirestoreEventPersistence(
      RetryableCloudDataStore cloudDataStore, CustomObjectMapper objectMapper) {
    this.cloudDataStore = cloudDataStore;
    this.objectMapper = objectMapper;
  }

  private String serialise(Object obj) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new EventPublishException("Failed to serialise event to JSON", e);
    }
  }

  @Override
  public void persistEvent(TopicType topicType, GenericEvent genericEvent) throws CTPException {
    String id = genericEvent.getHeader().getMessageId().toString();

    log.debug("Storing event data in Firestore", kv("id", id));

    EventBackupData eventData = new EventBackupData();
    eventData.setTopicType(topicType);
    eventData.setMessageFailureDateTimeInMillis(System.currentTimeMillis());
    eventData.setId(id);
    eventData.setEvent(serialise(genericEvent));

    cloudDataStore.storeObject(
        eventBackupSchema,
        genericEvent.getHeader().getMessageId().toString(),
        eventData,
        genericEvent.getHeader().getMessageId().toString());

    log.debug("Stored event data", kv("id", id));
  }
}
