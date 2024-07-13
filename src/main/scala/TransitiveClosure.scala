package foo

import cats.{Applicative, Monad}
import cats.implicits._

final case class FooId(value: Int) extends AnyVal

final case class Foo(
    id: FooId,
    references: List[FooId],
    data: String
)

trait FooRepository[F[_]] {
  def read(ids: List[FooId]): F[Set[Foo]]
}

object TransitiveClosure extends App {
  def readClosure[F[_]: Monad](repo: FooRepository[F], ids: List[FooId]): F[Set[Foo]] = {
    type LoopState = (Set[FooId], Set[FooId], Set[Foo])

    def loop(loopState: LoopState): F[Either[LoopState, Set[Foo]]] = {
      val (alreadyFetchedIds, idsToFetch, allItems) = loopState
      repo
        .read(idsToFetch.toList)
        .flatMap(newItems => {
          val idsToSkip = alreadyFetchedIds ++ newItems.map(_.id)
          // prevent from fetching existing items again
          val remainingRefs = newItems.flatMap(_.references).removedAll(idsToSkip)
          val updatedItems  = allItems ++ newItems
          // exit recursion in 2 cases:
          // a) items returned, no refs left
          // b) no items returned (which is an illegal state, but anyways...)
          if (remainingRefs.isEmpty) Applicative[F].pure(Right(updatedItems))
          // continue recursion
          else Applicative[F].pure(Left((idsToSkip, remainingRefs, updatedItems)))
        })
    }

    repo
      .read(ids)
      .flatMap(results => {
        val idsToSkip     = results.map(_.id)
        val notFetchedIds = results.flatMap(_.references).removedAll(idsToSkip)
        Monad[F].tailRecM((idsToSkip, notFetchedIds, results))(loop)
      })
  }

}
