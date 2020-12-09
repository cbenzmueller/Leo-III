package leo.datastructures

object TPTPAST {
  type Include = (String, Seq[String])
  type Annotations = Option[(GeneralTerm, Option[Seq[GeneralTerm]])]

  final case class Problem(includes: Seq[Include], formulas: Seq[AnnotatedFormula]) extends Pretty {
    override def pretty: String = {
      val sb: StringBuilder = new StringBuilder()
      includes.foreach { case (filename, inc) =>
        if (inc.isEmpty) {
          sb.append(s"include('$filename').\n")
        } else {
          sb.append(s"include('$filename', [${inc.map(s => s"'$s'").mkString(",")}]).\n")
        }
      }
      formulas.foreach { f =>
        sb.append(f.pretty)
        sb.append("\n")
      }
      if (sb.nonEmpty) sb.init.toString()
      else sb.toString()
    }
  }

//  sealed abstract class AnnotatedFormula[+A](val name: String, val role: String, val formula: A, val annotations: Annotations)
  sealed abstract class AnnotatedFormula extends Pretty {
    type F
    def name: String
    def role: String
    def formula: F
    def annotations: Annotations
  }
  final case class THFAnnotated(override val name: String,
                          override val role: String,
                          override val formula: THF.Formula,
                          override val annotations: Annotations) extends AnnotatedFormula {
    type F = THF.Formula

    override def pretty: String = prettifyAnnotated("thf", name, role, formula, annotations)
//      if (annotations.isEmpty) s"thf($name, $role, ${formula.pretty})."
//    else {
//      if (annotations.get._2.isEmpty) s"thf($name, $role, ${formula.pretty}, ${annotations.get._1.pretty})."
//      else s"thf($name, $role, ${formula.pretty}, ${annotations.get._1.pretty}, [${annotations.get._2.get.map(_.pretty).mkString(",")}])."
//    }
  }

  final case class TFFAnnotated(override val name: String,
                          override val role: String,
                          override val formula: TFF.Formula,
                          override val annotations: Annotations) extends AnnotatedFormula {
    type F = TFF.Formula

    override def pretty: String = prettifyAnnotated("tff", name, role, formula, annotations)
  }

  final case class FOFAnnotated(override val name: String,
                          override val role: String,
                          override val formula: FOF.Formula,
                          override val annotations: Annotations) extends AnnotatedFormula {
    type F = FOF.Formula

    override def pretty: String = prettifyAnnotated("fof", name, role, formula, annotations)
  }

  final case class TCFAnnotated(override val name: String,
                          override val role: String,
                          override val formula: TCF.Formula,
                          override val annotations: Annotations) extends AnnotatedFormula {
    type F = TCF.Formula

    override def pretty: String = prettifyAnnotated("tcf", name, role, formula, annotations)
  }

  final case class CNFAnnotated(override val name: String,
                          override val role: String,
                          override val formula: CNF.Formula,
                          override val annotations: Annotations) extends AnnotatedFormula {
    type F = CNF.Formula

    override def pretty: String = prettifyAnnotated("cnf", name, role, formula, annotations)
  }

  final case class TPIAnnotated(override val name: String,
                          override val role: String,
                          override val formula: TPI.Formula,
                          override val annotations: Annotations) extends AnnotatedFormula {
    type F = TPI.Formula

    override def pretty: String = prettifyAnnotated("tpi", name, role, formula, annotations)
  }

  @inline private[this] final def prettifyAnnotated(prefix: String, name: String, role: String, formula: Pretty, annotations: Annotations): String = {
    if (annotations.isEmpty) s"$prefix($name, $role, ${formula.pretty})."
    else {
      if (annotations.get._2.isEmpty) s"$prefix($name, $role, ${formula.pretty}, ${annotations.get._1.pretty})."
      else s"$prefix($name, $role, ${formula.pretty}, ${annotations.get._1.pretty}, [${annotations.get._2.get.map(_.pretty).mkString(",")}])."
    }
  }

