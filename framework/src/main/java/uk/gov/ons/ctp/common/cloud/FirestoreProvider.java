package uk.gov.ons.ctp.common.cloud;

import com.google.cloud.firestore.Firestore;

public interface FirestoreProvider {
  Firestore get();
}
