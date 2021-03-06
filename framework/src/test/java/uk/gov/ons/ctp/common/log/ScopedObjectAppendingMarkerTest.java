package uk.gov.ons.ctp.common.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import java.io.StringWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.junit.jupiter.api.Test;

public class ScopedObjectAppendingMarkerTest {

  @SuppressWarnings("deprecation")
  private static final JsonFactory FACTORY =
      new MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class SimpleName {
    private String name;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class MaskedName {
    private String forename;

    @LoggingScope(scope = Scope.MASK)
    private String surname;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class HashedName {
    private String forename;

    @LoggingScope(scope = Scope.HASH)
    private String surname;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class Couple {
    private String familyName;
    private MaskedName wife;
    private MaskedName husband;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class DinnerParty {
    @LoggingScope(scope = Scope.MASK)
    private String event;

    private Couple one;
    private Couple two;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class SelfRefClass {
    static final SelfRefClass FRED = new SelfRefClass("fred", null);
    private String name;
    private SelfRefClass next;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class SensitiveName {
    private String forename;

    @LoggingScope(scope = Scope.SENSITIVE)
    private String surname;
  }

  private ScopedObjectAppendingMarker append(String fieldName, Object object) {
    return new ScopedObjectAppendingMarker(fieldName, object);
  }

  private StringWriter generateLogging(Object obj) throws Exception {
    StringWriter writer = new StringWriter();
    JsonGenerator generator = FACTORY.createGenerator(writer);

    LogstashMarker marker = append("myObject", obj);
    generator.writeStartObject();
    marker.writeTo(generator);
    generator.writeEndObject();
    generator.flush();
    return writer;
  }

  @Test
  public void shouldWriteNull() throws Exception {
    StringWriter writer = generateLogging(null);
    assertThat(writer.toString()).isEqualTo("{\"myObject\":null}");
  }

  @Test
  public void simpleWrite() throws Exception {
    StringWriter writer = generateLogging(new SimpleName("fred"));
    assertThat(writer.toString()).isEqualTo("{\"myObject\":{\"name\":\"fred\"}}");
  }

  @Test
  public void simpleWriteWithNull() throws Exception {
    StringWriter writer = generateLogging(new SimpleName(null));
    assertThat(writer.toString()).isEqualTo("{\"myObject\":{\"name\":null}}");
  }

  @Test
  public void simpleFieldValue() throws Exception {
    SingleFieldAppendingMarker marker = append("myObject", new SimpleName("fred"));
    Object value = marker.getFieldValue();
    assertThat(value).isEqualTo("ScopedObjectAppendingMarkerTest.SimpleName(name=fred)");
  }

  @Test
  public void toStringPattern() throws Exception {
    SingleFieldAppendingMarker marker =
        new ScopedObjectAppendingMarker("myObject", new SimpleName("fred"));
    assertThat(marker.toString())
        .isEqualTo("myObject=ScopedObjectAppendingMarkerTest.SimpleName(name=fred)");
  }

  @Test
  public void toStringAlternativePattern() throws Exception {
    SingleFieldAppendingMarker marker =
        new ScopedObjectAppendingMarker("myObject", new SimpleName("fred"), "{0} :: {1}");
    assertThat(marker.toString())
        .isEqualTo("myObject :: ScopedObjectAppendingMarkerTest.SimpleName(name=fred)");
  }

  @Test
  public void combinedWriteAndToString() throws Exception {
    StringWriter writer = new StringWriter();
    JsonGenerator generator = FACTORY.createGenerator(writer);

    LogstashMarker marker = append("myObject", new SimpleName("fred"));
    generator.writeStartObject();
    marker.writeTo(generator);
    generator.writeEndObject();
    generator.flush();
    assertThat(writer.toString()).isEqualTo("{\"myObject\":{\"name\":\"fred\"}}");
    assertThat(marker.toString())
        .isEqualTo("myObject=ScopedObjectAppendingMarkerTest.SimpleName(name=fred)");
  }

  @Test
  public void maskedWrite() throws Exception {
    MaskedName myObject = new MaskedName("fred", "bloggs");
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":\"****\"}}");
    assertEquals("bloggs", myObject.getSurname());
  }

  @Test
  public void maskedWriteEmpty() throws Exception {
    MaskedName myObject = new MaskedName("fred", "");
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":\"\"}}");
    assertEquals("", myObject.getSurname());
  }

  @Test
  public void maskedWriteNull() throws Exception {
    MaskedName myObject = new MaskedName("fred", null);
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":null}}");
    assertNull(myObject.getSurname());
  }

  @Test
  public void sensitivedWrite() throws Exception {
    SensitiveName myObject = new SensitiveName("fred", "bloggs");
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":\"51c2d884\"}}");
    assertEquals("bloggs", myObject.getSurname());
  }

  @Test
  public void sensitivedWriteRedacted() throws Exception {
    SensitiveName myObject = new SensitiveName("fred", "REDACTED");
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":\"REDACTED\"}}");
    assertEquals("REDACTED", myObject.getSurname());
  }

  @Test
  public void sensitivedWriteNull() throws Exception {
    SensitiveName myObject = new SensitiveName("fred", null);
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":null}}");
    assertNull(myObject.getSurname());
  }

