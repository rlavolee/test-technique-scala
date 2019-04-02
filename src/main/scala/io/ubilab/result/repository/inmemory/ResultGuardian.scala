package io.ubilab.result.repository.inmemory

import java.time.{Clock, Instant}
import scala.collection.immutable.SortedMap

import akka.actor.typed.scaladsl.{Behaviors, StashBuffer}
import akka.actor.typed.{ActorRef, Behavior}

import io.ubilab.result.model.{EventResult, EventResultStatus, OwnerId, Result, ResultId}
import io.ubilab.result.repository.inmemory.ResultGuardian._

import scala.util.Random

final class ResultGuardian (
  clock: Clock
) {

  private def eventCreated(idOwner: OwnerId, now: Instant, acc: List[EventResult] = List.empty): List[EventResult] =
    EventResult(EventResultStatus.Created, idOwner, now) +: acc

  private def eventReceived(idOwner: OwnerId, acc: List[EventResult]): List[EventResult] =
    EventResult(EventResultStatus.Received, idOwner, Instant.now) +: acc

  private def eventSeen(idOwner: OwnerId, acc: List[EventResult]): List[EventResult] =
    EventResult(EventResultStatus.Seen, idOwner, Instant.now(clock)) +: acc

  private def eventUnSeen(idOwner: OwnerId, acc: List[EventResult]): List[EventResult] =
    EventResult(EventResultStatus.UnSeen, idOwner, Instant.now) +: acc

  private val cmdToEvent: WriteCmd => Option[RichResult] => Option[Event] = {
    case Cmd.Add(_, result, _) => {
      case None =>
        val now = Instant.now.plusNanos(Random.nextInt(10) + 1)
        Some(Event.AddedResult(RichResult(result, eventCreated(result.idOwner, now)), now))
      case _ => None
    }
    case _: Cmd.See => {
      case Some(richResult) =>
        Some(Event.Seen(
          richResult.copy(
            result = richResult.result.copy(isSeen = true),
            eventStatus = eventSeen(richResult.result.idOwner, richResult.eventStatus)
          )
        ))
      case _ => None
    }
    case _: Cmd.UnSee => {
      case Some(richResult) =>
        Some(Event.UnSeen(
          richResult.copy(
            result = richResult.result.copy(isSeen = false),
            eventStatus = eventUnSeen(richResult.result.idOwner, richResult.eventStatus)
          )
        ))
      case _ => None
    }
    case _: Cmd.Receive => {
      case Some(richResult) =>
        Some(Event.Received(
          richResult.copy(
            eventStatus = eventReceived(richResult.result.idOwner, richResult.eventStatus)
          )
        ))
      case _ => None
    }
    case _ => _ => None
  }

  private val eventToRichResult: Model => Event => Model =
    model => {
      case event: Event.AddedResult =>
        model + (event.at -> Map(event.richResult.result.id -> event.richResult))
      case event =>
        event.richResult.eventStatus.collectFirst{
          case EventResult(EventResultStatus.Created, _, date) =>
            model.updated(date, Map(event.richResult.result.id -> event.richResult))
        }.getOrElse(model)
    }


  // Writer
  private[inmemory] val writerBehavior: Behavior[(Model, WriteCmd, ActorRef[Cmd])] =
    Behaviors.receiveMessage{
      case (model: Model, cmd: WriteCmd, parent: ActorRef[WriteCmd]) =>
        cmdToEvent(cmd)(model.values.flatMap(_.get(cmd.id)).headOption) match {
          case Some(event) =>
            val modelToUpdate = eventToRichResult(model)(event)
            parent ! Cmd.Update(modelToUpdate, cmd.replyTo)
          case None =>
            parent ! Cmd.NotAllow(model, cmd.replyTo)
        }
        Behavior.same
    }

  // Reader
  private[inmemory] val readerBehavior: Behavior[(Model, ReadCmd)] = {
    Behaviors.receiveMessage{
      case (model: Model, cmd: Cmd.Get) =>
        val filteredModel = model.values.flatMap(_.get(cmd.id)).headOption.map(_.result)
        cmd.replyTo ! filteredModel
        Behaviors.same
      case (model: Model, cmd: Cmd.GetAll) =>
        cmd.replyTo ! model.values.flatMap(_.values.map(_.result)).toList
        Behaviors.same
      case (model: Model, cmd: Cmd.GetAllRichResult) =>
        cmd.replyTo ! model.values.flatMap(_.values).toList
        Behaviors.same
    }
  }

  val mainBehavior: Behavior[Cmd] =
    Behaviors.setup[Cmd]{ context =>

      val buffer = StashBuffer[Cmd](capacity = 100)

      val writerRef = context.spawn(writerBehavior, "writer")
      val readerRef = context.spawn(readerBehavior, "reader")

      def cmdManagerBehavior(model: Model): Behavior[Cmd] =
        Behaviors.receive{ (context, message) =>
          message match {
            case cmd: WriteCmd =>
              writerRef ! (model, cmd, context.self)
              saveBehavior
            case cmd: ReadCmd =>
              readerRef ! (model, cmd)
              Behaviors.same
            case _: Cmd =>
              Behaviors.same
          }
        }

      def saveBehavior: Behavior[Cmd] =
        Behaviors.receive{ (context, message) =>
          message match {
            case Cmd.Update(newModel, replyTo) =>
              replyTo ! Reply.Updated
              buffer.unstashAll(context, cmdManagerBehavior(newModel))
            case Cmd.NotAllow(model, replyTo) =>
              replyTo ! Reply.NotAllowed
              buffer.unstashAll(context, cmdManagerBehavior(model))
            case cmd: Cmd =>
              buffer.stash(cmd)
              Behaviors.same
          }
        }

      cmdManagerBehavior(SortedMap.empty)
    }
}

