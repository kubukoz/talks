type Pipe[A] = List[A] => List[A]

<span class="fragment">//type Endo[A] = A => A</span>
<span class="fragment">//type Pipe[A] = Endo[List[a]]<span class="fragment"> = List[a] => List[a]</span></span>
<span class="fragment">//type Pipe = Endo ∘ List</span>
<span class="fragment">implicit val pipeSemK: SemigroupK[Pipe] =
  SemigroupK[Endo].compose[List]</span>
<span class="fragment">
@typeclass trait SemigroupK[F[_]] {
  /**
    * Combine two F[A] values.
    */
  @simulacrum.op("<+>", alias = true)
  def combineK[A](x: F[A], y: F[A]): F[A]
}</span>
