package uk.gov.ons.ctp.common.cloud;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FirestoreProviderImpl implements FirestoreProvider {
  @Value("${spring.cloud.gcp.firestore.project-id}")
  private String gcpProject;

  private Firestore firestore;

  @PostConstruct
  public void create() {
    log.info("Connecting to Firestore project {}", gcpProject);
    firestore = FirestoreOptions.newBuilder().setProjectId(gcpProject).build().getService();
  }

  @Override
  public Firestore get() {
    return firestore;
  }
}
