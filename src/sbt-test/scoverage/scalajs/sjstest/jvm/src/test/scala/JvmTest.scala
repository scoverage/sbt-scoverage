import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JvmTest extends AnyFlatSpec with Matchers {

  "JVM UnderTest" should "work on JVM" in {
    UnderTest.jvmMethod shouldBe "jvm"
  }

}

