import org.scalatest.{FlatSpec, Matchers}

class GoodCoverageSpec extends FlatSpec with Matchers {
  "sum two numbers" should "be mathematically corrent sum" in {
    GoodCoverage.sum(1, 2) shouldBe 3
  }
}
