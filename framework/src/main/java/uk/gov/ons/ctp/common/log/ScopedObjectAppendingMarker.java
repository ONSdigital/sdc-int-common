package uk.gov.ons.ctp.common.log;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.cloud.Date;
import com.google.common.hash.Hashing;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.impl.ConfigurableMapper;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.marker.ObjectAppendingMarker;
import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * Provide a customised version of the appender code, so that we can apply logging scopes to
 * obfuscate annotated fields. This behaviour is inspired by the godaddy annotation logging scopes,
 * but applied in a simpler fashion, with minor restrictions.
 *
 * <p>Limitations:
 *
 * <ul>
 *   <li>only masks/hashes String types
 * </ul>
 */
@SuppressWarnings("serial")
public class ScopedObjectAppendingMarker extends ObjectAppendingMarker {
  private static final MapperFacade mapper = new ConfigurableMapper();
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

  private boolean shouldRecurse(Field f, Class<?> parentClass) {
    Class<?> clazz = f.getType();
    if (clazz == parentClass
        || Modifier.isStatic(f.getModifiers())
        || f.isEnumConstant()
        || clazz.isPrimitive()
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
        if (shouldRecurse(f, clazz)) {
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

    if (scopedObject != null && hasObfuscation(scopedObject.getClass())) {
      copyObject();
      processObjectAnnotation(scopedObject);
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
          if (shouldRecurse(f, clazz)) {
            processObjectAnnotation(FieldUtils.readField(f, obj, true));
          }
        } else {
          if (f.getType() != String.class) {
            // report problem to standard out since we don't want to log from
            // within the logging code itself
            System.out.println("Cannot mask/hash non-string field: " + f);
            continue;
          }
          FieldUtils.writeField(f, obj, scopedValue, true);
        }
      } catch (IllegalAccessException e) {
        // report problem to standard out since we don't want to log from
        // within the logging code itself
        e.printStackTrace();
      }
    }
  }

  private void copyObject() {
    scopedObject = mapper.map(scopedObject, scopedObject.getClass());
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
