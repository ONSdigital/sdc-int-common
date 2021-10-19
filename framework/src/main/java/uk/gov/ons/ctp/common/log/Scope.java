package uk.gov.ons.ctp.common.log;

public enum Scope {
  /** Default Scope. Field/Method will be logged. * */
  LOG,

  /** Field/Method will be masked. * */
  MASK,

  /** Field/Method will be hashed with the HashProcessor * */
  HASH,

  /**
   * Nuanced obfuscation based on sensitive data from RM.
   *
   * <p>
   *
   * <ul>
   *   <li>If the value is null or the empty string , emit the empty string
   *   <li>If the value is "REDACTED", emit "REDACTED"
   *   <li>Otherwise, behave as {@link Scope.HASH}
   * </ul>
   */
  SENSITIVE;
}