  sealed abstract class Number extends Pretty
  final case class Integer(value: Int) extends Number {
    override def pretty: String = value.toString
  }
  final case class Rational(numerator: Int, denominator: Int) extends Number {
    override def pretty: String = s"$numerator/$denominator"
  }
  final case class Real(wholePart: Int, decimalPlaces: Int, exponent: Int) extends Number {
    override def pretty: String = if (exponent == 1) s"$wholePart.$decimalPlaces"
                                  else s"$wholePart.${decimalPlaces}E$exponent"
  }

  final case class GeneralTerm(data: Seq[GeneralData], list: Option[Seq[GeneralTerm]]) extends Pretty {
    override def pretty: String = {
      val sb: StringBuilder = new StringBuilder()
      if (data.nonEmpty) {
        sb.append(data.map(_.pretty).mkString(":"))
      }
      if (list.isDefined) {
        if(data.nonEmpty) sb.append(":")
        sb.append("[")
        sb.append(list.get.map(_.pretty).mkString(","))
        sb.append("]")
      }
      sb.toString()
    }
  }

  /** General formula annotation data. Can be one of the following:
    *   - [[MetaFunctionData]], a term-like meta expression: either a (meta-)function or a (meta-)constant.
    *   - [[MetaVariable]], a term-like meta expression that captures a variable.
    *   - [[NumberData]], a numerical value.
    *   - [[DistinctObjectData]], an expression that represents itself.
    *   - [[GeneralFormulaData]], an expression that contains object-level formula expressions.
    *
    *   @see See [[GeneralTerm]] for some context and
    *        [[http://tptp.org/TPTP/SyntaxBNF.html#general_term]] for a use case.
    */
  sealed abstract class GeneralData extends Pretty
  /** @see [[GeneralData]] */
  final case class MetaFunctionData(f: String, args: Seq[GeneralTerm]) extends GeneralData {
    override def pretty: String = {
      val escapedF = escapeAtomicWord(f)
      if (args.isEmpty) escapedF else s"$escapedF(${args.map(_.pretty).mkString(",")})"
    }
  }
  /** @see [[GeneralData]] */
  final case class MetaVariable(variable: String) extends GeneralData {
    override def pretty: String = variable
  }
  /** @see [[GeneralData]] */
  final case class NumberData(number: Number) extends GeneralData {
    override def pretty: String = number.pretty
  }
  /** @see [[GeneralData]] */
  final case class DistinctObjectData(name: String) extends GeneralData {
    override def pretty: String = {
      assert(name.startsWith("\"") && name.endsWith("\""), "Distinct object without enclosing double quotes.")
      s""""${escapeDistinctObject(name.tail.init)}""""
    }
  }
  /** @see [[GeneralData]] */
  final case class GeneralFormulaData(data: FormulaData) extends GeneralData {
    override def pretty: String = data.pretty
  }

  sealed abstract class FormulaData extends Pretty
  final case class THFData(formula: THF.Formula) extends FormulaData {
    override def pretty: String = s"$$thf(${formula.pretty})"
  }
  final case class TFFData(formula: TFF.Formula) extends FormulaData {
    override def pretty: String = s"$$tff(${formula.pretty})"
  }
  final case class FOFData(formula: FOF.Formula) extends FormulaData {
    override def pretty: String = s"$$fof(${formula.pretty})"
  }
  final case class CNFData(formula: CNF.Formula) extends FormulaData {
    override def pretty: String = s"$$cnf(${formula.pretty})"
  }
  final case class FOTData(formula: FOF.Term) extends FormulaData {
    override def pretty: String = s"$$fot(${formula.pretty})"
  }

  private def escapeAtomicWord(word: String): String = {
    val simpleLowerWordRegex = "^[a-z][a-zA-Z\\d_]*$"
    if (word.matches(simpleLowerWordRegex)) word
    else s"'${word.replace("\\","\\\\").replace("'", "\\'")}'"
  }
  private def escapeDistinctObject(name: String): String = {
    name.replace("\\","\\\\").replace("\"", "\\\"")
  }


  object THF {
    type TypedVariable = (String, Type)
    type Type = Term

