package demo

object Util {
  def dcName(idx: Int): String =
    if (idx % 2 == 0) "dc1" else "dc2"
}
