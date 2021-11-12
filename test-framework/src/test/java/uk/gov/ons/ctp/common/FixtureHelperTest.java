package uk.gov.ons.ctp.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.junit.jupiter.api.Test;

public class FixtureHelperTest {

  @Data
  static class SimpleFixture {
    private String name;
  }

  @Data
  static class AnotherFixture {
    private int num;
    private String name;
  }

  @Test
  public void shouldLoadSimpleFixture() {
    var array = FixtureHelper.loadClassFixtures(SimpleFixture[].class);
    assertEquals(1, array.size());
    assertEquals("Fred", array.get(0).getName());
  }

  @Test
  public void shouldLoadSimpleFixtureAtPackageLevel() {
    var array = FixtureHelper.loadPackageFixtures(SimpleFixture[].class);
    assertEquals(1, array.size());
    assertEquals("Mary", array.get(0).getName());
  }

  @Test
  public void shouldLoadSimpleFixtureWithQualifier() {
    var array = FixtureHelper.loadClassFixtures(SimpleFixture[].class, "valid");
    assertEquals(1, array.size());
    assertEquals("Jim", array.get(0).getName());
  }

  @Test
  public void shouldLoadSimpleFixtureWithQualifierAtPackageLevel() {
    var array = FixtureHelper.loadPackageFixtures(SimpleFixture[].class, "valid");
    assertEquals(1, array.size());
    assertEquals("Albert", array.get(0).getName());
  }

  @Test
  public void shouldLoadClassNode() {
    var node = FixtureHelper.loadClassObjectNode();
    assertNotNull(node);
    assertEquals("Jane", new ObjectMapper().convertValue(node, SimpleFixture.class).getName());
  }

  @Test
  public void shouldLoadClassNodeWithQualifier() {
    var node = FixtureHelper.loadClassObjectNode("anyjson");
    assertNotNull(node);
    assertTrue(node.toString().contains("Frank"));
  }

  @Test
  public void shouldLoadPackageNodeWithQualifier() {
    var node = FixtureHelper.loadPackageObjectNode("anyjson");
    assertNotNull(node);
    assertTrue(node.toString().contains("Emily"));
  }

  @Test
  public void shouldLoadPackageNode() {
    var node = FixtureHelper.loadPackageObjectNode();
    assertNotNull(node);
    assertEquals("James", new ObjectMapper().convertValue(node, SimpleFixture.class).getName());
  }

  @Test
  public void shouldLoadFixtureWithInteger() {
    var array = FixtureHelper.loadClassFixtures(AnotherFixture[].class);
    assertEquals(1, array.size());
    assertEquals("Pete", array.get(0).getName());
    assertEquals(3, array.get(0).getNum());
  }

  @Test
  public void shouldRejectSimpleFixtureWithBadAttribute() {
    assertThrows(
        Exception.class, () -> FixtureHelper.loadClassFixtures(SimpleFixture[].class, "badattr"));
  }

  @Test
  public void shouldRejectSimpleFixtureWithBadAttributeAtPackageLevel() {
    assertThrows(
        Exception.class, () -> FixtureHelper.loadPackageFixtures(SimpleFixture[].class, "badattr"));
  }

  @Test
  public void shouldRejectSimpleFixtureWithExtraAttributes() {
    assertThrows(
        Exception.class, () -> FixtureHelper.loadClassFixtures(SimpleFixture[].class, "extra"));
  }

  @Test
  public void shouldRejectFixtureWithFloatThatShouldBeInteger() {
    assertThrows(
        Exception.class, () -> FixtureHelper.loadClassFixtures(AnotherFixture[].class, "badfloat"));
  }

  @Test
  public void shouldRejectFixtureWithNullThatShouldBeInteger() {
    assertThrows(
        Exception.class, () -> FixtureHelper.loadClassFixtures(AnotherFixture[].class, "badnull"));
  }
}
