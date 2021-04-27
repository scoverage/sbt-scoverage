import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SharedTest extends AnyFlatSpec with Matchers {

  "Shared UnderTest" should "return where it works" in {
    UnderTest.onJsAndJvm shouldBe "js and jvm"
  }

}
