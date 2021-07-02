package uk.gov.ons.ctp.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TestHelperTest {

  @Test
  public void testValidateAsDateTimeWithValidDateTime() {
    TestHelper.validateAsDateTime("2019-04-10T15:32:38.941+01:00");
  }

  @Test
  public void testValidateAsDateTimeWithNullDateTime() {
    assertThrows(AssertionError.class, () -> TestHelper.validateAsDateTime(null));
  }

  @Test
  public void testValidateAsDateTimeWithInvalidDateTime() {
    assertThrows(AssertionError.class, () -> TestHelper.validateAsDateTime("2019-04-10T15:32pm"));
  }

  @Test
  public void testValidateAsUUIDValid() {
    String uuid = "4c2cad7f-a942-4fe8-a04e-8d0fbd99f462";
    TestHelper.validateAsUUID(uuid);
  }

  @Test
  public void testValidateAsUUIDNull() {
    assertThrows(AssertionError.class, () -> TestHelper.validateAsUUID(null));
  }

  @Test
  public void testValidateAsUUIDInvalid() {
    String uuid = "2344-234234";
    assertThrows(AssertionError.class, () -> TestHelper.validateAsUUID(uuid));
  }
}
