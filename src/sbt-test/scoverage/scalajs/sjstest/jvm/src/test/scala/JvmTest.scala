import org.scalatest.{FlatSpec, Matchers}

class JvmTest extends FlatSpec with Matchers {

  "JVM UnderTest" should "work on JVM" in {
    UnderTest.jvmMethod shouldBe "jvm"
  }

}

