import org.specs2.mutable._

class PreserveSetSpec extends Specification {

  "PreserveSet" should {
    "sum two numbers" in {
      PreserveSet.sum(1, 2) mustEqual 3
    }
  }
}
