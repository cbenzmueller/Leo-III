package datastructures.internal

import scala.collection.immutable.{HashSet, BitSet, IntMap, HashMap}

/**
 * Implementation of the Leo III signature table. When created with `Signature.createWithHOL`
 * it contains some predefined symbols (including types, fixed symbols and defined symbols).
 * For details on that predefined symbols, check [[datastructures.internal.HOLSignature]].
 *
 * @author Alexander Steen
 * @since 02.05.2014
 * @note  Updated on 05.05.2014 (Moved case classes from `IsSignature` to this class)
 */
abstract sealed class Signature extends IsSignature with HOLSignature {
  override type Key = Int

  protected var curConstKey = 1

  protected var keyMap: Map[String, Int] = new HashMap[String, Int]
  protected var metaMap: IntMap[Meta] = IntMap.empty

  protected var typeSet, fixedSet, definedSet, uiSet: BitSet = BitSet.empty

  ///////////////////////////////
  // Meta information
  ///////////////////////////////

  /** Case class for meta information for base types that are indexed in the signature */
  protected[internal] case class TypeMeta(identifier: String,
                                          index: Key,
                                          k:  Kind,
                                          typeRep: Option[Type]) extends Meta {
    def name = identifier
    def key = index
    def symType = BaseType
    /** Return type representation of the symbol */
    def ty: Option[Type] = typeRep
    def kind: Option[Kind] = Some(k)
    def defn: Option[Term] = None

    def hasType = true // Since we provide type representation
    def hasKind = true
    def hasDefn = false
  }

  /** Case class for meta information for uninterpreted symbols */
  protected[internal] case class UninterpretedMeta(identifier: String,
                                                   index: Key,
                                                   typ: Type) extends Meta {
    def name = identifier
    def key = index
    def symType = Uninterpreted
    def ty: Option[Type] = Some(typ)
    def kind: Option[Kind] = None
    def defn: Option[Term] = None

    def hasType = true
    def hasKind = false
    def hasDefn = false
  }

  /** Case class for meta information for defined symbols */
  protected[internal] case class DefinedMeta(identifier: String,
                                             index: Key,
                                             typ: Option[Type],
                                             definition: Term) extends Meta {
    def name = identifier
    def key = index
    def symType = Defined
    def ty: Option[Type] = typ
    def kind: Option[Kind] = None
    def defn: Option[Term] = Some(definition)

    def hasType = typ.isDefined
    def hasKind = false
    def hasDefn = true
  }

  /** Case class for meta information for fixed (interpreted) symbols */
  protected[internal] case class FixedMeta(identifier: String,
                                           index: Key,
                                           typ: Type) extends Meta {
    def name = identifier
    def key = index
    def symType = Fixed
    def ty: Option[Type] = Some(typ)
    def kind: Option[Kind] = None
    def defn: Option[Term] = None

    def hasType = true
    def hasKind = false
    def hasDefn = false
  }

  ///////////////////////////////
  // Maintenance methods for the signature
  ///////////////////////////////

  protected def addConstant0(identifier: String, typ: Option[TypeOrKind], defn: Option[Term]): Key = {
    if (keyMap.contains(identifier)) {
      throw new IllegalArgumentException("Identifier " + identifier + " is already present in signature.")
    }

    val key = curConstKey
    curConstKey += 1
    keyMap += ((identifier, key))

    defn match {
      case None => { // Uninterpreted or type
        typ match {
          case None => throw new IllegalArgumentException("Neither definition nor type was passed to addConstant0.")
          case Some(Right(k:Kind)) => { // Type
            true match {
              case k.isTypeKind => {
                val meta = TypeMeta(identifier, key, k, Some(Type.mkType(key)))
                metaMap += (key, meta)
              }
              /*case k.isFunKind => {
                  throw new IllegalArgumentException("Constructor types not yet supported")
//                val meta = TypeMeta(identifier, key, k, Type.mkConstructorType(identifier))
//                metaMap += (key, meta)
              }*/
              case _ => { // it is neither a base or funKind, then it's a super kind.
              val meta = TypeMeta(identifier, key, Type.superKind, None)
                metaMap += (key, meta)
              }
            }
            typeSet += key
          }
          case Some(Left(t:Type)) => { // Uninterpreted symbol
          val meta = UninterpretedMeta(identifier, key, t)
            metaMap += (key, meta)
            uiSet += key
          }
        }
      }

      case Some(fed) => { // Defined
        val Left(ty) = typ.get
        val meta = DefinedMeta(identifier, key, Some(ty), fed)
          metaMap += (key, meta)
          definedSet += key
        }
    }

    key
  }

  def remove(key: Key): Boolean = {
     metaMap.get(key) match {
       case None => false
       case Some(meta) => {
         val id = meta.name
         metaMap -= key
         keyMap -= id
         true
       }
     }
  }

  def exists(identifier: String): Boolean = {
    keyMap.get(identifier) match {
      case None => false
      case _    => true
    }
  }


  def symbolExists(identifier: String): Boolean = keyMap.contains(identifier)

  def symbolType(identifier: String): SymbolType = metaMap(keyMap(identifier)).symType
  def symbolType(identifier: Key): SymbolType = metaMap(identifier).symType

  def getMeta(identifier: Key): Meta = meta(identifier)

  /** Adds a symbol to the signature that is then marked as `Fixed` symbol type */
  protected def addFixed(identifier: String, typ: Type): Unit = {
    val key = curConstKey
    curConstKey += 1
    keyMap += ((identifier, key))

    val meta = FixedMeta(identifier, key, typ)
    metaMap += (key, meta)
    fixedSet += key
  }

  ///////////////////////////////
  // Utility methods for constant symbols
  ///////////////////////////////

  def meta(key: Key): Meta = metaMap(key)
  def meta(identifier: String): Meta = meta(keyMap(identifier))

  ///////////////////////////////
  // Dumping of indexed symbols
  ///////////////////////////////

  def allConstants: Set[Key] = uiSet | fixedSet | definedSet | typeSet
  def fixedSymbols: Set[Key] = fixedSet.toSet
  def definedSymbols: Set[Key] = definedSet.toSet
  def uninterpretedSymbols: Set[Key] = uiSet.toSet
  def baseTypes: Set[Key] = typeSet.toSet

  ////////////////////////////////
  // Hard wired fixed keys
  ////////////////////////////////
  lazy val oKey = 1
  lazy val iKey = 2
}


object Signature {
  private case object Nil extends Signature

  /** Create an empty signature */
  def empty: Signature = Signature.Nil
  protected val globalSignature = empty
  def get = globalSignature

  /** Enriches the given signature with predefined symbols as described by [[datastructures.internal.HOLSignature]] */
  def withHOL(sig: Signature): Unit = {
    for ((name, k) <- sig.types) {
      sig.addConstant0(name, Some(Right(k)), None)
    }

    for ((name, ty) <- sig.fixedConsts) {
      sig.addFixed(name, ty)
    }

    for ((name, fed, ty) <- sig.definedConsts) {
      sig.addConstant0(name, Some(Left(ty)), Some(fed))
    }
  }
}