package leo.modules.input

import leo.datastructures.{Kind, Role, Signature, Term, Type, TPTPAST => TPTP}
import leo.datastructures.Term.{mkApp, mkAtom, mkBound, mkTermApp, Λ, λ}
import leo.datastructures.Type.{mkFunType, mkProdType, mkType, mkUnionType, mkVarType, typeKind, ∀}
import leo.modules.output.{SZS_Inappropriate, SZS_InputError, SZS_SyntaxError, SZS_TypeError}
import leo.modules.SZSException
import leo.Out

object InputProcessingNew {
  import leo.modules.HOLSignature.{i,o, rat, int, real, LitTrue, IF_THEN_ELSE, HOLUnaryConnective, HOLBinaryConnective}

  type TypeOrKind = Either[Type, Kind]
  type TermOrType = Either[Term, Type]
  // (Formula name, Term, Formula Role)
  type Result = (String, Term, Role)

  @inline private[this] final def processRole(role: String): Role = Role(role)

  @inline final def processAll(sig: Signature)(statements: Seq[TPTP.AnnotatedFormula]): Seq[Result] = statements.map(process(sig))

  final def process(sig: Signature)(statement: TPTP.AnnotatedFormula): Result = {
    import TPTP.{THFAnnotated, TFFAnnotated, FOFAnnotated, CNFAnnotated, TPIAnnotated}
    import leo.datastructures.Role_Definition

    val name = statement.name
    val role = processRole(statement.role)
    val maybeFormula: Option[Term] = statement match {
      case f@THFAnnotated(_, _, _, _) => processAnnotatedTHF(sig)(f)
      case f@TFFAnnotated(_, _, _, _) => processAnnotatedTFF(sig)(f)
      case f@FOFAnnotated(_, _, _, _) => processAnnotatedFOF(sig)(f)
      case f@CNFAnnotated(_, _, _, _) => Some(processAnnotatedCNF(sig)(f))
      case f@TPIAnnotated(_, _, _, _) => throw new SZSException(SZS_Inappropriate, "TPI format not supported (yet).")
    }
    maybeFormula match {
      case None => (name, LitTrue, role)
      case Some(formula) =>
        if (role == Role_Definition) (name, formula, processRole("axiom")) // If original role was definition but formula
          // is returned; then it was not recognized as definition and is now added as axiom
        else (name, formula, role)
    }
  }

  private[this] final def processAnnotatedTHF(sig: Signature)(statement: TPTP.THFAnnotated): Option[Term] = {
    import TPTP.THF.{Logical, Typing}

    statement.formula match {
      case Typing(atom, typ) =>
        if (statement.role == "type") {
          convertTHFType(sig)(typ) match {
            case Left(ty0) => sig.addUninterpreted(atom, ty0)
            case Right(k) => sig.addTypeConstructor(atom, k)
          }
          None
        } else throw new SZSException(SZS_InputError, s"Formula ${statement.name} contains a type specification but role is not 'type'." +
          s"This is considered an error; please check your input.")

      case Logical(formula) if statement.role == "definition" =>
        processTHFDef(sig)(formula) match {
          case Some((name, definition)) =>
            if (sig.exists(name)) {
              val meta = sig(name)
              if (meta.isUninterpreted) {
                definition match {
                  case Left(def0) =>
                    if (meta._ty == def0.ty) sig.addDefinition(meta.key, def0.betaNormalize.etaExpand)
                    else throw new SZSException(SZS_InputError, s"Trying to provide definition for symbol '$name', but type of definition " +
                      s"(${def0.ty.pretty(sig)}) does not match type declaration of symbol (${meta._ty.pretty}).")
                  case Right(_) => throw new SZSException(SZS_InputError, s"Type definition in formula ${statement.name} is not supported, yet.")
                }
              } else throw new SZSException(SZS_InputError, s"Trying to provide a definition for interpreted/defined symbol '$name'.")
            } else {
              definition match {
                case Left(def0) => sig.addDefined(name, def0.betaNormalize.etaExpand, def0.ty)
                case Right(_) => throw new SZSException(SZS_InputError, s"Type definition in formula ${statement.name} is not supported, yet.")
              }
            }
            None
          case None =>
            val res = convertTHFFormula(sig)(formula)
            res match {
              case Left(axiom) =>
                Out.info(s"No direction of definition ${statement.name} detected. Treating as axiom ...")
                Some(axiom)
              case Right(_) => throw new SZSException(SZS_InputError, s"Definition ${statement.name} contains a type statement on top-level, where a formula was expected.")
            }
        }

      case Logical(formula) =>
        val res = convertTHFFormula(sig)(formula)
        res match {
          case Left(formula0) => Some(formula0)
          case Right(_) => throw new SZSException(SZS_InputError, s"${statement.name} contains a type statement on top-level, where a formula was expected.")
        }
    }
  }

  @inline private[this] final def processTHFDef(sig: Signature)(formula: TPTP.THF.Formula): Option[(String, TermOrType)] = {
    import TPTP.THF.{BinaryFormula, Eq, FunctionTerm}

    formula match {
      case BinaryFormula(Eq, FunctionTerm(name, Seq()), right) =>
        val definition = convertTHFFormula(sig)(right)
        Some((name, definition))
      case _ => None
    }
  }

