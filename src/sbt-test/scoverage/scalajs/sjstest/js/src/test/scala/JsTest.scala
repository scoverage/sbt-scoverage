import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsTest extends AnyFlatSpec with Matchers {

  "JS UnderTest" should "work on JS" in {
    UnderTest.jsMethod shouldBe "js"
  }

}

