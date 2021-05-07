import munit.FunSuite

class SharedTest extends FunSuite {

  test("Shared UnderTest return where it works") {
    assertEquals(UnderTest.onJsAndJvm, "js and jvm")
  }

}
