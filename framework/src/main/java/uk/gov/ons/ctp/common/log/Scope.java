package uk.gov.ons.ctp.common.log;

public enum Scope {
  /** Default Scope. Field/Method will be logged. * */
  LOG,

  /** Field/Method will be masked, if not null or empty string. */
  MASK,

  /** Field/Method will be hashed with the HashProcessor, if not null or empty string. */
  HASH,

  /**
   * Nuanced obfuscation based on sensitive data from RM.
   *
   * <ul>
   *   <li>If the value is null, emit null
   *   <li>If the value is the empty string , emit the empty string
   *   <li>If the value is "REDACTED", emit "REDACTED"
   *   <li>Otherwise, behave as {@link HASH}
   * </ul>
   */
  SENSITIVE;
}