    sealed abstract class Formula extends Pretty
    final case class Typing(atom: String, typ: Type) extends Formula {
      override def pretty: String = {
        val escapedName = if (atom.startsWith("$") || atom.startsWith("$$")) atom else escapeAtomicWord(atom)
        s"$escapedName: ${typ.pretty}"
      }
    }
    final case class Logical(term: Term) extends Formula {
      override def pretty: String = term.pretty
    }

    // Types as terms; for TH1 parsing. That's why we dont have a clean separation between terms and types here.
    // We don't care for well-typedness etc. in parsing. We can parse syntactically correct but completely meaningless
    // and ill-typed inputs. This will be addressed in the interpretation step.
    sealed abstract class Term extends Pretty
    /** Might be function or just constant. Also distinct object? */
    final case class FunctionTerm(f: String, args: Seq[Term]) extends Term  {
      override def pretty: String = {
        val escapedF = if (f.startsWith("$") || f.startsWith("$$")) f else escapeAtomicWord(f)
        if (args.isEmpty) escapedF else s"$escapedF(${args.map(_.pretty).mkString(",")})"
      }

      @inline def isUninterpretedFunction: Boolean = !isDefinedFunction && !isSystemFunction
      @inline def isDefinedFunction: Boolean = f.startsWith("$") && !isSystemFunction
      @inline def isSystemFunction: Boolean = f.startsWith("$$")
      @inline def isConstant: Boolean = args.isEmpty
    }
    final case class QuantifiedFormula(quantifier: Quantifier, variableList: Seq[TypedVariable], body: Term) extends Term {
      override def pretty: String = s"(${quantifier.pretty} [${variableList.map{case (n,t) => s"$n:${t.pretty}"}.mkString(",")}]: (${body.pretty}))"
    }
    final case class Variable(name: String) extends Term {
      override def pretty: String = name
    }
    final case class UnaryFormula(connective: UnaryConnective, body: Term) extends Term {
      override def pretty: String = s"${connective.pretty} (${body.pretty})"
    }
    final case class BinaryFormula(connective: BinaryConnective, left: Term, right: Term) extends Term {
      override def pretty: String = s"(${left.pretty} ${connective.pretty} ${right.pretty})"
    }
    final case class Tuple(elements: Seq[Term]) extends Term {
      override def pretty: String = s"[${elements.map(_.pretty).mkString(",")}]"
    }
    final case class ConditionalTerm(condition: Term, thn: Term, els: Term) extends Term {
      override def pretty: String = s"$$ite(${condition.pretty}, ${thn.pretty}, ${els.pretty})"
    }
    final case class LetTerm(typing: Map[String, Type], binding: Map[Term, Term], body: Term) extends Term {
      override def pretty: String = s"$$let(...,${body.pretty})" // TODO
    }
    final case class ConnectiveTerm(conn: Connective) extends Term {
      override def pretty: String = s"(${conn.pretty})"
    }
    final case class DistinctObject(name: String) extends Term {
      override def pretty: String = {
        assert(name.startsWith("\"") && name.endsWith("\""), "Distinct object without enclosing double quotes.")
        s""""${escapeDistinctObject(name.tail.init)}""""
      }
    }
    final case class NumberTerm(value: Number) extends Term {
      override def pretty: String = value.pretty
    }

    sealed abstract class Connective extends Pretty
    sealed abstract class UnaryConnective extends Connective
    final case object ~ extends UnaryConnective { override def pretty: String = "~" }
    final case object !! extends UnaryConnective { override def pretty: String = "!!" }
    final case object ?? extends UnaryConnective { override def pretty: String = "??" }
    final case object @@+ extends UnaryConnective { override def pretty: String = "@@+" } // Choice
    final case object @@- extends UnaryConnective { override def pretty: String = "@@-" } // Description
    final case object @@= extends UnaryConnective { override def pretty: String = "@@=" } // Prefix equality

