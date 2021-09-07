package one

object BadCoverage {

  def sum(num1: Int, num2: Int) = {
    if (0 == num1) num2 else if (0 == num2) num1 else num1 + num2
  }

}
