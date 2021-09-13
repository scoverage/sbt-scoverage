import munit.FunSuite

class BadCoverageSpec extends FunSuite {

  test("one.BadCoverage should sum two numbers") {
    assertEquals(one.BadCoverage.sum(1, 2), 3)
    assertEquals(one.BadCoverage.sum(0, 3), 3)
    assertEquals(one.BadCoverage.sum(3, 0), 3)
  }

  test("two.BadCoverage should sum two numbers") {
    assertEquals(two.BadCoverage.sum(1, 2), 3)
  }

}
