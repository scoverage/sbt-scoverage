import org.specs2.mutable._

/**
 * Created by tbarke001c on 7/8/14.
 */
class GoodCoverageSpec extends Specification {

  "GoodCoverage" should {
    "sum two numbers" in {
      GoodCoverage.sum(1, 2) mustEqual 3
    }
  }
}
