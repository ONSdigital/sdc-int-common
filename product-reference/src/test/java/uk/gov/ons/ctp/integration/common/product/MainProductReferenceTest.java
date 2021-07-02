package uk.gov.ons.ctp.integration.common.product;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.CaseType;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;

/**
 * Runs the core tests against the production json, as well as additional tests specified below
 *
 * @author philwhiles
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class MainProductReferenceTest extends ProductReferenceTest {

  @Test
  public void totalProductsCheck() throws Exception {
    Product example = new Product();
    List<Product> products = productReference.searchProducts(example);
    assertEquals(132, products.size());
  }

  @Test
  public void mytotalProductsCheck() throws Exception {
    Product example = new Product();
    example.setCaseTypes(Arrays.asList(CaseType.HH));
    example.setRegions(Arrays.asList(Region.E));
    example.setIndividual(true);

    List<Product> products = productReference.searchProducts(example);
    assertEquals(6, products.size());
  }
}
