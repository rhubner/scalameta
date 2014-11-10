import org.scalatest._
import scala.meta.syntactic.show._

class ShowSuite extends ParseSuite {
  test("val x: Int (raw)") {
    val tree = templStat("val x: Int")
    assert(tree.show[Raw] === "Decl.Val(Nil, List(Term.Name(\"x\")), Type.Name(\"Int\"))")
  }

  test("val x: Int (code)") {
    val tree = templStat("val x: Int")
    assert(tree.show[Code] === "val x: Int")
  }

  test("~(1 + 2) + ~x.y(z) + (~x).y(z)") {
    val tree = templStat("~(1 + 2) + ~x.y(z) + (~x).y(z)")
    assert(tree.show[Code] === "~(1 + 2) + ~x.y(z) + (~x).y(z)")
  }

  test("(a + b + c) && (a + (b + c)) && (a :: b :: c) && ((a :: b) :: c)") {
    val tree = templStat("(a + b + c) && (a + (b + c)) && (a :: b :: c) && ((a :: b) :: c)")
    assert(tree.show[Code] === "a + b + c && a + (b + c) && (a :: b :: c) && ((a :: b) :: c)")
  }

  test("(x map y).foo") {
    val tree = templStat("(x map y).foo")
    assert(tree.show[Code] === "(x map y).foo")
  }

  test("string literals with newlines and double quotes") {
    val tree = templStat("""{
      val x = QQQ
        x
      QQQ
      val y = "\""
    }""".replace("QQQ", "\"\"\""))
    assert(tree.show[Raw] === """Term.Block(List(Defn.Val(Nil, List(Term.Name("x")), None, Lit.String("\n        x\n      ")), Defn.Val(Nil, List(Term.Name("y")), None, Lit.String("\""))))""")
    assert(tree.show[Code] === """
    |{
    |  val x = QQQ
    |        x
    |      QQQ
    |  val y = "\""
    |}
    """.trim.stripMargin.replace("QQQ", "\"\"\""))
  }

  test("interpolations") {
    val tree = templStat("""{
      val x = q"123 + $x + ${foo(123)} + 456"
      val y = QQQ
        $x
        $y
        ..$z
      QQQ
    }""".replace("QQQ", "\"\"\""))
    assert(tree.show[Raw] === """Term.Block(List(Defn.Val(Nil, List(Term.Name("x")), None, Term.Interpolate(Term.Name("q"), List(Lit.String("123 + "), Lit.String(" + "), Lit.String(" + 456")), List(Term.Name("x"), Term.Apply(Term.Name("foo"), List(Lit.Int(123)))))), Defn.Val(Nil, List(Term.Name("y")), None, Lit.String("\n        $x\n        $y\n        ..$z\n      "))))""")
    assert(tree.show[Code] === """
    |{
    |  val x = q"123 + $x + ${foo(123)} + 456"
    |  val y = QQQ
    |        $x
    |        $y
    |        ..$z
    |      QQQ
    |}
    """.trim.stripMargin.replace("QQQ", "\"\"\""))
  }

  test("foo.bar(bar) { baz }") {
    val tree = templStat("foo.bar(bar) { baz }")
    assert(tree.show[Code] === """
      |foo.bar(bar) {
      |  baz
      |}
    """.trim.stripMargin)
  }

  test("Template.self stringifications") {
    assert(templStat("new { val x = 2 }").show[Code] === "new { val x = 2 }")
    assert(templStat("new { self => val x = 2 }").show[Code] === "new { self => val x = 2 }")
    assert(templStat("new { self: Int => val x = 2 }").show[Code] === "new { self: Int => val x = 2 }")
    assert(templStat("""
      new {
        val x = 2
        val y = 3
      }
    """).show[Code] === """
      |new {
      |  val x = 2
      |  val y = 3
      |}
    """.trim.stripMargin)
    assert(templStat("""
      new { self =>
        val x = 2
        val y = 3
      }
    """).show[Code] === """
      |new { self =>
      |  val x = 2
      |  val y = 3
      |}
    """.trim.stripMargin)
    assert(templStat("""
      new { self: Int =>
        val x = 2
        val y = 3
      }
    """).show[Code] === """
      |new { self: Int =>
      |  val x = 2
      |  val y = 3
      |}
    """.trim.stripMargin)
    assert(templStat("class B { x: B => }").show[Code] === "class B { x: B => }")
  }

