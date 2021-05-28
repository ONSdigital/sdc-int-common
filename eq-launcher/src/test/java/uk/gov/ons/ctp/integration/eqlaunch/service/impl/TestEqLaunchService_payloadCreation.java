package uk.gov.ons.ctp.integration.eqlaunch.service.impl;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.JweDecryptor;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchCoreData;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;

public class TestEqLaunchService_payloadCreation {

  private static final String SIGNING_PUBLIC_SHA1 = "f008897e548d5c4fb9271e5a85f6fc4759301f26";

  private static final String ENCRYPTION_PRIVATE_SHA1 = "bc26b780aa46f053291ba122062e6075656c2345";

  private static final String SIGNING_PUBLIC_VALUE =
      "-----BEGIN PUBLIC KEY-----\n"
          + "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAzz3vj5w4oMB9tUd+NYtc\n"
          + "DxDgvd8NsnBIZL6qmBi9ph6HzNXORe+QErby3oLDnqPImZ38XVwLY3oZ9YGhVewD\n"
          + "+p1LFT2nNMhR9mJmGz2DxOZFeAGnv2aDMOV22eF3oX6z5/VseIE1+C/CDgRGXXTr\n"
          + "wkuFkHJIe9/9F90a8em7dzbOU+v9PCnY2Xrp2h7cYEinVP8GvvJhalZpAKkqWPBi\n"
          + "sTNhw4Xoq5ZzEJ8XDDyZntysDE6m2a6fyAJ6T8tR6MJa+2FIOH32tA3CDUBqaQaQ\n"
          + "opcCYl1V5t8ecQjYCk3/ikDqgVvVXntcUDkNrDiPLJKJ5Qp3UhBZSs0hepM0+PMe\n"
          + "gN1cIFMI5MvvzX+/zP5HEb2muCsvt3xxN1vJRk28/V+FznBYAuYpurd0QggT8xyr\n"
          + "YEXBg03LArpzZYPySYuN+R7p3tbA+q5AW1BRGFwa8p9Ch7y9B5BeFwxTolz7tmDa\n"
          + "3sIRnM0quyk2/Wz7OAqseoWak1MagrXuemKZEXqmzN0k0yFNqjputB8i1lRjZ3UO\n"
          + "py51VP+UeRIOMY8lUYMasYibujTZHZSeuDyHAlDpPMg6XKnp/f/+y6qBPDOSgE0d\n"
          + "mbxy1j2BFsEeM4WRcEx+ghkqL1XXLSZFWfUSwW/WZpnFstL1GCGMjoMzgKGF+N2Q\n"
          + "Bn+otbGCZbu41Os7YUYIdn0CAwEAAQ==\n"
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

  private static final String ENCRYPTION_PUBLIC_VALUE =
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

