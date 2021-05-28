package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressNotValid implements EventPayload {
  private CollectionCaseCompact collectionCase;
  private String reason;
  private String notes;
}
