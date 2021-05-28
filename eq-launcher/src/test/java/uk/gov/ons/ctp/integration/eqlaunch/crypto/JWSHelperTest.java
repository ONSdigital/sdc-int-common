package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.Test;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JWSHelper.DecodeJws;

public class JWSHelperTest {

  private static final String SIGNING_PUBLIC_VALUE =
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

  private static final String SIGNING_PRIVATE_VALUE =
      "-----BEGIN RSA PRIVATE KEY-----\n"
          + "MIIJKQIBAAKCAgEAzz3vj5w4oMB9tUd+NYtcDxDgvd8NsnBIZL6qmBi9ph6HzNXO\n"
          + "Re+QErby3oLDnqPImZ38XVwLY3oZ9YGhVewD+p1LFT2nNMhR9mJmGz2DxOZFeAGn\n"
          + "v2aDMOV22eF3oX6z5/VseIE1+C/CDgRGXXTrwkuFkHJIe9/9F90a8em7dzbOU+v9\n"
          + "PCnY2Xrp2h7cYEinVP8GvvJhalZpAKkqWPBisTNhw4Xoq5ZzEJ8XDDyZntysDE6m\n"
          + "2a6fyAJ6T8tR6MJa+2FIOH32tA3CDUBqaQaQopcCYl1V5t8ecQjYCk3/ikDqgVvV\n"
          + "XntcUDkNrDiPLJKJ5Qp3UhBZSs0hepM0+PMegN1cIFMI5MvvzX+/zP5HEb2muCsv\n"
          + "t3xxN1vJRk28/V+FznBYAuYpurd0QggT8xyrYEXBg03LArpzZYPySYuN+R7p3tbA\n"
          + "+q5AW1BRGFwa8p9Ch7y9B5BeFwxTolz7tmDa3sIRnM0quyk2/Wz7OAqseoWak1Ma\n"
          + "grXuemKZEXqmzN0k0yFNqjputB8i1lRjZ3UOpy51VP+UeRIOMY8lUYMasYibujTZ\n"
          + "HZSeuDyHAlDpPMg6XKnp/f/+y6qBPDOSgE0dmbxy1j2BFsEeM4WRcEx+ghkqL1XX\n"
          + "LSZFWfUSwW/WZpnFstL1GCGMjoMzgKGF+N2QBn+otbGCZbu41Os7YUYIdn0CAwEA\n"
          + "AQKCAgEAhlspDHnDXLRuyy/mauBGdp4ClhYd0yloRag3ARRJH4F7mRij+kMtrHRf\n"
          + "UFKGcDrOuojqK7yYxY1LdxbrecDhc4C2RLcLx/R27r0sZUykOOrw7rRkBHp5YyHg\n"
          + "w7Cg1lpGWIOMJzPdwWF09ZFf7Qb4Maa0mMj+pRC6DNaTuXJGzysA6Pd93Ztjsts0\n"
          + "8OxBA0sW8MvFm9WXwlDzEjKZ+b8evLMLFq+iAFwxjP0W/B/tmEiIYhI7qbTEce7p\n"
          + "TQILwFMAmSigob9IScMBo4W3dw+ChWZRbWQFZQARxEZviX63xPIBFoxq8C4Z0wiq\n"
          + "DJU+fS8jpxH5+YIP4abLpaP6G8vCYa4RltmGHa9cXzQeu7L7YJocVWwlGO5lePyM\n"
          + "KKAMXJqA/Gjy6X2DOOzec7viSZVgNqz+bne5/+FYupq7JZqo7wROuVwsS5vbUx8K\n"
          + "B8vjHmYkjWsSHk6ZA2RgAKq20rn0J2KdY5Bb93kLqoix7aeXyLjcoXGHBsGiP7B2\n"
          + "1oa4Vnxm6W9ABHUxNizEW/vGH9weydJ0Gwf03FWkNm1+tvLj3IshnpB/bTHU/ydX\n"
          + "NS6nuj1GjpFiXI+EZczVlocFzP7GAXoHRpJBgP0Ukf9J8IWFl4qOfQjhxSR8aEzV\n"
          + "tT1qwkjUJXO60BdJPTLAl8HFZVH5zkmuc2HD/IF/sFKFjMB9PwECggEBAPKZpyXc\n"
          + "CNEGJuKOd6tkwPDDct7lA5pOtd0a0NFm/oVUraMPkt9GfX04bClsRyC6yZhbJyMw\n"
          + "A/N25aOXG/Baz4P8Apek9d5m1GNdyiKZnAfhYbeJF3lNoVO8kQcKy/U60z8j38wW\n"
          + "u9id/AUBm51OAf54ae6PEHDNcWRNXQt/LoiUmP2c6akVfEkXiPFXqe2bwWureG9O\n"
          + "bsIurVWxolJMVzp0Go6YIhLXQwd9Sett6WliZ99pkwFF+4FEkUh3ru4e0tOkvCKO\n"
          + "yuTA7lDpy+zW98emwLJUy8n3Sii6d0ErRejrAx1RQW6zbUat/LIQXqBmTICYRQvH\n"
          + "M6zWJOFNm5lrEikCggEBANqwUd+KU9M67YQXi+C/ZVl2e2Cqf1KAvoGXPh/nJsUw\n"
          + "v8KJvIVK7kW1o0q8RO49WYapubBpNICPTGyWwOyfH+KSGRuloum9k331bvdaz6qw\n"
          + "zgGK1mTqFEXNElTiOsSpJaTuLt7bjdFHFsgQCOugBf8TWY6371mPgXtKvzKrFO0v\n"
          + "pMXhgpjVdlFgnXMJQpnSZdNh58Um0+zc/EjxIIDmjbHMj2SExKVKapOer1jrcBGS\n"
          + "O7JAIQrEQM0mqtenQPqe80FAPYY+SoUJjJ4YxeCOu+rOoSh1ClGL7I4g2FvhvgAC\n"
          + "2U737YidhnwERWdCFIfhkRmkoVR3e3wV6k0u5bEulDUCggEAAjW+HQ0tE2Jf9k5f\n"
          + "7rLDQy40nK0vZDd1VqdI4a9zgBluX37j0p7cw8hAy/vNhhHNhlLGP37PemdJ3jyh\n"
          + "J4ZcP5KLH4CEMNt08dbH4ZrOng/CiR55lURMxOuB0rOZegloJToZbs2CNo3x3sXN\n"
          + "+hfc0smcBW1ONAjbEJPX1iP5c4sO/bhxNHYapLvPJouq45w4ndd5CGKJhcFRGOe+\n"
          + "V8uUO9cU6tmd7dgCJ05P3xIoPyqDUbiveyJ9EQdj32ofsNGdEAp/ID12wbC0Ow52\n"
          + "KhknNq1hMf6twJA9H7PbJD/VqjKB28GCvBRsWWl6VNDrW8Cyz7UTY/ETmm509Yx0\n"
          + "b2hXSQKCAQEAtFrP2vz47u7dbaABs0QF8Mc/L1TNlpwpATVbffIjzmLK80Sm3oMS\n"
          + "iRko53znmFeuWtnlE3FgZFpKHBAkYcFGCZSV8nAjMIQxfKMKdiNFuy7/ZtQ6xpUq\n"
          + "TPq4kJrW/tPFAQWSUCdgCWWIi0x2HuUlrN0ncgWN9x3cGnNlxgLESmyNhsjZ7PO3\n"
          + "FZwJnhLYA4Y6hh9rhvPjuafyxLFgLg52c1kSNUMt7me2B7LKSBo7nbItW296EKgU\n"
          + "DV1DboE4nLi0Q28YjnsW+CsM9mHV58GvhxIMZRJJhUFRwVGjPfupt9aho3fjRVUs\n"
          + "0WCwYF1mEz5bbXuRtdioVYi5aBgyRHL2tQKCAQBd8UvLfBdQV68oKY7wtSQqm22j\n"
          + "QDooKTbP5UEyEqNZnOTJRAZck2cx0Jhpbav4mr8AYG6Z28NRT/ugcBrwIrYfALCw\n"
          + "5wQWRr5Ss+mvmL37Htn3H1zK8ynk07KWFlFPkP5a4MZ2WBiiseRkeATpY/CULtU8\n"
          + "fF4ZHF2XzRjjB7D7UNFS7vfyQuM+badal4/R/RaizuozmJMKSQyAt3RFnIek6oOk\n"
          + "Rc0Tn5hPi9ccZLbHgXp21A2lgn8pzfNzIO/Hg56I8Td+Q2E0+MJJL7Mhcccv4CdL\n"
          + "AuSIJ3MHTJ3yPMNg1yeSkau2vXPQgcrApjCuLK5Uo/mhM1bJjKOxB7FisXEC\n"
          + "-----END RSA PRIVATE KEY-----";

