package stepX

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait Component {
  def derive: Component

  def toTree(implicit c: blackbox.Context): c.Tree
}

case class Addition(a: Component, b: Component) extends Component {
  override def derive: Component = Addition(a.derive, b.derive)

  override def toTree(implicit c: blackbox.Context): c.Tree = {
    import c.universe._

    q"${a.toTree} + ${b.toTree}"
  }
}

case class Variable(s: String) extends Component {
  override def derive: Component = DoubleConstant(1)

  override def toTree(implicit c: blackbox.Context): c.Tree = {
    import c.universe._
    Ident(TermName(s))
  }
}

case class DoubleConstant(a: Double) extends Component {
  override def derive: Component = DoubleConstant(0)

  override def toTree(implicit c: blackbox.Context): c.Tree = {
    import c.universe._
    Literal(Constant(a))
  }
}

case class Multiplication(a: Component, b: Component) extends Component {
  override def derive: Component = {
    Addition(
      Multiplication(a, b.derive),
      Multiplication(b, a.derive)
    )
  }

  override def toTree(implicit c: blackbox.Context): c.Tree = {
    import c.universe._
    q"${a.toTree} * ${b.toTree}"
  }
}

case class Negate(a: Component) extends Component {
  override def derive: Component = Negate(a.derive)

  override def toTree(implicit c: blackbox.Context): c.Tree = {
    import c.universe._

    q"-${a.toTree}"
  }
}

case class Power(a: Component, b: Component) extends Component {
  override def derive: Component = this match {
    case Power(Variable(_), DoubleConstant(bb)) if bb != 0 =>
      Multiplication(
        b, Power(a, DoubleConstant(bb - 1))
      )
  }

  override def toTree(implicit c: blackbox.Context): c.Tree = {
    import c.universe._
    q"scala.math.`package`.pow(${a.toTree}, ${b.toTree})"
  }
}

object Macros {
  /**
    * derive(x => -2 * (math.pow(x, 2) + 4 * x - 5)) == -4 * x - 8
    **/
  def derive(f: Double => Double): Double => Double = macro deriveImpl

  def deriveImpl(c: blackbox.Context)(f: c.Expr[Double => Double]): c.Expr[Double => Double] = {
    import c.universe._

    val q"($arg) => $body" = f.tree

    val q"$_ val $argName: $_" = arg

    def extractComponent(tree: Tree): Component = tree match {
      case Ident(TermName(name)) => Variable(name)
      case Literal(Constant(a)) => DoubleConstant(a.toString.toDouble)
      case q"+$a" => extractComponent(a)
      case q"-$a" => Negate(extractComponent(a))
      case q"$a + $b" => Addition(extractComponent(a), extractComponent(b))
      case q"$a - $b" => Addition(extractComponent(a), Negate(extractComponent(b)))
      case q"$a * $b" => Multiplication(extractComponent(a), extractComponent(b))
      case q"scala.math.`package`.pow($a, $b)" => Power(extractComponent(a), extractComponent(b))
    }

    val result = extractComponent(body).derive.toTree(c)

    println(s"The new function is ${showCode(result)}")

    c.Expr(q"($argName: Double) => $result")
  }
}