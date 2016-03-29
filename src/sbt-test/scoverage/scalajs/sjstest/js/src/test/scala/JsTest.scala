import org.scalatest.{FlatSpec, Matchers}

class JsTest extends FlatSpec with Matchers {

  "JS UnderTest" should "work on JS" in {
    UnderTest.jsMethod shouldBe "js"
  }

}

