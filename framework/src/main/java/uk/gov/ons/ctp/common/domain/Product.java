package uk.gov.ons.ctp.common.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
  private String fulfilmentCode;
  private String description;
  private ProductGroup productGroup;
  private DeliveryChannel deliveryChannel;
  private List<Language> languages;
}
