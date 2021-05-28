package uk.gov.ons.ctp.integration.eqlaunch.service;

import uk.gov.ons.ctp.common.error.CTPException;

public interface EqLaunchService {
  String getEqLaunchJwe(EqLaunchData launchData) throws CTPException;

  String getEqFlushLaunchJwe(EqLaunchCoreData launchData) throws CTPException;
}
