package io.ubilab.result.service

import io.ubilab.result.model.{Result, ResultId}
import io.ubilab.result.repository.ResultRepositoryImpl

final class ResultService (
  resultRepository: ResultRepositoryImpl
) {
  private def updateSeen(id: ResultId, isSeen: Boolean): Unit =
    resultRepository.get(id).foreach{ result =>
      resultRepository.update(id, result.copy(isSeen = isSeen))
    }

  def addResult(result: Result): Boolean =
    resultRepository.add(result.id, result)

  def seenAllResult(): Unit =
    getAllResult.foreach(result => seenResult(result.id))

  def seenResult(id: ResultId): Unit =
    updateSeen(id, isSeen = true)

  def unseenResult(id: ResultId): Unit =
    updateSeen(id, isSeen = false)

  def getAllResult: List[Result] =
    resultRepository.getAll.toList

  def getAllResultSeen: List[Result] =
    getAllResult.filter(_.isSeen)

  def numberOfEventSeen: Int =  ???
}

object ResultService {

  def build(resultRepository: ResultRepositoryImpl): ResultService =
    new ResultService(resultRepository)
}