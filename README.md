# Transitive Closure

Transitive closure assessment. Scala, Cats, ScalaTest

## Objective

A database contains `Foo` items, and all their `references` do exist in db. One can use `FooRepository` interface to interact with db.

The task is to implement a `readClosure` function, which accepts a list of `FooId` and returns them and all objects which they refer to (directly or indirectly). The function should be tested with mock implementations of `FooRepository` (which should also be developed).
 

## Technical decisions

### 1. Tail recursive monadic function is used

This allows large relation graphs

### 2. Cyclic relations are allowed

Items can refer to each other in any order

### 3. Each item will be requested exactly once

This was not required by the objective. However, since all elements are merged in a single Set, there will ever be no need in fetching some element twice, that's why this restriction was added (and tested). Also this makes sense in case of ring-like relations.

## run it

1. install [sbt](https://www.scala-sbt.org/)
2. run tests with `sbt test`
