package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import com.nimbusds.jose.JWSObject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JWEHelper.DecryptJwe;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JWSHelper.DecodeJws;

/** Decrypt a launch token. */
@Slf4j
public class JweDecryptor {
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
      log.error("Failed to retrieve public key to decrypt JWE", kv("kid", jweHelper.getKid(jwe)));
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Failed to retrieve public key to decrypt JWE");
    }

    Optional<Key> privateKey = keyStore.getKeyById(jwsHelper.getKid(jws));
    if (privateKey.isPresent()) {
      return jwsHelper.decode(jws, privateKey.get());
    } else {
      log.error("Failed to retrieve private key to verify JWS", kv("kid", jwsHelper.getKid(jws)));
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Failed to retrieve private key to verify JWS");
    }
  }
}
