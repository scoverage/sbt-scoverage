import munit.FunSuite

class JvmTest extends FunSuite {

  test("JVM UnderTest work on JVM") {
    assertEquals(UnderTest.jvmMethod, "jvm")
  }

}