object ResultGuardian {

  type Model = SortedMap[Instant, Map[ResultId, RichResult]]

  //Commands
  sealed abstract class Cmd extends Product with Serializable

  sealed abstract class ReadCmd extends Cmd
  sealed abstract class WriteCmd extends Cmd with ReplyTo[Reply] {
    def id: ResultId
  }

  object Cmd {
    //Write
    final case class Add (id: ResultId, result: Result, replyTo: ActorRef[Reply]) extends WriteCmd

    final case class See (id: ResultId, replyTo: ActorRef[Reply]) extends WriteCmd
    final case class UnSee (id: ResultId, replyTo: ActorRef[Reply]) extends WriteCmd
    final case class Receive (id: ResultId, replyTo: ActorRef[Reply]) extends WriteCmd

    //Read
    final case class Get (id: ResultId, replyTo: ActorRef[Option[Result]]) extends ReadCmd
    final case class GetAll (replyTo: ActorRef[List[Result]]) extends ReadCmd
    final case class GetAllRichResult (replyTo: ActorRef[List[RichResult]]) extends ReadCmd

    //Internal
    private[inmemory] final case class Update (model: Model, replyTo: ActorRef[Reply]) extends Cmd
    private[inmemory] final case class NotAllow (model: Model, replyTo: ActorRef[Reply]) extends Cmd
  }

  //Events
  abstract class Event extends Product with Serializable {
    def richResult: RichResult
  }

  object Event {
    final case class AddedResult (richResult: RichResult, at: Instant) extends Event
    final case class Seen (richResult: RichResult) extends Event
    final case class UnSeen (richResult: RichResult) extends Event
    final case class Received (richResult: RichResult) extends Event
  }

  //Replies
  trait ReplyTo[T] {
    def replyTo: ActorRef[T]
  }
  sealed abstract class Reply extends Product with Serializable
  object Reply {
    case object Updated extends Reply
    case object NotAllowed extends Reply
  }

  sealed abstract class ResultReply extends Reply with Product with Serializable
  final case class RichResult(
    result:      Result,
    eventStatus: List[EventResult]
  ) extends ResultReply
}
