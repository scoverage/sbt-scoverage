import munit.FunSuite
import b.AdderScala

class AdderTestSuite extends FunSuite {
  test("Adder should sum two numbers") {
    assertEquals(AdderScala.add(1, 2), 3)
  }
}
