object assertions {
  def test(name: String)(a: Unit): Unit = ()

  test("list") {
    val list = List("foo", "bar")

    //fails ✅
    assert(list.size == 3)

    //never gets run ❌
    assert(list.forall(_ == "moo"))
  }
}
