package foo

import cats.{Monad, Applicative}
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
  def readClosure[F[_] : Monad](repo: FooRepository[F], ids: List[FooId]): F[Set[Foo]] = {

    def loop(existingItems: Set[Foo]): F[Either[Set[Foo], Set[Foo]]] =
      existingItems
        .flatMap(_.references)
        .removedAll(existingItems.map(_.id)) // prevent from fetching existing items again
        .toList match {
        case List() => Applicative[F].pure(Right(existingItems)) // prevent from passing an empty list of ids
        case ids =>
          repo
            .read(ids)
            .flatMap(newItems =>
              if (newItems.isEmpty) Applicative[F].pure(Right(existingItems)) // exit tailrec
              else Applicative[F].pure(Left(existingItems ++ newItems))) // continue tailrec
      }

    repo
      .read(ids)
      .flatMap(Monad[F].tailRecM(_)(loop))
  }
}