    sealed abstract class BinaryConnective extends Connective
    final case object Eq extends BinaryConnective { override def pretty: String = "=" }
    final case object Neq extends BinaryConnective { override def pretty: String = "!=" }
    // non-assoc
    final case object <=> extends BinaryConnective { override def pretty: String = "<=>" }
    final case object Impl extends BinaryConnective { override def pretty: String = "=>" }
    final case object <= extends BinaryConnective { override def pretty: String = "<=" }
    final case object <~> extends BinaryConnective { override def pretty: String = "<~>" }
    final case object ~| extends BinaryConnective { override def pretty: String = "~|" }
    final case object ~& extends BinaryConnective { override def pretty: String = "~&" }
    // assoc
    final case object | extends BinaryConnective { override def pretty: String = "|" }
    final case object & extends BinaryConnective { override def pretty: String = "&" }
    final case object App extends BinaryConnective { override def pretty: String = "@" } // left-assoc
    // term-as-type
    final case object FunTyConstructor extends BinaryConnective { override def pretty: String = ">" }
    final case object ProductTyConstructor extends BinaryConnective { override def pretty: String = "*" }
    final case object SumTyConstructor extends BinaryConnective { override def pretty: String = "+" }

    sealed abstract class Quantifier extends Pretty
    final case object ! extends Quantifier { override def pretty: String = "!" } // All
    final case object ? extends Quantifier { override def pretty: String = "?" } // Exists
    final case object ^ extends Quantifier { override def pretty: String = "^" } // Lambda
    final case object @+ extends Quantifier { override def pretty: String = "@+" } // Choice
    final case object @- extends Quantifier { override def pretty: String = "@-" } // Description
    final case object !> extends Quantifier { override def pretty: String = "!>" } // Big pi
    final case object ?* extends Quantifier { override def pretty: String = "?*" } // Big sigma
  }

  object TFF {
    type TypedVariable = (String, Type)
    type Type = Term

    sealed abstract class Formula extends Pretty
    final case class Typing(atom: String, typ: Type) extends Formula {
      override def pretty: String = {
        val escapedName = if (atom.startsWith("$") || atom.startsWith("$$")) atom else escapeAtomicWord(atom)
        s"$escapedName: ${typ.pretty}"
      }
    }
    final case class Logical(term: Term) extends Formula {
      override def pretty: String = term.pretty
    }

    sealed abstract class Term extends Pretty


    sealed abstract class Connective extends Pretty
    sealed abstract class UnaryConnective extends Connective
    final case object ~ extends UnaryConnective { override def pretty: String = "~" }

    sealed abstract class BinaryConnective extends Connective
    final case object Eq extends BinaryConnective { override def pretty: String = "=" }
    final case object Neq extends BinaryConnective { override def pretty: String = "!=" }
    // non-assoc
    final case object <=> extends BinaryConnective { override def pretty: String = "<=>" }
    final case object Impl extends BinaryConnective { override def pretty: String = "=>" }
    final case object <= extends BinaryConnective { override def pretty: String = "<=" }
    final case object <~> extends BinaryConnective { override def pretty: String = "<~>" }
    final case object ~| extends BinaryConnective { override def pretty: String = "~|" }
    final case object ~& extends BinaryConnective { override def pretty: String = "~&" }
    // assoc
    final case object | extends BinaryConnective { override def pretty: String = "|" }
    final case object & extends BinaryConnective { override def pretty: String = "&" }

    sealed abstract class Quantifier extends Pretty
    final case object ! extends Quantifier { override def pretty: String = "!" } // All
    final case object ? extends Quantifier { override def pretty: String = "?" } // Exists

    // term-as-type
    final case object FunTyConstructor extends BinaryConnective { override def pretty: String = ">" }
    final case object ProductTyConstructor extends BinaryConnective { override def pretty: String = "*" }
    final case object SumTyConstructor extends BinaryConnective { override def pretty: String = "+" }
  }

  object FOF {
    sealed abstract class Formula extends Pretty

    abstract class Term extends Pretty
  }

  object TCF {
    sealed abstract class Formula extends Pretty
  }

  object CNF {
    sealed abstract class Formula extends Pretty
  }

  object TPI {
    sealed abstract class Formula extends Pretty
  }
}
