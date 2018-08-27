package step1

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object SimpleMacros {
  def hello2(s: String): Unit = macro hello2Impl

  def hello2Impl(c: blackbox.Context)(s: c.Expr[String]): c.Expr[Unit] = {
    import c.universe._

    val manualASTExpr = c.Expr[Unit](
      Apply(
        Ident(TermName("println")),
        List(
          Apply(
            Select(
              Apply(
                Select(
                  Literal(
                    Constant("hello, ")
                  ),
                  TermName(
                    "$plus"
                  )
                ),
                List(
                  s.tree
                )
              ),
              TermName(
                "$plus"
              )
            ),
            List(
              Literal(Constant("!"))
            )
          )
        )
      )
    )

    val reifiedExpr = reify {
      println("Hello, " + s.splice + "!")
    }

    val quasiquotedExpr = c.Expr(q"""println("hello, " + ${s.tree} + "!")""")

    quasiquotedExpr
    //manualASTExpr
    //reifiedExpr
  }

  def getCode(a: Any): String = macro getCodeImpl

  def getCodeImpl(c: blackbox.Context)(a: c.Expr[Any]): c.Expr[String] = {
    import c.universe._
    val aCode = showCode(a.tree)

    c.Expr(q"$aCode")
  }
}

object NonMacros {
  def hello(s: String) = println(s"Hello, $s!")
}