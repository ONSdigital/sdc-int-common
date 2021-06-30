package uk.gov.ons.ctp.common.log;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.Date;
import com.google.common.hash.Hashing;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.UUID;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.marker.ObjectAppendingMarker;
import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * Provide a customised version of the appender code, so that we can apply logging scopes to
 * obfuscate annotated fields. This behaviour is inspired by the godaddy annotation logging scopes,
 * but applied in a simpler fashion.
 *
 * <p>Limitations:
 *
 * <ul>
 *   <li>only masks/hashes String types
 *   <li>does not recurse down object tree
 * </ul>
 */
@SuppressWarnings("serial")
public class ScopedObjectAppendingMarker extends ObjectAppendingMarker {
  private static final ObjectMapper mapper = new ObjectMapper();
  private Object scopedObject;
  private boolean scopesProcessed;

  public ScopedObjectAppendingMarker(String fieldName, Object object) {
    super(fieldName, object);
    this.scopedObject = object;
  }

  public ScopedObjectAppendingMarker(String fieldName, Object object, String messageFormatPattern) {
    super(fieldName, object, messageFormatPattern);
    this.scopedObject = object;
  }

  private boolean shouldRecurse(Field f) {
    Class<?> clazz = f.getType();
    if (clazz.isPrimitive()
        || clazz == String.class
        || clazz == Date.class
        || clazz == UUID.class
        || clazz.isEnum()) {
      return false;
    }
    return clazz.getPackageName().startsWith("uk.gov.ons");
  }

  private boolean hasObfuscation(Class<?> clazz) {
    Field[] fields = clazz.getDeclaredFields();

    for (Field f : fields) {
      LoggingScope loggingScope = f.getAnnotation(LoggingScope.class);
      Scope scope = loggingScope == null ? Scope.LOG : loggingScope.scope();

      if (scope == Scope.LOG) {
        if (shouldRecurse(f)) {
          if (hasObfuscation(f.getType())) {
            return true;
          }
        }
      } else {
        return true;
      }
    }
    return false;
  }

  private void processAnnotations() {
    if (scopesProcessed) {
      return;
    }

    if (hasObfuscation(scopedObject.getClass())) {
      try {
        copyObject();
        processObjectAnnotation(scopedObject);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
    scopesProcessed = true;
  }

  private void processObjectAnnotation(Object obj) {
    Class<?> clazz = obj.getClass();

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
            Object value = FieldUtils.readField(f, obj, true);
            scopedValue = hash(value);
            break;
          default:
            break;
        }

        if (scopedValue == null) {
          if (shouldRecurse(f)) {
            processObjectAnnotation(FieldUtils.readField(f, obj, true));
          }
        } else {
          if (f.getType() != String.class) {
            System.out.println("Cannot mask/hash non-string field: " + f);
            continue;
          }
          FieldUtils.writeField(f, obj, scopedValue, true);
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }

  private void copyObject() throws JsonProcessingException {
    scopedObject =
        mapper.readValue(mapper.writeValueAsString(scopedObject), scopedObject.getClass());
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
