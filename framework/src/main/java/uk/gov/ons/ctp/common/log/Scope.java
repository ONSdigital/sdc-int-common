package uk.gov.ons.ctp.common.log;

public enum Scope {
  /** Default Scope. Field/Method will be logged. * */
  LOG,

  /** Field/Method will be masked. * */
  MASK,

  /** Field/Method will be hashed with the HashProcessor * */
  HASH;
}
