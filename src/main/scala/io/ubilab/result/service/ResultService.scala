package io.ubilab.result.service

import io.ubilab.result.model.{Result, ResultId}
import io.ubilab.result.repository.ResultRepositoryImpl

final class ResultService (
  resultRepository: ResultRepositoryImpl
) {
  def addResult(result: Result) = ???

  def seenResult(id: ResultId): Unit = ???

  def unseenResult(id: ResultId): Unit = ???

  def getAllResult: List[Result] =
    resultRepository.getAll.toList

  def getAllResultSeen: List[Result] = ???

  def getAllResultUnSeen: List[Result] = ???

  def numberOfEventSeen: Int =  ???
}

object ResultService {

  def build(resultRepository: ResultRepositoryImpl): ResultService =
    new ResultService(resultRepository)
}