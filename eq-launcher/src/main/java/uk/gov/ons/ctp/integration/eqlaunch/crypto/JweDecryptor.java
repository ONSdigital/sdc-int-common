package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.nimbusds.jose.JWSObject;
import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JWEHelper.DecryptJwe;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JWSHelper.DecodeJws;

/** Decrypt a launch token. */
public class JweDecryptor {

  private static final Logger log = LoggerFactory.getLogger(JweDecryptor.class);

  private DecodeJws jwsHelper = new DecodeJws();
  private DecryptJwe jweHelper = new DecryptJwe();
  private KeyStore keyStore;

  public JweDecryptor(KeyStore keyStore) {
    this.keyStore = keyStore;
  }

  /**
   * Implementation to extract the JWS payload of a JWE, verify the JWS and return it's payload.
   *
   * @param jwe encrypted value
   * @return decrypted value
   * @throws CTPException on error.
   */
  public String decrypt(String jwe) throws CTPException {

    Optional<Key> publicKey = keyStore.getKeyById(jweHelper.getKid(jwe));
    JWSObject jws;
    if (publicKey.isPresent()) {
      jws = jweHelper.decrypt(jwe, publicKey.get());
    } else {
      log.with("kid", jweHelper.getKid(jwe)).error("Failed to retrieve public key to decrypt JWE");
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Failed to retrieve public key to decrypt JWE");
    }

    Optional<Key> privateKey = keyStore.getKeyById(jwsHelper.getKid(jws));
    if (privateKey.isPresent()) {
      return jwsHelper.decode(jws, privateKey.get());
    } else {
      log.with("kid", jwsHelper.getKid(jws)).error("Failed to retrieve private key to verify JWS");
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Failed to retrieve private key to verify JWS");
    }
  }
}
