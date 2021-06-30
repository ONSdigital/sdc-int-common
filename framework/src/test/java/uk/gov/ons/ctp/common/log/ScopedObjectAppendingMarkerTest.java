package uk.gov.ons.ctp.common.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import java.io.StringWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.logstash.logback.marker.LogstashMarker;
import org.junit.Test;

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
  static class SensitiveName {
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
    private SensitiveName wife;
    private SensitiveName husband;
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

  private LogstashMarker append(String fieldName, Object object) {
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
  public void simpleWrite() throws Exception {
    StringWriter writer = generateLogging(new SimpleName("fred"));
    assertThat(writer.toString()).isEqualTo("{\"myObject\":{\"name\":\"fred\"}}");
  }

  @Test
  public void sensitiveWrite() throws Exception {
    SensitiveName myObject = new SensitiveName("fred", "bloggs");
    StringWriter writer = generateLogging(myObject);
    assertThat(writer.toString())
        .isEqualTo("{\"myObject\":{\"forename\":\"fred\",\"surname\":\"****\"}}");
    assertEquals("bloggs", myObject.getSurname());
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
  public void nestedWrite() throws Exception {
    SensitiveName husband = new SensitiveName("fred", "bloggs");
    SensitiveName wife = new SensitiveName("jane", "smith");

    Couple myObject = new Couple("smith-bloggs", wife, husband);
    StringWriter writer = generateLogging(myObject);
    String expected =
        "{\"myObject\":{\"familyName\":\"smith-bloggs\","
            + "\"wife\":{\"forename\":\"jane\",\"surname\":\"****\"},"
            + "\"husband\":{\"forename\":\"fred\",\"surname\":\"****\"}}}";
    assertThat(writer.toString()).isEqualTo(expected);
  }

  @Test
  public void multiNestedWrite() throws Exception {
    SensitiveName husband = new SensitiveName("fred", "bloggs");
    SensitiveName wife = new SensitiveName("jane", "smith");
    Couple couple1 = new Couple("smith-bloggs", wife, husband);

    SensitiveName husband2 = new SensitiveName("jim", "jones");
    SensitiveName wife2 = new SensitiveName("susie", "cook");
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
}
