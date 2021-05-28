package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import uk.gov.ons.ctp.common.error.CTPException;

/** Store for cryptographic keys */
@Data
public class KeyStore {

  private static final Logger log = LoggerFactory.getLogger(KeyStore.class);
  private Keys keys;

  /**
   * Creates a store of cryptographic keys.
   *
   * @param cryptoKeys JSON String of cryptographic keys.
   * @throws CTPException if failure to read keys.
   */
  @SuppressWarnings("deprecation")
  public KeyStore(String cryptoKeys) throws CTPException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    try {
      keys = mapper.readValue(cryptoKeys, Keys.class);
    } catch (Exception e) {
      log.error("Failed to read cryptographic keys");
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Failed to read cryptographic keys");
    }
    keys.getKeys()
        .forEach(
            (key, value) -> {
              value.setKid(key);
            });
  }

  /**
   * Gets a list of keys that match the purpose and type and returns the first key in that list.
   *
   * @param purpose Purpose of key e.g. authentication
   * @param type e.g. private or public
   * @return Optional containing Key if match found
   */
  public Optional<Key> getKeyForPurposeAndType(String purpose, String type) {

    List<Key> matching =
        keys.getKeys().values().stream()
            .filter(x -> (x.getPurpose().equals(purpose) && x.getType().equals(type)))
            .collect(Collectors.toList());

    if (!matching.isEmpty()) {
      return Optional.of(matching.get(0));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Get key by Id
   *
   * @param kid key Id
   * @return optional key
   */
  public Optional<Key> getKeyById(String kid) {
    if (keys.getKeys().containsKey(kid)) {
      return Optional.of(keys.getKeys().get(kid));
    } else {
      return Optional.empty();
    }
  }
}