  private DecodeJws jwsDecoder = new DecodeJws();

  @Test
  public void testEncodeBadKey() throws Exception {
    Key key = new Key();
    // Need a private key to sign JWS
    key.setValue(SIGNING_PUBLIC_VALUE);
    CTPException e = assertThrows(CTPException.class, () -> new JWSHelper.EncodeJws(key));
    assertEquals(CTPException.Fault.SYSTEM_ERROR, e.getFault());
  }

  @Test
  public void testGetKidNotInHeader() throws Exception {
    JWSHeader jwsHeader =
        new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();
    JWSObject jwsObject = new JWSObject(jwsHeader, new Payload(""));

    CTPException e = assertThrows(CTPException.class, () -> jwsDecoder.getKid(jwsObject));
    assertEquals(CTPException.Fault.SYSTEM_ERROR, e.getFault());
  }

  @Test
  public void testGetKidEmptyString() throws Exception {
    JWSHeader jwsHeader =
        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("").type(JOSEObjectType.JWT).build();

    JWSObject jwsObject = new JWSObject(jwsHeader, new Payload(""));

    CTPException e = assertThrows(CTPException.class, () -> jwsDecoder.getKid(jwsObject));
    assertEquals(CTPException.Fault.SYSTEM_ERROR, e.getFault());
  }

