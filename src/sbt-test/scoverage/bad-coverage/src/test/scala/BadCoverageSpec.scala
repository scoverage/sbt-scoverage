import munit.FunSuite

/** Created by tbarke001c on 7/8/14.
  */
class BadCoverageSpec extends FunSuite {

  test("BadCoverage should sum two numbers") {
    assertEquals(BadCoverage.sum(1, 2), 3)
  }
}
