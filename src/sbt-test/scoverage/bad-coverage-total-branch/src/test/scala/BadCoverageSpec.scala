import munit.FunSuite

class BadCoverageSpec extends FunSuite {

  test("BadCoverage should sum two numbers") {
    assertEquals(BadCoverage.sum(1, 2), 3)
    assertEquals(BadCoverage.sum(0, 3), 3)
  }

}
