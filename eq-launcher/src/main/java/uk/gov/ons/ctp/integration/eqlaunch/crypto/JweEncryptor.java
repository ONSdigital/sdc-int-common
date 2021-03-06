package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import com.nimbusds.jose.JWSObject;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JWEHelper.EncryptJwe;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JWSHelper.EncodeJws;

/**
 * Implementation to sign a set of claims as a JSON Web Signature (JWS) and encrypt the JWS as the
 * payload of a JSON Web Encryption (JWE)
 */
@Slf4j
public class JweEncryptor {
  private static String KEYTYPE_PRIVATE = "private";
  private static String KEYTYPE_PUBLIC = "public";

  private EncodeJws jwsEncoder;
  private EncryptJwe jweEncryptor;
  private Key privateKey;
  private Key publicKey;

  /**
   * Constructor.
   *
   * @param keyStore key store
   * @param keyPurpose purpose
   * @throws CTPException on error
   */
  public JweEncryptor(KeyStore keyStore, String keyPurpose) throws CTPException {
    Optional<Key> privateKeyOpt = keyStore.getKeyForPurposeAndType(keyPurpose, KEYTYPE_PRIVATE);
    if (privateKeyOpt.isPresent()) {
      this.privateKey = privateKeyOpt.get();
    } else {
      log.error(
          "Failed to retrieve key to sign claims",
          kv("keyPurpose", keyPurpose),
          kv("keyType", KEYTYPE_PRIVATE));
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Failed to retrieve private key to sign claims");
    }

    Optional<Key> publicKeyOpt = keyStore.getKeyForPurposeAndType(keyPurpose, KEYTYPE_PUBLIC);
    if (publicKeyOpt.isPresent()) {
      this.publicKey = publicKeyOpt.get();
    } else {
      log.error(
          "Failed to retrieve key to encode payload",
          kv("keyPurpose", keyPurpose),
          kv("keyType", KEYTYPE_PUBLIC));
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Failed to retrieve public key to encode payload");
    }

    this.jwsEncoder = new EncodeJws(this.privateKey);
    this.jweEncryptor = new EncryptJwe(this.publicKey);
  }

  /**
   * Implementation to produce signed JWS for a set of claims and encrypt as the payload of a JWE.
   *
   * @param claims set of claims
   * @return encrypted value
   * @throws CTPException on error
   */
  public String encrypt(Map<String, Object> claims) throws CTPException {
    JWSObject jws = jwsEncoder.encode(claims);
    return jweEncryptor.encrypt(jws);
  }
}
