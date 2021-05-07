import munit.FunSuite

/** Created by tbarke001c on 7/8/14.
  */
class GoodCoverageSpec extends FunSuite {

  test("GoodCoverage should sum two numvers") {
    assertEquals(GoodCoverage.sum(1, 2), 3)
  }
}
