package uk.gov.ons.ctp.common.event.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.ons.ctp.common.event.TopicType;

/**
 * This class holds data about an event which failed to send.
 *
 * <p>If the underlying send mechanism fails to send an event then an instance of this object is
 * persisted into the backup event collection in Firestore for later resending.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EventBackupData {
  private TopicType topicType;
  private Long messageFailureDateTimeInMillis;
  private Long messageSentDateTimeInMillis;
  private String id;
  private String event;
}
