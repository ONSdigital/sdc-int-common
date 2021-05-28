package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import lombok.Data;
import uk.gov.ons.ctp.common.error.CTPException;

@Data
/** Cryptographic Key model object */
public class Key {

  private static final Logger log = LoggerFactory.getLogger(Key.class);

  private String kid;
  private String purpose;
  private String type;
  private String value;

  /**
   * Return a JWK from Pem-encoded string
   *
   * @return JWK
   * @throws CTPException on error
   */
  public JWK getJWK() throws CTPException {
    try {
      return JWK.parseFromPEMEncodedObjects(value);
    } catch (JOSEException ex) {
      log.with("kid", kid).error("Could not parse key value");
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Could not parse key value");
    }
  }
}
