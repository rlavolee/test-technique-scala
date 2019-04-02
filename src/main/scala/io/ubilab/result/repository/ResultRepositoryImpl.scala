package io.ubilab.result.repository

import scala.concurrent.Future
import io.ubilab.result.model.{Result, ResultId}
import io.ubilab.result.repository.inmemory.ResultGuardian.RichResult

abstract class ResultRepositoryImpl {

  def getAll: Future[List[Result]]

  def getAllRichResult: Future[List[RichResult]]

  def get(id: ResultId): Future[Option[Result]]

  def add(id: ResultId, result: Result): Future[Boolean]

  def seen(id: ResultId): Future[Boolean]

  def unSeen(id: ResultId): Future[Boolean]

}
