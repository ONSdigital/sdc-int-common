package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.RSAKey;
import java.text.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.ons.ctp.common.error.CTPException;

/** Helper class for encrypting, decrypting a JWS as the payload of a JWE. */
@Slf4j
public abstract class JWEHelper {

  public static class EncryptJwe extends JWEHelper {
    private Key key;
    private JWEHeader jweHeader;
    private RSAEncrypter encryptor;

    /**
     * Constructor
     *
     * @param key cryptographic key to use for encryption
     * @throws CTPException on error
     */
    public EncryptJwe(Key key) throws CTPException {
      this.key = key;
      this.jweHeader = buildHeader(key);
      RSAKey jwk = (RSAKey) key.getJWK();

      try {
        encryptor = new RSAEncrypter(jwk);
      } catch (JOSEException e) {
        log.error("Cannot initialise encryption for JWE", kv("kid", key.getKid()));
        throw new CTPException(
            CTPException.Fault.SYSTEM_ERROR, "Cannot initialise encryption for JWE");
      }
    }

    /**
     * Encrypt JWS as payload of JWE
     *
     * @param jws payload
     * @return encrypted result
     * @throws CTPException on error
     */
    public String encrypt(JWSObject jws) throws CTPException {
      log.debug("Encrypting with public key", kv("kid", key.getKid()));
      Payload payload = new Payload(jws);
      JWEObject jweObject = new JWEObject(jweHeader, payload);

      try {
        jweObject.encrypt(this.encryptor);
        return jweObject.serialize();
      } catch (JOSEException e) {
        log.error("Failed to encrypt JWE", kv("kid", key.getKid()), e);
        throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Failed to encrypt JWE");
      }
    }

    @SuppressWarnings("deprecation")
    private JWEHeader buildHeader(Key key) {

      // We HAVE to use the deprecated Algo to remain compatible with EQ
      JWEHeader jweHeader =
          new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP, EncryptionMethod.A256GCM)
              .keyID(key.getKid())
              .build();
      return jweHeader;
    }
  }

  public static class DecryptJwe extends JWEHelper {
    /**
     * Decrypt JWE returning JWS payload.
     *
     * @param jwe JWE encrypted String
     * @param key Cryptographic key to decrypt JWE
     * @return JWSObject representing payload
     * @throws CTPException on error
     */
    public JWSObject decrypt(String jwe, Key key) throws CTPException {

      JWEObject jweObject;
      try {
        jweObject = JWEObject.parse(jwe);
      } catch (ParseException e) {
        log.error("Failed to parse JWE string", kv("jwe", jwe), kv("kid", key.getKid()), e);
        throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Failed to parse JWE string");
      }

      try {
        jweObject.decrypt(new RSADecrypter((RSAKey) key.getJWK()));
      } catch (JOSEException e) {
        log.error(
            "Failed to decrypt JWE with provided key", kv("jwe", jwe), kv("kid", key.getKid()), e);
        throw new CTPException(
            CTPException.Fault.SYSTEM_ERROR, "Failed to decrypt JWE with provided key");
      }

      Payload payload = jweObject.getPayload();
      if (payload == null) {
        log.error("Extracted JWE Payload null", kv("jwe", jwe), kv("kid", key.getKid()));
        throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Extracted JWE Payload null");
      }

      return payload.toJWSObject();
    }
  }

  /**
   * Return key hint (Id) from JWE header
   *
   * @param jwe JWE encrypted String
   * @return String representing Key hint from header
   * @throws CTPException when fails to retrieve key Id from header.
   */
  public String getKid(String jwe) throws CTPException {
    try {
      JWEObject jweObject = JWEObject.parse(jwe);
      String keyId = jweObject.getHeader().getKeyID();
      if (StringUtils.isEmpty(keyId)) {
        log.error("Failed to extract key Id from JWE header", kv("jwe", jwe));
        throw new CTPException(
            CTPException.Fault.SYSTEM_ERROR, "Failed to extract key Id from JWE header");
      }
      return keyId;
    } catch (ParseException e) {
      log.error("Failed to parse JWE string", kv("jwe", jwe), e);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Failed to parse JWE string");
    }
  }
}
