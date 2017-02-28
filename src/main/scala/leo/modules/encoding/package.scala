package leo.modules

/**
  * This package contains all algorithms related to first-order encoding
  * of higher-order problems. In particular
  *
  *  - [[leo.modules.encoding.Encoding]] is a facade for the most common use cases,
  *  - [[leo.modules.encoding.Monotonicity]] captures monotonicity inference (for type erasure)
  *  - [[leo.modules.encoding.TypedFOLEncoding]] encodes higher-order problems into polymorphic first-order logic
  *  - [[leo.modules.encoding.UntypedFOLEncoding]] encodes higher-order problems into untyped first-order logic
  *      or polymorphic first-order logic problems into untyped first-order logic.
  *  - [[leo.modules.encoding.Monomorphization]] contains monomorphization of polymorphic problems
  *
  * Further classes capture specalized aspects of the above operations, e.g. parameter to them.
  *
  * @since February 2017
  */
package object encoding