  private static final String ENCRYPTION_PRIVATE_VALUE =
      "-----BEGIN RSA PRIVATE KEY-----\n"
          + "MIIJKgIBAAKCAgEAn2YIoRD0Bcwz6DdF3IaAjCcRYv7oAFJng/af1gTpvd6iKRnk\n"
          + "c8sHNKs0QoFrIGK7YPVqvkYCKSss46JNqSGhl+eKJLiM2F1Z5zdF7wnhRMvtmYpW\n"
          + "uWpUT+/0sdh1m/dd9XjVRyCGppcimi1NqxKBEfOW7a+y11LrlLxuQsEaa5aUzB1q\n"
          + "hq17MYzc7/cERPWpFO0hn8VuymbwJ7Tp5IvQlb0MF7WggckoEYQCDM7yh3uFFO63\n"
          + "F37TUHvDNQ2HepcgbAqCNmsReVEB4i56GxO0oO2wJpv0ch+9g0hpViB43C8IYR+D\n"
          + "uBiduKqs6XD5Jy9QNBaC5s6pmS4mtqZEc3KtZ8N30DWUspTod4Pwhuz3i2aI+PBz\n"
          + "5S9nmAF/yjmmubElvNCeISY/LAYk+DV9oG1MCsXrzJbHyy+vWdARmFd2pfqolZkC\n"
          + "faLmad0gN8/1Sw7jW8xWwuV1TqJOvcphthfpt0z8MzxR5aok+JZ6jxe0LVn6XDo1\n"
          + "CNw7dehKiy4UyRDI6WM3t+FdvjnJMJu9JZJgCKnRHUIUyXb7O7M6pVDnMk4ptdgm\n"
          + "8rxpxkGl24//vKiEpuSL+VqtpPkRuv0Lk1/R4AUYYeuu20rCwnXqqyHDUL4u66Go\n"
          + "kHmmlEDHEB0Z/hEX7ZvFt1PhODyXOlho1H0amZrnkh13fkWKTPVDjY/n+3ECAwEA\n"
          + "AQKCAgAX/vpJlQ3HWahuyvNfcXgkoTUC3DD862rd+OCzDWZKyRtMaLN1oxjgmu1x\n"
          + "HZ6M75AZ1phNMKjenbtSQXrDfWagQaEQSiAZ6mPAZRfIFoqtGq4YMTVBEHrE1fDW\n"
          + "XSnHYwPoElq5LHJY3eO9phhNKqn3k94ixhJ8S+VfLMbEkeFyVObtm3gP8knbecNA\n"
          + "9MTVxM3BcrlfHg/BtKFuuNOFLm8mO6F4aEVThH7dtCvC1dy4/KVaM12o0dTqBteq\n"
          + "QvEkSV/+I87adl/ZDZLlA+4p6VuF5oJgLZYtIA4dl2cUKitHqmTuihoAOuRzim4h\n"
          + "bnpzpbHDRahLKs1pXAND99/SKW9lNKmlkc3NkMXUHSVy13ovNnuh3AbkDcMyNfaK\n"
          + "hAZMnZZ0TFXXTf9WzK2023Gvpk4YHwCxEP/800UK7lfuUAvyJiGElzN3KLX6ZeTK\n"
          + "PRYu1fZjz2uLBHr9sc8MoT3RF1PEGIPbuNAHbdaclSJuxicaek2UiN+SEo2JeywH\n"
          + "oMLJpNPYQmnXFJUUn0QxF09Q0dNQIbYNImEn6tAkZubtCU0sq05pjCfUmjo7dkfF\n"
          + "pgXJes2Ny57RWEh8bsTLyaAI6NGe31t2POfRJRe14M7FrTif6PjH6CKqy78G7L0R\n"
          + "Iq6/J+U+qXUPOZ58rwWNyX4bxrPVUlbj2h7v59acfJXg63iG6QKCAQEA0/37ci7V\n"
          + "tZFcqLbgrNMIMhj9fLTuOU/gJwh7DDi0SlerOsFXyNqHGLoEtz2ZowmLACE7UUHj\n"
          + "KEcYiUbe2orVU0efR67CBmfZpv/jbGXx4f3PC8whA+6y+MkiZEtCtB54ghqnq5k2\n"
          + "mKaQGbhst4HHWZkYRlJscBACqibIWcyOfejycQr3mKk5oRXsXX37tvdN4M+ITxyK\n"
          + "1iP7pA18SJMjnFXIbjkGM4bXKsFCUyA7n3ga/QtrnfDIqIaoSekabtek85PWkv9J\n"
          + "IPBESa0IY4NeNMhIKFg/ockOTUz3vExB9Qlx8fNyUPHp1/5q2606mvx1kBmU0YxN\n"
          + "+ZpuqymCt9IRPwKCAQEAwH0K54iLltustYIzEhW0VRwO+UDbVgPKgXIowpue2/T+\n"
          + "QoPLmbsU0D1LlOoPMoY2KA9hBFCqjVpKN+1s8V4iss3sfaTwl4jp49dcfN+8f9h6\n"
          + "YtXRQscOvtTkTeSSFQveuhFwQDz1fmvRlQTfx14702/g0hk7eo4M8EHkOrZ++1qD\n"
          + "a445SwlIpfbgg1xgk7z0/DjhYT1p26MDDhqinhfOvz2dvVdoWwqJxrwEqqV/dpLI\n"
          + "zI1zWIg4WDEftu15Lbu0bSgGfBO6Toq/jC2w9nHXZ+kUr5TYb2eML0K54hQBX4/l\n"
          + "Td607qayWzKbg0Y7kXt//xIfhXNB+Qvvb4+L33gXTwKCAQEAsO877bpPXoLbrmks\n"
          + "rrJYFdAryekNJ8KWGJ4IbVPqjl4LqTdz+E0EEsw3YWVFlQFDBbG224Q9fwGP5gvD\n"
          + "tu3HTQHrXB3tDQGidLMAcxnHP9Lh+zu9lpWkxObDEKiMHN+NvzFWHm/7VH84qG74\n"
          + "NlVYOF0CGLd0kDecsb063VBQ4aAKsfTwYHdlhSBSLloXPEe+tVMhtr3g1wgjYHJs\n"
          + "9/nu7+3dIrnvhhQWBDZXECBIAzpwdCETCjjMkXQKg93N1ROhNjd/ESOHOrmulTuZ\n"
          + "U81yVGWRbDLzsSmpCTWjU4LmTgk7FDEXiZ3Pihprv7wevPrXeJURU7Bh6GFP2wYD\n"
          + "dz8aawKCAQEAh8ywEMEImhnJMxT0KuIdI550Ae170KdECycbEpDv9oZVi1+oGIEg\n"
          + "IfnchObMafy94G1ZKM4wRSFaEzLyFScwm60kF1ByXLY/LHsXyRGfzyJO1cO7qrz8\n"
          + "7i0MFNgZJ7gEHomHBAr9t38hkVyq8+DLnFPl7NPYXpIxFp+mALYXyAUHiq4ARav7\n"
          + "EPunUYWurQw2WmtWtl+5ezb6RM0NxTNOrSUvUBGGkhRRN2AWer3fuyu7dnSk1+Ti\n"
          + "u3WMxmWrhFRU6l2+lXLlkdvZX3As1PFj4u9RpL4CJDaNdBpDQfiTL3Q5dkKUq3n3\n"
          + "gtelV2wJQo46sx16F5BFDwsYlUBUkggLiQKCAQEAq2maL0a+jCcuKK9wJws7V2Q6\n"
          + "WMHhswm4//5XDKbY1d0qjJyusyDxD4Xqr72s8EsRv4hRKktgFEdkMizwq5snrMdO\n"
          + "6g/estUWxUjW1Wf3fgW75W2M8CSq3acbg5uirqiFTSR51/pTWhlpzf4GoZX2u4x2\n"
          + "1hKZ7esavfCth8G1xx1KSVouqn6E7WvVUN4XqYvYRHVXYXzgS7t5mrnnEp1fjuFm\n"
          + "JU61HNJIMLrQGgHf7+0gq3Q6gtVka7OsH9GBmrJi9S+lGO9xm+e36QRcZe6z70Gm\n"
          + "NE02d5/qDJxpFUoF8gc/c9bTrdab5gmNOurud/n2j0oxb3t8kmiyxyPaB+xlbA==\n"
          + "-----END RSA PRIVATE KEY-----";