  private[this] final def convertTHFFormula(sig: Signature)(formula: TPTP.THF.Formula): TermOrType = {
    import TPTP.THF.{FunctionTerm, QuantifiedFormula, Variable, UnaryFormula, BinaryFormula,
      Tuple, ConditionalTerm, LetTerm, ConnectiveTerm, DistinctObject, NumberTerm}

    formula match {
      case FunctionTerm(f, args) => ???
      case QuantifiedFormula(quantifier, variableList, body) => ???
      case Variable(name) => ???
      case UnaryFormula(connective, body) => ???
      case BinaryFormula(connective, left, right) => ???
      case Tuple(elements) => ???
      case ConditionalTerm(condition, thn, els) => ???
      case LetTerm(typing, binding, body) => ???
      case ConnectiveTerm(conn) => ???
      case DistinctObject(name) => ???
      case NumberTerm(value) => ???
    }
  }

  private[this] final def convertTHFType(sig: Signature)(typ: TPTP.THF.Type): TypeOrKind = {
    import TPTP.THF.{FunctionTerm, QuantifiedFormula, Variable, UnaryFormula, BinaryFormula,
      Tuple, ConditionalTerm, LetTerm, ConnectiveTerm, DistinctObject, NumberTerm}

    typ match {
      case FunctionTerm(f, args) =>
        if (args.nonEmpty) throw new SZSException(SZS_TypeError, s"Malformed type expression: ${typ.toString}")
        else {
          if (f.startsWith("$$")) {
            if (sig.exists(f)) Left(mkType(sig(f).key))
            else throw new SZSException(SZS_InputError, s"Unknown system type/type operator $f")
          } else if (f.startsWith("$")) {
            if (f == "$tType") Right(typeKind)
            else if (sig.exists(f)) Left(mkType(sig(f).key))
            else throw new SZSException(SZS_Inappropriate, s"Unknown TPTP type/type operator $f")
          } else {
            if (sig.exists(f)) Left(mkType(sig(f).key))
            else throw new SZSException(SZS_InputError, s"Unknown type/type operator $f, please specify its kind before.")
          }
        }

      case QuantifiedFormula(quantifier, variableList, body) => ???

      case Variable(name) => Left(mkVarType(???))

      case BinaryFormula(connective, left, right) =>
        import TPTP.THF.{FunTyConstructor, ProductTyConstructor, SumTyConstructor, App}

        connective match {
          case FunTyConstructor | ProductTyConstructor | SumTyConstructor | App =>
            val convertedLeft = convertTHFType(sig)(left)
            val convertedRight = convertTHFType(sig)(right)
            (convertedLeft, convertedRight) match {
              case (Left(left0), Left(right0)) =>
                val result = (connective: @unchecked) match { // Exhaustiveness guaranteed by match-case condition a few lines above
                  case FunTyConstructor => mkFunType(left0, right0)
                  case ProductTyConstructor => mkProdType(left0, right0)
                  case SumTyConstructor => mkUnionType(left0, right0)
                  case App => left0.app(right0)
                }
                Left(result)
              case (Right(left0), Right(right0)) if connective == FunTyConstructor => Right(Kind.mkFunKind(left0, right0))
              case _ => throw new SZSException(SZS_TypeError, s"Binary type '${typ.pretty}' either contains both types and $$tType which is not allowed," +
                s"or contains $$tType outside the context of the function type constructor '>'.")
            }
          case _ => throw new SZSException(SZS_TypeError, s"Top-level binary type (one of '>', '*', '+', or '@') expected but term connective '${connective.pretty}' was given.")
        }

      case Tuple(elements) =>
        val convertedTys = elements.map(convertTHFType(sig))
        val (tys, kinds) = convertedTys.partitionMap(identity)
        if (kinds.isEmpty) {
          throw new SZSException(SZS_Inappropriate, "Leo-III currently does not support tuples.")
        } else throw new SZSException(SZS_TypeError, s"Tuple type '${typ.pretty}' contains kinds (such as $$tType) which is not allowed.")
        
      //      case LetTerm(typing, binding, body) => ??? // TODO: We could, in principle, allow let-terms in type expressions.
      //      case DistinctObject(name) => ??? // TODO: Can a distinct object serve as type identifier?
      case _ => throw new SZSException(SZS_InputError, s"Malformed type-expression: '${typ.pretty}'.")
    }
  }

  final def processAnnotatedTFF(sig: Signature)(statement: TPTP.TFFAnnotated): Option[Term] = ???

  final def processAnnotatedFOF(sig: Signature)(statement: TPTP.FOFAnnotated): Option[Term] = ???

  final def processAnnotatedCNF(sig: Signature)(statement: TPTP.CNFAnnotated): Term = {
    import TPTP.CNF.Logical

    statement.formula match {
      case Logical(f) => processCNF(f) // No other kind of CNF formula exists
    }
  }

  private[this] final def processCNF(cnfFormula: TPTP.CNF.Formula): Term = ???

}
