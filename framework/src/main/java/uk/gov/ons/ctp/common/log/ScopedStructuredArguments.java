package uk.gov.ons.ctp.common.log;

import static net.logstash.logback.argument.StructuredArguments.DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN;
import static net.logstash.logback.argument.StructuredArguments.VALUE_ONLY_MESSAGE_FORMAT_PATTERN;

import net.logstash.logback.argument.StructuredArgument;

public class ScopedStructuredArguments {

  static StructuredArgument keyValue(String key, Object value, String messageFormatPattern) {
    return new ScopedObjectAppendingMarker(key, value, messageFormatPattern);
  }

  public static StructuredArgument keyValue(String key, Object value) {
    return keyValue(key, value, DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN);
  }

  public static StructuredArgument value(String key, Object value) {
    return keyValue(key, value, VALUE_ONLY_MESSAGE_FORMAT_PATTERN);
  }

  public static StructuredArgument kv(String key, Object value) {
    return keyValue(key, value);
  }

  // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
  public static StructuredArgument v(String key, Object value) {
    return value(key, value);
  }
}