  private static final String JWTKEYS_ENCRYPTION =
      "{\"keys\": {\""
          + SIGNING_PUBLIC_SHA1
          + "\": "
          + "{\"purpose\": \"authentication\", "
          + "\"type\": \"private\""
          + ", \"value\": \""
          + SIGNING_PRIVATE_VALUE
          + "\"}, \""
          + ENCRYPTION_PRIVATE_SHA1
          + "\": "
          + "{\"purpose\": \"authentication\", "
          + "\"type\": \"public\""
          + ", \"value\": \""
          + ENCRYPTION_PUBLIC_VALUE
          + "\"}}}";

  private static final String JWTKEYS_DECRYPTION =
      "{\"keys\": {\""
          + SIGNING_PUBLIC_SHA1
          + "\": "
          + "{\"purpose\": \"decryption\", "
          + "\"type\": \"public\""
          + ", \"value\": \""
          + SIGNING_PUBLIC_VALUE
          + "\"}, \""
          + ENCRYPTION_PRIVATE_SHA1
          + "\": "
          + "{\"purpose\": \"decryption\", "
          + "\"type\": \"private\""
          + ", \"value\": \""
          + ENCRYPTION_PRIVATE_VALUE
          + "\"}}}";
  private static final String SALT = "CENSUS";
  private static final String A_QUESTIONNAIRE_ID = "11100000009";
  private static final String ENCRYTPED_RESPONSE_ID = A_QUESTIONNAIRE_ID + "804def6a16184d28";