  @Test
  public void testDecodeWrongKey() throws Exception {
    RSAKey rsaJWK = new RSAKeyGenerator(4096).keyID("123").generate();

    JWSHeader jwsHeader =
        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("123").type(JOSEObjectType.JWT).build();

    JWSObject jwsObject = new JWSObject(jwsHeader, new Payload(""));

    jwsObject.sign(new RSASSASigner(rsaJWK));

    Key key = new Key();
    key.setValue(SIGNING_PUBLIC_VALUE);
    CTPException e = assertThrows(CTPException.class, () -> jwsDecoder.decode(jwsObject, key));
    assertEquals(CTPException.Fault.SYSTEM_ERROR, e.getFault());
  }

  @Test
  public void testDecodeBadKey() throws Exception {
    RSAKey rsaJWK = new RSAKeyGenerator(4096).keyID("123").generate();

    JWSHeader jwsHeader =
        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("1234567").type(JOSEObjectType.JWT).build();

    JWSObject jwsObject = new JWSObject(jwsHeader, new Payload(""));

    jwsObject.sign(new RSASSASigner(rsaJWK));

    Key key = new Key();
    // Need a public key to verify JWS
    key.setValue(SIGNING_PRIVATE_VALUE);
    CTPException e = assertThrows(CTPException.class, () -> jwsDecoder.decode(jwsObject, key));
    assertEquals(CTPException.Fault.SYSTEM_ERROR, e.getFault());
  }
}
