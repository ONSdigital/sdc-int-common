package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Map;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import uk.gov.ons.ctp.common.error.CTPException;

/**
 * Helper class for signing provided claims, encoding as a JWS token or verifying and decoding
 * provided JWS.
 */
public abstract class JWSHelper {

  private static final Logger log = LoggerFactory.getLogger(JWSHelper.class);

  /**
   * Return key hint (Id) from JWS header
   *
   * @param jwsObject JWS object
   * @return String representing Key hint from header
   * @throws CTPException on error
   */
  public String getKid(JWSObject jwsObject) throws CTPException {
    String keyId = jwsObject.getHeader().getKeyID();
    if (StringUtils.isEmpty(keyId)) {
      log.error("Failed to extract Key Id");
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Failed to extract Key Id");
    }
    return keyId;
  }

  /** Class for JWS encoding for single key, caching key processing. */
  public static class EncodeJws extends JWSHelper {
    private Key key;
    private JWSHeader jwsHeader;
    private RSASSASigner signer;

    /**
     * Constructor.
     *
     * @param key key with which to sign claims.
     * @throws CTPException on error.
     */
    public EncodeJws(Key key) throws CTPException {
      this.key = key;
      this.jwsHeader = buildHeader(key);
      RSAKey jwk = (RSAKey) key.getJWK();

      try {
        this.signer = new RSASSASigner(jwk);
      } catch (JOSEException e) {
        log.with("kid", key.getKid()).error("Failed to create private JWSSigner to sign claims");
        throw new CTPException(
            CTPException.Fault.SYSTEM_ERROR, "Failed to create private JWSSigner to sign claims");
      }
    }

    /**
     * Return JWSObject with provided claims using key provided
     *
     * @param claims to be signed
     * @return JWSObject representing JWS token
     * @throws CTPException on error
     */
    public JWSObject encode(Map<String, Object> claims) throws CTPException {
      log.with(key.getKid()).debug("Encoding with public key");
      Payload jwsClaims = buildClaims(claims);
      JWSObject jwsObject = new JWSObject(jwsHeader, jwsClaims);

      try {
        jwsObject.sign(this.signer);
        return jwsObject;
      } catch (JOSEException e) {
        log.with("kid", key.getKid()).error("Failed to sign claims");
        throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Failed to sign claims");
      }
    }

    private JWSHeader buildHeader(Key key) {
      JWSHeader jwsHeader =
          new JWSHeader.Builder(JWSAlgorithm.RS256)
              .type(JOSEObjectType.JWT)
              .keyID(key.getKid())
              .build();
      return jwsHeader;
    }

    private Payload buildClaims(Map<String, Object> claims) {
      JSONObject jsonObject = new JSONObject(claims);
      Payload jwsClaims = new Payload(jsonObject);
      return jwsClaims;
    }
  }

  public static class DecodeJws extends JWSHelper {
    /**
     * Check the signature of this JWS object against the provided Key.
     *
     * @param jwsObject JWS object
     * @param key key
     * @return JWS claims
     * @throws CTPException on error
     */
    public String decode(JWSObject jwsObject, Key key) throws CTPException {
      try {
        if (jwsObject.verify(new RSASSAVerifier((RSAKey) key.getJWK()))) {
          Payload payload = jwsObject.getPayload();
          if (payload == null) {
            log.error("Extracted JWS Payload null");
            throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Extracted JWS Payload null");
          }
          return payload.toString();
        } else {
          log.with("kid", key.getKid()).error("Failed to verify JWS signature");
          throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Failed to verify JWS signature");
        }
      } catch (JOSEException e) {
        log.with("kid", key.getKid()).error("Failed to verify JWS signature");
        throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Failed to verify JWS signature");
      }
    }
  }
}