  /**
   * Calls both the public and package methods in the class under test and asserts that each returns
   * the expected map The public method returns the encrypted JWE representation of the map, the
   * package method returns the map raw. The test decodes the encrypted form and deserialises it, as
   * well as sanitising it of variable UUID and date time values. The test asserts both the
   * behaviour of the public method and its use of the package method.
   *
   * @throws Exception
   */
  @Test
  public void createFieldServicePayload() throws Exception {
    KeyStore keyStoreEncryption = new KeyStore(JWTKEYS_ENCRYPTION);
    EqLaunchServiceImpl eqLaunchService = new EqLaunchServiceImpl(keyStoreEncryption);

    KeyStore keyStoreDecryption = new KeyStore(JWTKEYS_DECRYPTION);
    JweDecryptor decryptor = new JweDecryptor(keyStoreDecryption);

    // create expectation
    UUID collectionExerciseId = UUID.randomUUID();
    String uprn = "10023122451";
    UUID caseId = UUID.randomUUID();

    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put("jti", "88888888-8888-8888-8888-888888888888");
    expectedMap.put("tx_id", "88888888-8888-8888-8888-888888888888");
    expectedMap.put("iat", "12345");
    expectedMap.put("exp", "12345");
    expectedMap.put("case_type", "H");
    expectedMap.put("collection_exercise_sid", collectionExerciseId.toString());
    expectedMap.put("region_code", "GB-ENG");
    expectedMap.put("ru_ref", uprn);
    expectedMap.put("case_id", caseId.toString());
    expectedMap.put("language_code", "en");
    expectedMap.put("display_address", "ONS, Segensworth\'s Road");
    expectedMap.put("response_id", ENCRYTPED_RESPONSE_ID);
    expectedMap.put("account_service_url", "http://localhost:9092/start");
    expectedMap.put("account_service_log_out_url", "http://localhost:9092/start/save-and-exit");
    expectedMap.put("channel", "field");
    expectedMap.put("user_id", "1234567890");
    expectedMap.put("questionnaire_id", A_QUESTIONNAIRE_ID);
    expectedMap.put("eq_id", "census");
    expectedMap.put("period_id", "2021");
    expectedMap.put("form_type", "H");
    expectedMap.put("survey", "CENSUS");

    // create params for code under test
    Language language = Language.ENGLISH;
    Source source = Source.FIELD_SERVICE;
    Channel channel = Channel.FIELD;
    CaseContainerDTO caseContainer = new CaseContainerDTO();
    String userId = "1234567890";
    String questionnaireId = A_QUESTIONNAIRE_ID;
    String formType = "H";
    String accountServiceUrl = "http://localhost:9092/start";
    String accountServiceLogoutUrl = "http://localhost:9092/start/save-and-exit";

    caseContainer.setCaseType("H");
    caseContainer.setCollectionExerciseId(collectionExerciseId);
    caseContainer.setRegion("E");
    caseContainer.setUprn(uprn); // for the 'ru_ref' field
    caseContainer.setId(caseId);
    caseContainer.setAddressLine1("ONS");
    caseContainer.setAddressLine2(null);
    caseContainer.setAddressLine3("Segensworth's Road");
    caseContainer.setTownName("Titchfield");
    caseContainer.setPostcode("PO15 5RR");
    caseContainer.setSurveyType("CENSUS");

    EqLaunchCoreData coreLaunchData =
        EqLaunchCoreData.builder()
            .language(language)
            .source(source)
            .channel(channel)
            .questionnaireId(questionnaireId)
            .formType(formType)
            .salt(SALT)
            .build();

    // Run code under to test to get a payload
    Map<String, Object> payloadMapFromComplexCall =
        eqLaunchService.createPayloadMap(
            coreLaunchData,
            caseContainer,
            userId,
            null,
            accountServiceUrl,
            accountServiceLogoutUrl);

    assertEquals(
        "expectedMap should equal the cleaned map from the complex call",
        expectedMap,
        cleanPayloadMap(payloadMapFromComplexCall));

    EqLaunchData launchData =
        build(coreLaunchData, caseContainer, userId, accountServiceUrl, accountServiceLogoutUrl);

    // Run code under test to get encrypted payload string
    String payloadStringFromSimpleCall = eqLaunchService.getEqLaunchJwe(launchData);

    // decrypt it
    String decrypted = decryptor.decrypt(payloadStringFromSimpleCall);

    // turn it back into a map
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<HashMap<String, Object>> typeRef =
        new TypeReference<HashMap<String, Object>>() {};
    Map<String, Object> payloadMapFromSimpleCall = mapper.readValue(decrypted, typeRef);

    assertEquals(
        "expectedMap should equal the cleaned map from the simple call",
        expectedMap,
        cleanPayloadMap(payloadMapFromSimpleCall));
  }

