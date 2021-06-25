package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;

@Slf4j
@Data
/** Cryptographic Key model object */
public class Key {
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
      log.error("Could not parse key value", kv("kid", kid));
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Could not parse key value");
    }
  }
}
