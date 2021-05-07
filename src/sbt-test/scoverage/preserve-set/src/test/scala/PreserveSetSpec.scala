import munit.FunSuite

class PreserveSetSpec extends FunSuite {

  test("PreserveSet should sum two numbers") {
    assertEquals(PreserveSet.sum(1, 2), 3)
  }
}
