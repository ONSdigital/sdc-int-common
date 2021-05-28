package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import java.util.Map;
import lombok.Data;

/** Holder for Cryptographic keys */
@Data
public class Keys {

  private Map<String, Key> keys;
}
