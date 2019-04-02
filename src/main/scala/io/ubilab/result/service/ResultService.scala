package io.ubilab.result.service

import scala.concurrent.{ExecutionContext, Future}

import io.ubilab.result.model.{EventResult, Result, ResultId}
import io.ubilab.result.repository.ResultRepositoryImpl
import io.ubilab.result.repository.inmemory.ResultGuardian.RichResult

final class ResultService (
  resultRepository: ResultRepositoryImpl
)(implicit ec: ExecutionContext) {

  def addResults(results: List[Result]): Future[List[Boolean]] =
    Future.sequence(results.map(addResult))

  def addResult(result: Result): Future[Boolean] =
    resultRepository.add(result.id, result)

  def seenAllResult: Future[List[Boolean]] =
    for {
      results <- getAllResult
      acknowledges <- Future.sequence(results.map(result => seenResult(result.id)))
    } yield acknowledges

  def seenResult(id: ResultId): Future[Boolean] =
    resultRepository.seen(id)

  def unseenResult(id: ResultId): Future[Boolean] =
    resultRepository.unSeen(id)

  def getAllResult: Future[List[Result]] =
    resultRepository.getAll

  def getAllRichResult: Future[List[RichResult]] =
    resultRepository.getAllRichResult

  def getAllResultSeen: Future[List[Result]] =
    getAllResult.map(_.filter(_.isSeen))

  def getAllResultUnSeen: Future[List[Result]] =
    getAllResult.map(_.filterNot(_.isSeen))

  def getAllResultByRecentUpdate: Future[List[Result]] = {

    val sortByRecentDate: (EventResult, EventResult) => Boolean = {
      case (er1, er2) => er1.createdAt.isAfter(er2.createdAt)
    }

    val sortByRecentRichResult: (RichResult, RichResult) => Boolean = {
      case (rr1, rr2) =>
        (for {
          sortRr1 <- rr1.eventStatus.sortWith(sortByRecentDate).headOption
          sortRr2 <- rr2.eventStatus.sortWith(sortByRecentDate).headOption
        } yield {
          sortRr1.createdAt.isAfter(sortRr2.createdAt)
        }).getOrElse(false)
    }

    for {
      richResults <- getAllRichResult
      sortRichResults = richResults.sortWith(sortByRecentRichResult)
    } yield sortRichResults.map(_.result)
  }
}

object ResultService {

  def build(
    resultRepository: ResultRepositoryImpl
  )(implicit ec: ExecutionContext): ResultService =
    new ResultService(resultRepository)
}