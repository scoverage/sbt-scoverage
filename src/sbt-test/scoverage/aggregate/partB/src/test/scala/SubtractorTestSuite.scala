import munit.FunSuite
import org.scoverage.issue53.part.b.SubtractorScala

/** Created by Mikhail Kokho on 7/10/2015.
  */
class SubtractorTestSuite extends FunSuite {
  test("Subtractor should substract two numbers") {
    assertEquals(SubtractorScala.minus(2, 1), 1)
  }
}