  /**
   * Calls both the public and package methods in the class under test and asserts that each returns
   * the expected map The public method returns the encrypted JWE representation of the map, the
   * package method returns the map raw. The test decodes the encrypted form and deserialises it, as
   * well as sanitising it of variable UUID and date time values. The test asserts both the
   * behaviour of the public method and its use of the package method.
   *
   * @throws Exception
   */
  @Test
  public void createFlusherPayload() throws Exception {
    KeyStore keyStoreEncryption = new KeyStore(JWTKEYS_ENCRYPTION);
    EqLaunchServiceImpl eqLaunchService = new EqLaunchServiceImpl(keyStoreEncryption);

    KeyStore keyStoreDecryption = new KeyStore(JWTKEYS_DECRYPTION);
    JweDecryptor decryptor = new JweDecryptor(keyStoreDecryption);

    // create expectation
    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put("jti", "88888888-8888-8888-8888-888888888888");
    expectedMap.put("tx_id", "88888888-8888-8888-8888-888888888888");
    expectedMap.put("iat", "12345");
    expectedMap.put("exp", "12345");
    expectedMap.put("language_code", "en");
    expectedMap.put("response_id", ENCRYTPED_RESPONSE_ID);
    expectedMap.put("channel", "rh");
    expectedMap.put("roles", "flusher");
    expectedMap.put("questionnaire_id", A_QUESTIONNAIRE_ID);
    expectedMap.put("eq_id", "census");
    expectedMap.put("period_id", "2021");
    expectedMap.put("form_type", "H");

    // create params for code under test
    Language language = Language.ENGLISH;
    Source source = Source.RESPONDENT_HOME;
    Channel channel = Channel.RH;
    String questionnaireId = A_QUESTIONNAIRE_ID;
    String formType = "H";

    EqLaunchCoreData launchData =
        EqLaunchCoreData.builder()
            .language(language)
            .source(source)
            .channel(channel)
            .questionnaireId(questionnaireId)
            .formType(formType)
            .salt(SALT)
            .build();

    // Run code under to test to get the payload map.
    Map<String, Object> payloadMapFromComplexCall =
        eqLaunchService.createPayloadMap(launchData, null, null, "flusher", null, null);

    assertEquals(
        "expectedMap should equal the cleaned map from the complex call",
        expectedMap,
        cleanPayloadMap(payloadMapFromComplexCall));

    // Run code under test to get encrypted payload string
    String payloadStringFromSimpleCall = eqLaunchService.getEqFlushLaunchJwe(launchData);

    // decrypt it
    String decrypted = decryptor.decrypt(payloadStringFromSimpleCall);

    // turn it back into a map
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<HashMap<String, Object>> typeRef =
        new TypeReference<HashMap<String, Object>>() {};
    Map<String, Object> payloadMapFromSimpleCall = mapper.readValue(decrypted, typeRef);

    assertEquals(
        "expectedMap should equal the cleaned map from the simple call",
        expectedMap,
        cleanPayloadMap(payloadMapFromSimpleCall));
  }

  @Test
  public void createEqLaunchPayload() throws Exception {
    KeyStore keyStoreEncryption = new KeyStore(JWTKEYS_ENCRYPTION);
    EqLaunchServiceImpl eqLaunchService = new EqLaunchServiceImpl(keyStoreEncryption);

    KeyStore keyStoreDecryption = new KeyStore(JWTKEYS_DECRYPTION);
    JweDecryptor decryptor = new JweDecryptor(keyStoreDecryption);

    // Load case
    CaseContainerDTO caseData = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);

    // create expectation
    Map<String, Object> expectedMap = getExpectedMap(caseData);

    // create params for code under test
    Language language = Language.ENGLISH;
    Source source = Source.CONTACT_CENTRE_API;
    Channel channel = Channel.CC;
    String questionnaireId = A_QUESTIONNAIRE_ID;
    String formType = "H";
    String agentId = "123456";
    String accountServiceLogoutUrl = "https://localhost/questionnaireSaved";

    EqLaunchCoreData coreLaunchData =
        EqLaunchCoreData.builder()
            .language(language)
            .source(source)
            .channel(channel)
            .questionnaireId(questionnaireId)
            .formType(formType)
            .salt(SALT)
            .build();

    // Run code under to test to get the payload map.
    Map<String, Object> payloadMapFromComplexCall =
        eqLaunchService.createPayloadMap(
            coreLaunchData, caseData, agentId, null, null, accountServiceLogoutUrl);

    assertEquals(
        "expectedMap should equal the cleaned map from the complex call",
        expectedMap,
        cleanPayloadMap(payloadMapFromComplexCall));

    EqLaunchData launchData =
        build(coreLaunchData, caseData, agentId, null, accountServiceLogoutUrl);

    // Run code under test to get encrypted payload string
    String payloadStringFromSimpleCall = eqLaunchService.getEqLaunchJwe(launchData);

