import munit.FunSuite
import org.scoverage.issue53.part.a.AdderScala

/** Created by Mikhail Kokho on 7/10/2015.
  */
class AdderTestSuite extends FunSuite {
  test("Adder should sum two numbers") {
    assertEquals(AdderScala.add(1, 2), 3)
  }
}