  test("new X") {
    assert(templStat("new X").show[Code] === "new X")
    assert(templStat("new X {}").show[Code] === "new X {}")
  }

  test("ascribe and annotate") {
    assert(templStat("_: Int").show[Code] === "_: Int")
    assert(templStat("(_: Int) + 2").show[Code] === "(_: Int) + 2")
    assert(templStat("x: @foo").show[Code] === "x: @foo")
    assert(templStat("(x: @foo) + 2").show[Code] === "(x: @foo) + 2")
  }

  test("compound types") {
    assert(tpe("Foo").show[Code] === "Foo")
    assert(tpe("Foo {}").show[Code] === "Foo")
    assert(tpe("Foo { type T = Int }").show[Code] === "Foo { type T = Int }")
    assert(tpe("Foo { type T = Int; type U <: String }").show[Code] === "Foo { type T = Int; type U <: String }")
    assert(tpe("Foo with Bar").show[Code] === "Foo with Bar")
    assert(tpe("Foo with Bar {}").show[Code] === "Foo with Bar {}")
    assert(tpe("Foo with Bar { type T = Int }").show[Code] === "Foo with Bar { type T = Int }")
    assert(tpe("Foo with Bar { type T = Int; type U <: String }").show[Code] === "Foo with Bar { type T = Int; type U <: String }")
  }

  test("packages") {
    assert(topLevel("package foo.bar; class C").show[Code] === "package foo.bar\nclass C")
    assert(topLevel("package foo.bar; class C; class D").show[Code] === "package foo.bar\nclass C\nclass D")
    assert(topLevel("package foo.bar { class C }").show[Code] === "package foo.bar {\n  class C\n}")
    assert(topLevel("package foo.bar { class C; class D }").show[Code] === "package foo.bar {\n  class C\n  class D\n}")
  }

  test("type parameter mods") {
    assert(topLevel("class C[@foo T]").show[Code] === "class C[@foo T]")
    assert(topLevel("class C[+T]").show[Code] === "class C[+T]")
    assert(topLevel("class C[@foo +T]").show[Code] === "class C[@foo +T]")
  }

  test("primary constructor mods") {
    assert(topLevel("class C").show[Code] === "class C")
    assert(topLevel("class C private").show[Code] === "class C private")
    assert(topLevel("class C @foo(x)").show[Code] === "class C @foo(x)")
    assert(topLevel("class C @foo(x) private").show[Code] === "class C @foo(x) private")
    assert(topLevel("class C(x: Int)").show[Code] === "class C(x: Int)")
    assert(topLevel("class C private (x: Int)").show[Code] === "class C private (x: Int)")
    assert(topLevel("class C @foo(x) (x: Int)").show[Code] === "class C @foo(x) (x: Int)")
    assert(topLevel("class C @foo(x) private (x: Int)").show[Code] === "class C @foo(x) private (x: Int)")
  }

  test("parentheses in patterns") {
    assert(templStat("x match { case (xs: List[Int]) :+ x => ??? }").show[Code] === """
      |x match {
      |  case (xs: List[Int]) :+ x =>
      |    ???
      |}
    """.trim.stripMargin)
  }

  test("List(x, y) :: z") {
    assert(templStat("List(x, y) :: z").show[Code] == "List(x, y) :: z")
    assert(templStat("x match { case List(x, y) :: z => ??? }").show[Code] === """
      |x match {
      |  case List(x, y) :: z =>
      |    ???
      |}
    """.trim.stripMargin)
  }

  test("secondary ctor") {
    assert(topLevel("class C(x: Int) { def this() { this(2); println(\"OBLIVION!!!\") } }").show[Code] === "class C(x: Int) { def this() { this(2); println(\"OBLIVION!!!\") } }")
  }

  test("case semicolons") {
    assert(templStat("x match { case y => foo1; foo2 }").show[Code] === """
      |x match {
      |  case y =>
      |    foo1
      |    foo2
      |}
    """.trim.stripMargin)
  }

  test("assorted literals") {
    assert(templStat("true").show[Code] === "true")
    assert(templStat("false").show[Code] === "false")
    assert(templStat("0").show[Code] === "0")
    assert(templStat("0l").show[Code] === "0l")
    assert(templStat("0f").show[Code] === "0.0f")
    assert(templStat("0d").show[Code] === "0.0d")
    assert(templStat("'0'").show[Code] === "'0'")
    assert(templStat("\"0\"").show[Code] === "\"0\"")
    assert(templStat("'zero").show[Code] === "'zero")
    assert(templStat("null").show[Code] === "null")
    assert(templStat("()").show[Code] === "()")
  }

