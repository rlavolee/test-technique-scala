package io.ubilab.result.service

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._

import akka.actor.typed.ActorSystem
import org.scalatest.{AsyncFunSpec, BeforeAndAfterAll, Matchers}

import io.ubilab.result.model.{EventResult, EventResultStatus, OwnerId, Result, ResultId}
import io.ubilab.result.repository.inmemory.ResultGuardian.RichResult
import io.ubilab.result.repository.inmemory.{ResultGuardian, ResultRepository}

class ResultServiceSpec extends AsyncFunSpec with BeforeAndAfterAll with Matchers {

  val results = List(
    Result(
      id = ResultId(46),
      idOwner = OwnerId(76),
      idRecipients = List(42),
      isSeen = false,
      contentOfResult = "test"
    ),
    Result(
      id = ResultId(47),
      idOwner = OwnerId(76),
      idRecipients = List(42),
      isSeen = false,
      contentOfResult = "test"
    ),
    Result(
      id = ResultId(48),
      idOwner = OwnerId(76),
      idRecipients = List(42),
      isSeen = false,
      contentOfResult = "test"
    )
  )

  describe("Step 1 : initialisation du projet avec 0 et 1 resultat") {

    val now = Instant.now
    val clock = Clock.fixed(now, ZoneId.systemDefault())
    val behavior = new ResultGuardian(clock)
    val system = ActorSystem[ResultGuardian.Cmd](behavior.mainBehavior, "system")

    implicit val ec: ExecutionContextExecutor = system.executionContext

    val resultRepository = new ResultRepository(system)
    val resultService = ResultService.build(resultRepository)

    it("devrait être initialisé avec une liste de résultat vide") {

      resultService.getAllResult.map(results => results shouldEqual List())
    }
  }

  describe("Après l'ajout d'un résultat,") {

    val now = Instant.now
    val clock = Clock.fixed(now, ZoneId.systemDefault())
    val behavior = new ResultGuardian(clock)
    val system = ActorSystem[ResultGuardian.Cmd](behavior.mainBehavior, "system")

    implicit val ec: ExecutionContextExecutor = system.executionContext

    val resultRepository = new ResultRepository(system)
    val resultService = ResultService.build(resultRepository)

    resultService.addResult(
      Result(
        id = ResultId(46),
        idOwner = OwnerId(76),
        idRecipients = List(42),
        isSeen = false,
        contentOfResult = "test"
      )
    )

    it("devrait avoir une liste de 1 résultat non vue") {

      resultService.getAllResult.map(_.length shouldEqual 1)

    }

    it("devrait avoir une liste de 1 résultat vue aprés la vision de ce résultat") {

      resultService.seenResult(ResultId(46))
      resultService.getAllResultSeen.map(_.length shouldEqual 1)
      resultService.getAllResult.map(_.head.isSeen shouldEqual true)
    }

  }

  describe("Après l'ajout de 3 résultats,") {
    // init le service avec 3 resultats
    val now = Instant.now
    val clock = Clock.fixed(now, ZoneId.systemDefault())
    val behavior = new ResultGuardian(clock)
    val system = ActorSystem[ResultGuardian.Cmd](behavior.mainBehavior, "system")

    implicit val ec: ExecutionContextExecutor = system.executionContext

    val resultRepository = new ResultRepository(system)
    val resultService = ResultService.build(resultRepository)
    Await.result(resultService.addResults(results), 5.seconds)

    it("devrait avoir une liste de 3 resultats non vue aprés l'ajout de 3 resultat.") {
      resultService.getAllResult.map(_.length shouldEqual 3)
      resultService.getAllResultSeen.map(_.length shouldEqual 0)
    }

    it("ne devrait pas authorisé l'ajout d'un résultats avec un id existant") {
      val sut =
        resultService.addResult(
          Result(
            id = ResultId(46),
            idOwner = OwnerId(76),
            idRecipients = List(42),
            isSeen = false,
            contentOfResult = "test"
          )
        )

      sut.map(_ shouldEqual false)
    }

    it("devrait avoir 1 resultats vue dans la liste aprés la vision d'un resultat") {
      Await.result(resultService.seenResult(ResultId(46)), 5.seconds)
      resultService.getAllResultSeen.map(_.length shouldEqual 1)
      resultService.getAllResult.map(_.head.isSeen shouldEqual true)
    }

    it("devrait avoir les 3 resultats vue dans la liste aprés qu'il soit tous vue") {
      Await.result(resultService.seenAllResult, 5.seconds)
      for {
        resultsSeen <- resultService.getAllResultSeen
        results <- resultService.getAllResult
      } yield {
        resultsSeen.length shouldEqual 3
        results.map(_.isSeen) shouldEqual List(true, true, true)
      }
    }

    it("devrait avoir plus que 2 resultats vue dans la liste aprés qu'il soit tous vue puis 1 ou la vue est enlevé") {
      resultService.getAllResultSeen.map(_.length should be > 2)
      Await.result(resultService.unseenResult(ResultId(46)), 5.seconds)
      resultService.getAllResultSeen.map(_.length shouldEqual 2)
    }

    it("ne devrait pas planté aprés la vision d'un resultat non ajouté") {
      resultService.seenResult(ResultId(9000))
      succeed
    }

  }


