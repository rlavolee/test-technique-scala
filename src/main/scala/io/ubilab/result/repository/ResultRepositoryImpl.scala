package io.ubilab.result.repository

import io.ubilab.result.model.{Result, ResultId}

abstract class ResultRepositoryImpl {

  def getAll: Iterable[Result]

  def get(id: ResultId): Option[Result]

  def add(id: ResultId, result: Result): Boolean

  def update(id: ResultId, result: Result): Unit

}
