import org.scalatest.{FlatSpec, Matchers}

class SharedTest extends FlatSpec with Matchers {

  "Shared UnderTest" should "return where it works" in {
    UnderTest.onJsAndJvm shouldBe "js and jvm"
  }

}