  describe("Après l'ajout de 3 résultats,") {
    // init le service avec 3 resultats
    val now = Instant.now
    val clock = Clock.fixed(now, ZoneId.systemDefault())
    val behavior = new ResultGuardian(clock)
    val system = ActorSystem[ResultGuardian.Cmd](behavior.mainBehavior, "system")

    implicit val ec: ExecutionContextExecutor = system.executionContext

    val resultRepository = new ResultRepository(system)
    val resultService = ResultService.build(resultRepository)
    Await.result(resultService.addResults(results), 5.seconds)

    // unsafe get for testing purpose only
    val getCreationDate: RichResult => Instant = {
      _.eventStatus.collectFirst{
        case EventResult(EventResultStatus.Created, _, date) => date
      }.get
    }

    it("devrait avoir la list des résultat dans l'order de création ( en se basant sur les events de création)") {
      // as my model is a SortedMap on Instant creation date
      for {
        results <- resultService.getAllResult
        richResults <- resultService.getAllRichResult
        resultsId = results.map(_.id)
        richResultsOrderedId =
          richResults
            .sortWith{case (rr1, rr2) => getCreationDate(rr1).isBefore(getCreationDate(rr2))}.map(_.result.id)
      } yield {
        resultsId shouldEqual richResultsOrderedId
      }
    }

    it("devrait avoir 1 event a la date de maintenant quand 1 résultat est vue") {
      Await.result(resultService.seenResult(ResultId(46)), 5.seconds)
      // unsafe get for testing purpose only
      for {
        results <- resultService.getAllRichResult
        result = results.find(_.result.id == ResultId(46)).get
      } yield {
        val createdAt = result.eventStatus.collectFirst{
          case EventResult(EventResultStatus.Seen, _, date) => date
        }.get

        createdAt shouldEqual now
      }
    }

    it("devrait avoir 2 events avec 2 dates différent aprés la vision d'un resultat puis la suppression de la vision") {
      Await.result(resultService.seenResult(ResultId(47)), 5.seconds)
      Await.result(resultService.unseenResult(ResultId(47)), 5.seconds)
      // unsafe get for testing purpose only
      for {
        results <- resultService.getAllRichResult
        result = results.find(_.result.id == ResultId(47)).get
      } yield {
        val events = result.eventStatus.filter{_.status match {
          case EventResultStatus.Seen => true
          case EventResultStatus.UnSeen => true
          case _ => false
        }}

        events.length shouldEqual 2

        val (seen, unseen) =
          (events.find(_.status == EventResultStatus.Seen).get, events.find(_.status == EventResultStatus.UnSeen).get)

        seen.createdAt.isBefore(unseen.createdAt) shouldEqual true
      }
    }

    it("devrait avoir une fonction qui retourne une liste ordonnée des resultats par rapport au dernier modifier") {
      Await.result(resultService.unseenResult(ResultId(46)), 5.seconds)
      Await.result(resultService.unseenResult(ResultId(47)), 5.seconds)
      Await.result(resultService.unseenResult(ResultId(48)), 5.seconds)

      for {
        results <- resultService.getAllResultByRecentUpdate
        resultsId = results.map(_.id)
      } yield {
        resultsId should not equal List(ResultId(46), ResultId(47), ResultId(48))
      }
    }
  }


  describe("N'hésitez pas a proposer de nouveaux tests") {}
}
