import org.specs2.mutable._

class NotExcludedSpec extends Specification {

  "NotExcluded" should {
    "sum two numbers" in {
      new NotExcluded().f() mustEqual 15
    }
  }
}
