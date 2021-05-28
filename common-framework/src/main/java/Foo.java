public class Foo {

  public static void main(String[] args) {
    double sleep = 300;
    double multiplier = 1.3;
    double accrued = 0.0;
    for (int i = 0; i < 30; i++) {
      System.out.println(
          String.format("iter %d sleep %f accreued %f", i, sleep / 1000, accrued / 1000));
      sleep = sleep * multiplier;
      accrued += sleep;
    }
  }
}
