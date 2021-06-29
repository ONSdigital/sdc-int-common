package uk.gov.ons.ctp.common.log;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.hash.Hashing;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.marker.ObjectAppendingMarker;
import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * Provide a customised version of the appender code, so that we can apply logging scopes to
 * obfuscate annotated fields. This behaviour is inspired by the godaddy annotation logging scopes,
 * but applied in a simpler fashion.
 */
@SuppressWarnings("serial")
public class ScopedObjectAppendingMarker extends ObjectAppendingMarker {

  private final Object scopedObject;
  private boolean scopesProcessed;

  public ScopedObjectAppendingMarker(String fieldName, Object object) {
    super(fieldName, object);
    this.scopedObject = object;
  }

  public ScopedObjectAppendingMarker(String fieldName, Object object, String messageFormatPattern) {
    super(fieldName, object, messageFormatPattern);
    this.scopedObject = object;
  }

  private void processAnnotations() {
    if (scopesProcessed) {
      return;
    }

    Class<?> clazz = scopedObject.getClass();

    Field[] fields = clazz.getDeclaredFields();

    for (Field f : fields) {
      LoggingScope loggingScope = f.getAnnotation(LoggingScope.class);
      Scope scope = loggingScope == null ? Scope.LOG : loggingScope.scope();

      try {
        String scopedValue = null;

        switch (scope) {
          case MASK:
            scopedValue = "****";
            break;
          case HASH:
            Object value = FieldUtils.readField(f, scopedObject, true);
            scopedValue = hash(value);
            break;
          default:
            break;
        }

        if (scopedValue != null) {
          FieldUtils.writeField(f, scopedObject, scopedValue, true);
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    scopesProcessed = true;
  }

  // provide a short hash for a field to obfuscate it.
  private String hash(Object object) {
    ByteArrayOutputStream binaryStream = new ByteArrayOutputStream();

    try {
      ObjectOutputStream outputStream = new ObjectOutputStream(binaryStream);
      outputStream.writeObject(object);
    } catch (IOException e) {
      return "<Error Hashing>";
    }
    return Hashing.murmur3_32().hashBytes(binaryStream.toByteArray()).toString();
  }

  @Override
  protected void writeFieldValue(JsonGenerator generator) throws IOException {
    processAnnotations();
    generator.writeObject(scopedObject);
  }

  @Override
  public Object getFieldValue() {
    processAnnotations();
    return StructuredArguments.toString(scopedObject);
  }
}