  @Test
  public void sensitivedWriteEmpty() throws Exception {
    SensitiveName myObject = new SensitiveName("fred", "");
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":\"\"}}");
    assertTrue(myObject.getSurname().isEmpty());
  }

  @Test
  public void hashedWrite() throws Exception {
    HashedName myObject = new HashedName("fred", "bloggs");
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":\"51c2d884\"}}");
    assertEquals("bloggs", myObject.getSurname());
  }

  @Test
  public void hashedWriteEmpty() throws Exception {
    HashedName myObject = new HashedName("fred", "");
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":\"\"}}");
    assertEquals("", myObject.getSurname());
  }

  @Test
  public void hashedWriteNull() throws Exception {
    HashedName myObject = new HashedName("fred", null);
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":null}}");
    assertNull(myObject.getSurname());
  }

  @Test
  public void nestedWrite() throws Exception {
    MaskedName husband = new MaskedName("fred", "bloggs");
    MaskedName wife = new MaskedName("jane", "smith");

    Couple myObject = new Couple("smith-bloggs", wife, husband);
    StringWriter writer = generateLogging(myObject);
    String expected =
        "{\"myObject\":{\"familyName\":\"smith-bloggs\","
            + "\"wife\":{\"forename\":\"jane\",\"surname\":\"****\"},"
            + "\"husband\":{\"forename\":\"fred\",\"surname\":\"****\"}}}";
    assertThat(writer.toString()).isEqualTo(expected);
  }

  @Test
  public void nestedWriteWithNull() throws Exception {
    MaskedName husband = new MaskedName("fred", "bloggs");
    MaskedName wife = null;

    Couple myObject = new Couple("smith-bloggs", wife, husband);
    StringWriter writer = generateLogging(myObject);
    String expected =
        "{\"myObject\":{\"familyName\":\"smith-bloggs\","
            + "\"wife\":null,"
            + "\"husband\":{\"forename\":\"fred\",\"surname\":\"****\"}}}";
    assertThat(writer.toString()).isEqualTo(expected);
  }

  @Test
  public void multiNestedWrite() throws Exception {
    MaskedName husband = new MaskedName("fred", "bloggs");
    MaskedName wife = new MaskedName("jane", "smith");
    Couple couple1 = new Couple("smith-bloggs", wife, husband);

    MaskedName husband2 = new MaskedName("jim", "jones");
    MaskedName wife2 = new MaskedName("susie", "cook");
    Couple couple2 = new Couple("cook-jones", wife2, husband2);

    DinnerParty party = new DinnerParty("anniversary", couple1, couple2);

    StringWriter writer = generateLogging(party);
    String expected =
        "{\"myObject\":{\"event\":\"****\","
            + "\"one\":{\"familyName\":\"smith-bloggs\","
            + "\"wife\":{\"forename\":\"jane\",\"surname\":\"****\"},"
            + "\"husband\":{\"forename\":\"fred\",\"surname\":\"****\"}},"
            + "\"two\":{\"familyName\":\"cook-jones\","
            + "\"wife\":{\"forename\":\"susie\",\"surname\":\"****\"},"
            + "\"husband\":{\"forename\":\"jim\",\"surname\":\"****\"}}}}";
    assertThat(writer.toString()).isEqualTo(expected);
  }

  @Test
  public void shouldHandleSelfReferencingClasses() throws Exception {
    StringWriter writer = generateLogging(new SelfRefClass("Jim", null));
    assertThat(writer.toString()).isEqualTo("{\"myObject\":{\"name\":\"Jim\",\"next\":null}}");
  }
}
