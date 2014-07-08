import org.specs2.mutable._

/**
 * Created by tbarke001c on 7/8/14.
 */
class BadCoverageSpec extends Specification {

  "BadCoverage" should {
    "sum two numbers" in {
      BadCoverage.sum(1, 2) mustEqual 3
    }
  }
}