  test("context and view bounds") {
    assert(templStat("class C[T: List, U <% Int]").show[Code] === "class C[T: List, U <% Int]")
    assert(templStat("def m[T: List, U <% Int] = ???").show[Code] === "def m[T: List, U <% Int] = ???")
  }

  test("some tricky parenthesization") {
    assert(templStat("if (1) 2 else 3 + 4").show[Code] === "if (1) 2 else 3 + 4")
    assert(templStat("(if (1) 2 else 3) + 4").show[Code] === "(if (1) 2 else 3) + 4")
    assert(templStat("if (1) 2 else 3 match { case _ => }").show[Code] === "if (1) 2 else 3 match {\n  case _ =>\n}")
    assert(templStat("(if (1) 2 else 3) match { case _ => }").show[Code] === "(if (1) 2 else 3) match {\n  case _ =>\n}")
    assert(templStat("unit.toCheck += (() => body)").show[Code] === "unit.toCheck += (() => body)")
    assert(templStat("({ foo1; foo2 }).orElse(bar)").show[Code] === "{\n  foo1\n  foo2\n}.orElse(bar)")
    assert(templStat("(foo match { case _ => }).orElse(bar)").show[Code] === "(foo match {\n  case _ =>\n}).orElse(bar)")
    assert(templStat("foo || (if (cond) bar else baz)").show[Code] === "foo || (if (cond) bar else baz)")
    assert(templStat("foo && (bar match { case _ => })").show[Code] === "foo && (bar match {\n  case _ =>\n})")
    assert(templStat("\"foo \" + (if (cond) bar else baz)").show[Code] === "\"foo \" + (if (cond) bar else baz)")
    assert(templStat("foo match { case bar @ (_: T1 | _: T2) => }").show[Code] === "foo match {\n  case bar @ (_: T1 | _: T2) =>\n}")
    assert(templStat("foo match { case A + B / C => }").show[Code] === "foo match {\n  case A + B / C =>\n}")
    assert(templStat("foo match { case (A + B) / C => }").show[Code] === "foo match {\n  case (A + B) / C =>\n}")
    assert(templStat("foo match { case A + (B / C) => }").show[Code] === "foo match {\n  case A + B / C =>\n}")
    assert(templStat("foo match { case bar :: Nil :: Nil => }").show[Code] === "foo match {\n  case bar :: Nil :: Nil =>\n}")
    assert(templStat("foo match { case (bar :: Nil) :: Nil => }").show[Code] === "foo match {\n  case (bar :: Nil) :: Nil =>\n}")
    assert(templStat("@(foo @foo) class Bar").show[Code] === "@(foo @foo) class Bar")
    assert(templStat("(foo: Foo): @foo").show[Code] === "(foo: Foo): @foo")
    assert(templStat("type T = A + B / C").show[Code] === "type T = A + B / C")
    assert(templStat("type T = (A + B) / C").show[Code] === "type T = A + B / C")
    assert(templStat("type T = A + (B / C)").show[Code] === "type T = A + (B / C)")
    assert(templStat("type T = A :: B :: C").show[Code] === "type T = A :: B :: C")
    assert(templStat("type T = (A :: B) :: C").show[Code] === "type T = (A :: B) :: C")
    assert(templStat("foo match { case _: A | _: B => }").show[Code] === "foo match {\n  case _: A | _: B =>\n}")
    assert(templStat("foo match { case _: A | _: B | _: C => }").show[Code] === "foo match {\n  case _: A | _: B | _: C =>\n}")
  }

  test("more trickiness") {
    assert(templStat("def foo(bar_ : Int) = ???").show[Code] === "def foo(bar_ : Int) = ???")
    assert(templStat("class C[T_ : Foo]").show[Code] === "class C[T_ : Foo]")
    assert(templStat("val scala_ : NameType = ???").show[Code] === "val scala_ : NameType = ???")
  }

  test("class C extends (() => Int)") {
    assert(templStat("class C extends (() => Int)").show[Code] === "class C extends (() => Int)")
  }

  test("class C(x: Int)(implicit y: String, z: Boolean)") {
    assert(templStat("class C(x: Int)(implicit y: String, z: Boolean)").show[Code] === "class C(x: Int)(implicit y: String, z: Boolean)")
  }
}