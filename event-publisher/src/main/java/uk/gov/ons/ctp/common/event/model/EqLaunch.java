package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EqLaunch implements EventPayload {
  private String qid;
}
