import org.specs2.mutable._
import org.scoverage.issue53.part.b.SubtractorScala

/**
 * Created by Mikhail Kokho on 7/10/2015.
 */
class SubtractorTestSuite extends Specification {
  "Subtractor" should {
    "subtract two numbers" in {
      SubtractorScala.minus(2, 1) mustEqual 1
    }
  }
}

