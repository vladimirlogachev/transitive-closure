import foo._

import cats.data.Writer
import cats.implicits._
import org.scalatest._
import flatspec._
import matchers._

object FooTestingTools {

  type Log = Vector[List[Int]]

  type WriterLog[A] = Writer[Log, A]

  case class WriterFooRepo(existingItems: List[Foo]) extends FooRepository[WriterLog] {
    def read(ids: List[FooId]): WriterLog[Set[Foo]] =
      Writer(
        Vector(ids.map(_.value)),
        ids
          .flatMap(id => existingItems.find(_.id == id))
          .toSet
      )

  }

  // generate list of items with 1-based ids.
  // Each of them is linked to the next one, but the last one is not linked
  def generateFooChain(count: Int): List[Foo] =
    (1 to count)
      .map(n => Foo(FooId(n), if (n == count) List.empty else List(FooId(n + 1)), s"data $n"))
      .toList

  def eachRequestedExactlyOnce(log: Log): Boolean = {
    val sorted = log.flatten.sorted
    sorted == sorted.distinct
  }

  case class OptionFooRepo(existingItems: List[Foo]) extends FooRepository[Option] {
    def read(ids: List[FooId]): Option[Set[Foo]] =
      ids
        .map(id => existingItems.find(_.id == id))
        .sequence
        .map(_.toSet)

  }

}

class TransitiveClosureSpec extends AnyFlatSpec with should.Matchers {

  import FooTestingTools._

  "short chain of elements" should "be traversed as expected" in {
    val shortChain = generateFooChain(3)

    val (log, result) = TransitiveClosure.readClosure(WriterFooRepo(shortChain), List(FooId(1))).run

    log shouldBe Vector(List(1), List(2), List(3))
    eachRequestedExactlyOnce(log) shouldBe true
    result.map(_.id.value).toList.sorted shouldBe List(1, 2, 3)
  }

  "tree-like structure" should "be traversed as expected" in {
    val treeItems = List(
      Foo(FooId(1), List(FooId(2), FooId(4)), "data 1"),
      Foo(FooId(2), List(FooId(3)), "data 2"),
      Foo(FooId(3), List.empty, "data 3"),
      Foo(FooId(4), List(FooId(5)), "data 4"),
      Foo(FooId(5), List.empty, "data 5"),
      Foo(FooId(6), List.empty, "data 6 never used")
    )

    val (log, result) = TransitiveClosure.readClosure(WriterFooRepo(treeItems), List(FooId(1))).run

    log shouldBe Vector(List(1), List(2, 4), List(3, 5))
    eachRequestedExactlyOnce(log) shouldBe true
    result.map(_.id.value).toList.sorted shouldBe List(1, 2, 3, 4, 5)
  }

  "diamond-like structure" should "be traversed as expected" in {
    val diamondItems = List(
      Foo(FooId(1), List(FooId(2), FooId(3), FooId(4)), "data 1"),
      Foo(FooId(2), List(FooId(5)), "data 2"),
      Foo(FooId(3), List(FooId(5)), "data 3"),
      Foo(FooId(4), List(FooId(5)), "data 4"),
      Foo(FooId(5), List.empty, "data 5"),
      Foo(FooId(6), List.empty, "data 6 never used")
    )

    val (log, result) = TransitiveClosure.readClosure(WriterFooRepo(diamondItems), List(FooId(1))).run

    log shouldBe Vector(List(1), List(2, 3, 4), List(5))
    eachRequestedExactlyOnce(log) shouldBe true
    result.map(_.id.value).toList.sorted shouldBe List(1, 2, 3, 4, 5)
  }

  "circle-like structure" should "be traversed as expected" in {
    val circleItems = List(
      Foo(FooId(1), List(FooId(2)), "data 1"),
      Foo(FooId(2), List(FooId(3)), "data 2"),
      Foo(FooId(3), List(FooId(4)), "data 3"),
      Foo(FooId(4), List(FooId(1)), "data 4 cycled to 1"),
      Foo(FooId(5), List.empty, "data 5 never used")
    )

    val (log, result) = TransitiveClosure.readClosure(WriterFooRepo(circleItems), List(FooId(1))).run

    log shouldBe Vector(List(1), List(2), List(3), List(4))
    eachRequestedExactlyOnce(log) shouldBe true
    result.map(_.id.value).toList.sorted shouldBe List(1, 2, 3, 4)
  }

  "long chain of elements" should "should be traversed without a stack overflow" in {
    val longChain = generateFooChain(3000)

    val (_, result) = TransitiveClosure.readClosure(WriterFooRepo(longChain), List(FooId(1))).run

    result.size shouldBe 3000
  }

  "short chain of elements" should "just play well with Option monad" in {
    val shortChain = generateFooChain(3)

    val resultOption = TransitiveClosure.readClosure(OptionFooRepo(shortChain), List(FooId(1)))

    resultOption.map(_.map(_.id.value).toList.sorted) shouldBe Some(List(1, 2, 3))
  }

  "short chain of elements" should "fail if an Option monad fails" in {
    // what this test covers is not an inconsistency case (which is not required by the objective)
    // but instead it ensures the correct usage of flatMap, which should always return None if there ever was one.
    val inconsistentItems = List(Foo(FooId(1), List(FooId(2)), "data 1"))

    val resultOption = TransitiveClosure.readClosure(OptionFooRepo(inconsistentItems), List(FooId(1)))

    resultOption.map(_.map(_.id.value).toList.sorted) shouldBe None
  }

}
