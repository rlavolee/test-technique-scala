package io.ubilab.result.repository.inmemory

import io.ubilab.result.model.{Result, ResultId}
import io.ubilab.result.repository.ResultRepositoryImpl

final class ResultRepository extends ResultRepositoryImpl {

  private var results: Map[ResultId, Result] = Map.empty

  def getAll: Iterable[Result] = results.values

  def get(id: ResultId): Option[Result] = results.get(id)

  def add(id: ResultId, result: Result): Boolean =
    results.get(id) match {
      case None =>
        results = results + (id -> result)
        true
      case Some(_) => false
    }

  def update(id: ResultId, result: Result): Unit =
    results = results.updated(id, result)

}
