package uk.gov.ons.ctp.integration.eqlaunch.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.model.UacUpdate;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder = true)
@Data
public class EqLaunchCoreData {
  @NonNull private Language language;
  @NonNull private Source source;
  @NonNull private Channel channel;
  @NonNull private UacUpdate uacUpdate;
  @NonNull private String salt;

  public EqLaunchCoreData coreCopy() {
    return this.toBuilder().build();
  }
}
