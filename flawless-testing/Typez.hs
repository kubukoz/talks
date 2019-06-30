type Boolean    = True | False
type Option a   = Some a | None
type Either a b = Left a | Right b
type List a     = Nil | Cons a (List a)

type Book = (Id, String)
type Person = (String, List[Person])

type Bit = Off | On
type Byte = (Bit, Bit, Bit, Bit, Bit, Bit, Bit, Bit)
type Char = Byte

type String = List[Char]

Either[A, Option[B]] = Either[Option[A], B]
(A, Option[B]) = Either[(A, B), A]
Either[A, Either[B, C]] = Either[Either[A, B], C]
Either[Option[A], Option[B]] = Either[A, Option[Option[B]]]
