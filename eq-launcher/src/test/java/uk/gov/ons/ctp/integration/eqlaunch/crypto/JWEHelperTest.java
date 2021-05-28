package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.Test;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JWEHelper.DecryptJwe;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JWEHelper.EncryptJwe;

public class JWEHelperTest {

  private static final String SPURIOUS_RSA_PUBLIC_VALUE =
      "-----BEGIN PUBLIC KEY-----\n"
          + "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAn2YIoRD0Bcwz6DdF3IaA\n"
          + "jCcRYv7oAFJng/af1gTpvd6iKRnkc8sHNKs0QoFrIGK7YPVqvkYCKSss46JNqSGh\n"
          + "l+eKJLiM2F1Z5zdF7wnhRMvtmYpWuWpUT+/0sdh1m/dd9XjVRyCGppcimi1NqxKB\n"
          + "EfOW7a+y11LrlLxuQsEaa5aUzB1qhq17MYzc7/cERPWpFO0hn8VuymbwJ7Tp5IvQ\n"
          + "lb0MF7WggckoEYQCDM7yh3uFFO63F37TUHvDNQ2HepcgbAqCNmsReVEB4i56GxO0\n"
          + "oO2wJpv0ch+9g0hpViB43C8IYR+DuBiduKqs6XD5Jy9QNBaC5s6pmS4mtqZEc3Kt\n"
          + "Z8N30DWUspTod4Pwhuz3i2aI+PBz5S9nmAF/yjmmubElvNCeISY/LAYk+DV9oG1M\n"
          + "CsXrzJbHyy+vWdARmFd2pfqolZkCfaLmad0gN8/1Sw7jW8xWwuV1TqJOvcphthfp\n"
          + "t0z8MzxR5aok+JZ6jxe0LVn6XDo1CNw7dehKiy4UyRDI6WM3t+FdvjnJMJu9JZJg\n"
          + "CKnRHUIUyXb7O7M6pVDnMk4ptdgm8rxpxkGl24//vKiEpuSL+VqtpPkRuv0Lk1/R\n"
          + "4AUYYeuu20rCwnXqqyHDUL4u66GokHmmlEDHEB0Z/hEX7ZvFt1PhODyXOlho1H0a\n"
          + "mZrnkh13fkWKTPVDjY/n+3ECAwEAAQ==\n"
          + "-----END PUBLIC KEY-----";

  private DecryptJwe jweDecryptor = new DecryptJwe();

  @Test
  public void testEncryptBadKey() throws Exception {

    RSAKey rsaJWK = new RSAKeyGenerator(4096).keyID("123").generate();
    JWSHeader jwsHeader =
        new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaJWK.getKeyID())
            .type(JOSEObjectType.JWT)
            .build();
    JWSObject jwsObject = new JWSObject(jwsHeader, new Payload(""));
    jwsObject.sign(new RSASSASigner(rsaJWK));

    Key key = new Key();
    key.setValue("");

    CTPException e = assertThrows(CTPException.class, () -> new EncryptJwe(key));

    assertEquals(CTPException.Fault.SYSTEM_ERROR, e.getFault());
  }

  @Test
  public void testGetKidNotInHeader() throws Exception {

    JWEHeader jweHeader =
        new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM).build();
    JWEObject jweObject = new JWEObject(jweHeader, new Payload(""));

    RSAKey rsaJWK = new RSAKeyGenerator(4096).keyID("123").generate();
    jweObject.encrypt(new RSAEncrypter(rsaJWK));

    CTPException e =
        assertThrows(CTPException.class, () -> jweDecryptor.getKid(jweObject.serialize()));

    assertEquals(CTPException.Fault.SYSTEM_ERROR, e.getFault());
  }

  @Test
  public void testGetKidEmptyString() throws Exception {

    JWEHeader jweHeader =
        new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
            .keyID("")
            .build();
    JWEObject jweObject = new JWEObject(jweHeader, new Payload(""));

    RSAKey rsaJWK = new RSAKeyGenerator(4096).keyID("123").generate();
    jweObject.encrypt(new RSAEncrypter(rsaJWK));

    CTPException e =
        assertThrows(CTPException.class, () -> jweDecryptor.getKid(jweObject.serialize()));

    assertEquals(CTPException.Fault.SYSTEM_ERROR, e.getFault());
  }

  @Test
  public void testDecryptUnparseable() throws Exception {
    Key key = new Key();
    CTPException e = assertThrows(CTPException.class, () -> jweDecryptor.decrypt("", key));
    assertEquals(CTPException.Fault.SYSTEM_ERROR, e.getFault());
  }

  @Test
  public void testDecryptWrongKey() throws Exception {
    RSAKey rsaJWKSign = new RSAKeyGenerator(4096).keyID("123").generate();
    JWSHeader jwsHeader =
        new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaJWKSign.getKeyID())
            .type(JOSEObjectType.JWT)
            .build();
    JWSObject jwsObject = new JWSObject(jwsHeader, new Payload(""));
    jwsObject.sign(new RSASSASigner(rsaJWKSign));

    RSAKey rsaJWKEncrypt = new RSAKeyGenerator(4096).keyID("456").generate();
    JWEHeader jweHeader =
        new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
            .keyID("")
            .build();
    JWEObject jweObject = new JWEObject(jweHeader, new Payload(""));
    jweObject.encrypt(new RSAEncrypter(rsaJWKEncrypt));

    Key key = new Key();
    key.setValue(SPURIOUS_RSA_PUBLIC_VALUE);
    key.setKid("123");
    CTPException e =
        assertThrows(CTPException.class, () -> jweDecryptor.decrypt(jweObject.serialize(), key));
    assertEquals(CTPException.Fault.SYSTEM_ERROR, e.getFault());
  }
}
