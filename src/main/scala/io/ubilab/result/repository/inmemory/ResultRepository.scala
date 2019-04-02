package io.ubilab.result.repository.inmemory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import akka.actor.Scheduler

import io.ubilab.result.model.{Result, ResultId}
import io.ubilab.result.repository.ResultRepositoryImpl
import io.ubilab.result.repository.inmemory.ResultGuardian.Cmd._
import io.ubilab.result.repository.inmemory.ResultGuardian.{Reply, RichResult}

final class ResultRepository (
  system: ActorSystem[ResultGuardian.Cmd]
) extends ResultRepositoryImpl {

  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = system.scheduler
  implicit val ec: ExecutionContextExecutor = system.executionContext

  override def get(id: ResultId): Future[Option[Result]] =
    system ? (ref => Get(id, ref))

  override def getAll: Future[List[Result]] =
    system ? (ref => GetAll(ref))

  override def getAllRichResult: Future[List[RichResult]] =
    system ? (ref => GetAllRichResult(ref))

  private val replyToBoolean: Reply => Boolean = {
    case Reply.Updated => true
    case Reply.NotAllowed => false
    case _ => false
  }

  override def add(id: ResultId, result: Result): Future[Boolean] =
    (system ?[Reply](ref => Add(result.id, result, ref))).map(replyToBoolean)

  override def seen(id: ResultId): Future[Boolean] =
    (system ?[Reply](ref => See(id, ref))).map(replyToBoolean)

  override def unSeen(id: ResultId): Future[Boolean] =
    (system ?[Reply](ref => UnSee(id, ref))).map(replyToBoolean)

}