    // decrypt it
    String decrypted = decryptor.decrypt(payloadStringFromSimpleCall);

    // turn it back into a map
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<HashMap<String, Object>> typeRef =
        new TypeReference<HashMap<String, Object>>() {};
    Map<String, Object> payloadMapFromSimpleCall = mapper.readValue(decrypted, typeRef);

    assertEquals(
        "expectedMap should equal the cleaned map from the simple call",
        expectedMap,
        cleanPayloadMap(payloadMapFromSimpleCall));
  }

  @Test
  public void createEqLaunchPayloadForSurveyTypeCCS() throws Exception {
    KeyStore keyStoreEncryption = new KeyStore(JWTKEYS_ENCRYPTION);
    EqLaunchServiceImpl eqLaunchService = new EqLaunchServiceImpl(keyStoreEncryption);

    // Load case
    CaseContainerDTO caseData = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(1);

    // create expectation
    Map<String, Object> expectedMap = getExpectedMap(caseData);

    // create params for code under test
    Language language = Language.ENGLISH;
    Source source = Source.CONTACT_CENTRE_API;
    Channel channel = Channel.CC;
    String formType = "H";
    String agentId = "123456";
    String accountServiceLogoutUrl = "https://localhost/questionnaireSaved";

    EqLaunchCoreData coreLaunchData =
        EqLaunchCoreData.builder()
            .language(language)
            .source(source)
            .channel(channel)
            .questionnaireId(A_QUESTIONNAIRE_ID)
            .formType(formType)
            .salt(SALT)
            .build();

    // Run code under to test to get the payload map.
    Map<String, Object> payloadMapFromComplexCall =
        eqLaunchService.createPayloadMap(
            coreLaunchData, caseData, agentId, null, null, accountServiceLogoutUrl);

    assertEquals(
        "expectedMap should equal the cleaned map from the complex call",
        expectedMap,
        cleanPayloadMap(payloadMapFromComplexCall));
  }

  private Map<String, Object> cleanPayloadMap(Map<String, Object> payloadMap) {
    payloadMap.put("jti", "88888888-8888-8888-8888-888888888888");
    payloadMap.put("tx_id", "88888888-8888-8888-8888-888888888888");
    payloadMap.put("iat", "12345");
    payloadMap.put("exp", "12345");
    return payloadMap;
  }

  private EqLaunchData build(
      EqLaunchCoreData coreData,
      CaseContainerDTO caseContainer,
      String userId,
      String accountServiceUrl,
      String accountServiceLogoutUrl) {
    return EqLaunchData.builder()
        .language(coreData.getLanguage())
        .source(coreData.getSource())
        .channel(coreData.getChannel())
        .questionnaireId(coreData.getQuestionnaireId())
        .formType(coreData.getFormType())
        .salt(coreData.getSalt())
        .caseContainer(caseContainer)
        .userId(userId)
        .accountServiceUrl(accountServiceUrl)
        .accountServiceLogoutUrl(accountServiceLogoutUrl)
        .build();
  }

  private Map<String, Object> getExpectedMap(CaseContainerDTO caseData) {
    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put("jti", "88888888-8888-8888-8888-888888888888");
    expectedMap.put("tx_id", "88888888-8888-8888-8888-888888888888");
    expectedMap.put("iat", "12345");
    expectedMap.put("exp", "12345");
    expectedMap.put("language_code", "en");
    expectedMap.put("response_id", ENCRYTPED_RESPONSE_ID);
    expectedMap.put("channel", "cc");
    expectedMap.put("questionnaire_id", A_QUESTIONNAIRE_ID);
    expectedMap.put("eq_id", "census");
    expectedMap.put("period_id", "2021");
    expectedMap.put("form_type", "H");
    expectedMap.put("case_type", caseData.getCaseType());
    expectedMap.put("collection_exercise_sid", caseData.getCollectionExerciseId().toString());
    expectedMap.put("region_code", "GB-ENG");
    expectedMap.put(
        "ru_ref",
        caseData.getSurveyType().equalsIgnoreCase("CCS")
            ? caseData.getId().toString()
            : caseData.getUprn());
    expectedMap.put("case_id", caseData.getId().toString());
    expectedMap.put(
        "display_address", caseData.getAddressLine1() + ", " + caseData.getAddressLine2());
    expectedMap.put("survey", caseData.getSurveyType());
    expectedMap.put("user_id", "123456");
    expectedMap.put("account_service_log_out_url", "https://localhost/questionnaireSaved");
    return expectedMap;
  }
}
