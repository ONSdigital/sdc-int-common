package uk.gov.ons.ctp.common.cloud;

import static java.util.stream.Collectors.toList;
import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

@Slf4j
@Service
public class FirestoreDataStore implements CloudDataStore {

  @Autowired private FirestoreProvider provider;

  /**
   * Write object to Firestore collection. If the collection already holds an object with the
   * specified key then the contents of the value will be overwritten.
   *
   * @param schema - holds the name of the collection that the object will be added to.
   * @param key - identifies the object within the collection.
   * @param value - is the object to be written to Firestore. To be storable/retrievable by
   *     Firestore it must either have public fields or provide get/set methods that allow access to
   *     its contents.
   * @throws CTPException if any failure was detected interacting with Firestore.
   * @throws DataStoreContentionException if the object was not stored but should be retried with an
   *     exponential backoff.
   */
  @Override
  public void storeObject(final String schema, final String key, final Object value)
      throws CTPException, DataStoreContentionException {
    log.info("Saving object to Firestore", kv("schema", schema), kv("key", key));

    // Store the object
    ApiFuture<WriteResult> result = provider.get().collection(schema).document(key).set(value);

    // Wait for Firestore to complete
    try {
      result.get();
      log.info("Firestore save completed", kv("schema", schema), kv("key", key));
    } catch (Exception e) {
      log.error(
          "Failed to create object in Firestore",
          kv("schema", schema),
          kv("key", key),
          kv("exceptionChain", describeExceptionChain(e)),
          e);

      if (isRetryableFirestoreException(e)) {
        // Firestore is overloaded. Use Spring exponential backoff to force a retry.
        // This is intended to catch 'Too much contention' exceptions and any other
        // Firestore exception where it is worth retrying.
        log.info("Firestore contention detected", kv("schema", schema), kv("key", key));
        throw new DataStoreContentionException(
            "Firestore contention on schema '" + schema + "'", e);
      }

      String failureMessage =
          "Failed to create object in Firestore. Schema: " + schema + " with key " + key;
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
  }

  // This method supports logging which aims to protect against future unexpected changes in
  // how google throw exceptions for retryable operations. If Google change Firestore behaviour
  // and we don't detect a retryable operation then we want our logging to be good enough to
  // allow a code fix.
  private String describeExceptionChain(Throwable e) {
    StringBuilder builder = new StringBuilder();

    while (e != null) {
      builder.append("caused by " + e.getClass().getName() + " ");
      e = e.getCause();
    }

    return builder.toString().trim();
  }

  private boolean isRetryableFirestoreException(Exception e) {
    boolean retryable = false;

    // Traverse the exception chain looking for a StatusRuntimeException
    Throwable t = e;
    while (t != null) {
      if (t instanceof StatusRuntimeException) {
        StatusRuntimeException statusRuntimeException = (StatusRuntimeException) t;
        Code failureCode = statusRuntimeException.getStatus().getCode();

        if (failureCode == Status.RESOURCE_EXHAUSTED.getCode()
            || failureCode == Status.ABORTED.getCode()
            || failureCode == Status.DEADLINE_EXCEEDED.getCode()
            || failureCode == Status.UNAVAILABLE.getCode()) {
          retryable = true;
          break;
        } else {
          log.info(
              "StatusRuntimeException found in exception heirarchy, but it's not a retryable code",
              kv("Status", statusRuntimeException.getStatus()),
              kv("StatusCode", failureCode),
              kv("StatusDescription", statusRuntimeException.getStatus().getDescription()));
        }
      }

      t = t.getCause();
    }

    return retryable;
  }

  /**
   * Read an object from Firestore.
   *
   * @param <T> The object type that results should be returned in.
   * @param target the class of the object type that results should be returned in.
   * @param schema - is the name of the collection which holds the object.
   * @param key - identifies the object within the collection.
   * @return - Optional containing the object if it was found, otherwise the optional will contain
   *     null.
   */
  @Override
  public <T> Optional<T> retrieveObject(Class<T> target, final String schema, final String key)
      throws CTPException {
    log.info("Fetching object from Firestore", kv("schema", schema), kv("key", key));

    // Submit read request to firestore
    FieldPath fieldPathForId = FieldPath.documentId();
    List<T> documents = runSearch(target, schema, fieldPathForId, key);

    // Squash results down to single document
    Optional<T> result = null;
    if (documents.isEmpty()) {
      result = Optional.empty();
      if (log.isDebugEnabled()) {
        log.debug("Search didn't find any objects");
      }
    } else if (documents.size() == 1) {
      result = Optional.of(documents.get(0));
      log.info("Search found single result", kv("schema", schema), kv("key", key));
    } else {
      log.error(
          "Firestore found more than one result object",
          kv("resultsSize", documents.size()),
          kv("schema", schema),
          kv("key", key));
      String failureMessage =
          "Firestore returned more than 1 result object. Returned "
              + documents.size()
              + " objects for Schema '"
              + schema
              + "' with key '"
              + key
              + "'";
      throw new CTPException(Fault.SYSTEM_ERROR, failureMessage);
    }
    return result;
  }

  /**
   * List all objects found in the given schema.
   *
   * @param <T> The object type that results should be returned in.
   * @param target the class of the object type that results should be returned in.
   * @param schema is the schema to search.
   * @return the List of results.
   * @throws CTPException if anything goes wrong.
   */
  @Override
  public <T> List<T> list(Class<T> target, String schema) throws CTPException {
    log.debug("Listing all items in Firestore", kv("schema", schema), kv("target", target));
    try {
      ApiFuture<QuerySnapshot> query = provider.get().collection(schema).get();
      QuerySnapshot querySnapshot = query.get();
      List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
      return documents.stream().map(d -> d.toObject(target)).collect(toList());
    } catch (Exception e) {
      log.error(
          "Failed to list Firestore items {} {}", kv("target", target), kv("schema", schema), e);
      String failureMessage =
          "Failed to list Firestore items. Target class: '"
              + target
              + "', schema: '"
              + schema
              + "'";
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
  }

  /**
   * Runs a firestore object search. This returns objects whose field is equal to the search value.
   *
   * @param <T> The object type that results should be returned in.
   * @param target the class of the object type that results should be returned in.
   * @param schema is the schema to search.
   * @param fieldPathElements is an array of strings that describe the path to the search field. eg,
   *     [ "case", "addresss", "postcode" ]
   * @param searchValue is the value that the field must equal for it to be returned as a result.
   * @return the List of results.
   * @throws CTPException if anything goes wrong.
   */
  public <T> List<T> search(
      Class<T> target, final String schema, String[] fieldPathElements, String searchValue)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug(
          "Searching Firestore",
          kv("schema", schema),
          kv("fieldPathElements", fieldPathElements),
          kv("searchValue", searchValue),
          kv("target", target));
    }

    // Run a query for a custom search path
    FieldPath fieldPath = FieldPath.of(fieldPathElements);
    List<T> r = runSearch(target, schema, fieldPath, searchValue);
    if (log.isDebugEnabled()) {
      log.debug("Firestore search returning results", kv("resultSize", r.size()));
    }
    return r;
  }

