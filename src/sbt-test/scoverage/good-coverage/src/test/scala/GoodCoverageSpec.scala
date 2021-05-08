import munit.FunSuite

/** Created by tbarke001c on 7/8/14.
  */
class GoodCoverageSpec extends FunSuite {

  test("GoodCoverage should sum two numbers") {
    assertEquals(GoodCoverage.sum(1, 2), 3)
    assertEquals(GoodCoverage.sum(0, 3), 3)
    assertEquals(GoodCoverage.sum(3, 0), 3)
  }

  test("two.GoodCoverage should sum two numbers") {
    assertEquals(two.GoodCoverage.sum(1, 2), 3)
    assertEquals(two.GoodCoverage.sum(0, 3), 3)
    assertEquals(two.GoodCoverage.sum(3, 0), 3)
  }

}