  private <T> List<T> runSearch(
      Class<T> target, final String schema, FieldPath fieldPath, String searchValue)
      throws CTPException {
    // Run a query
    ApiFuture<QuerySnapshot> query =
        provider.get().collection(schema).whereEqualTo(fieldPath, searchValue).get();

    // Wait for query to complete and get results
    QuerySnapshot querySnapshot;
    try {
      querySnapshot = query.get();
    } catch (Exception e) {
      log.error("Failed to search schema", kv("schema", schema), kv("fieldPath", fieldPath), e);
      String failureMessage =
          "Failed to search schema '" + schema + "' by field '" + "'" + fieldPath;
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
    List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();

    // Convert the results to Java objects
    List<T> results;
    try {
      results = documents.stream().map(d -> d.toObject(target)).collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Failed to convert Firestore result to Java object", kv("target", target), e);
      String failureMessage =
          "Failed to convert Firestore result to Java object. Target class '" + target + "'";
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }

    return results;
  }

  /**
   * Delete an object from Firestore. No error is thrown if the object doesn't exist.
   *
   * @param schema - is the name of the collection which holds the object.
   * @param key - identifies the object within the collection.
   */
  @Override
  public void deleteObject(final String schema, final String key) throws CTPException {
    log.info("Deleting object from Firestore", kv("schema", schema), kv("key", key));

    // Tell firestore to delete object
    DocumentReference docRef = provider.get().collection(schema).document(key);
    ApiFuture<WriteResult> result = docRef.delete();

    // Wait for delete to complete
    try {
      result.get();
      log.info("Firestore delete completed", kv("schema", schema), kv("key", key));
    } catch (Exception e) {
      log.error("Failed to delete object from Firestore", kv("schema", schema), kv("key", key), e);
      String failureMessage =
          "Failed to delete object from Firestore. Schema: " + schema + " with key " + key;
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
  }

  /**
   * Returns the names of top level Firestore collections.
   *
   * @return a Set with the names of the current Firestore collections.
   */
  @Override
  public Set<String> getCollectionNames() {
    Set<String> collectionNames = new HashSet<>();
    provider.get().listCollections().forEach(c -> collectionNames.add(c.getId()));
    return collectionNames;
  }
}
