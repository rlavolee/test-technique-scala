package io.ubilab.result.service

import io.ubilab.result.model.{Result, ResultId}
import io.ubilab.result.repository.inmemory.ResultRepository
import org.scalatest.{FunSpec, Matchers}

class ResultServiceSpec extends FunSpec with Matchers {

  val results = List(
    Result(
      id = ResultId(46),
      idOwner = 76,
      idRecipients = List(42),
      isSeen = false,
      eventResults = Nil,
      contentOfResult = "test"
    ),
    Result(
      id = ResultId(47),
      idOwner = 76,
      idRecipients = List(42),
      isSeen = false,
      eventResults = Nil,
      contentOfResult = "test"
    ),
    Result(
      id = ResultId(48),
      idOwner = 76,
      idRecipients = List(42),
      isSeen = false,
      eventResults = Nil,
      contentOfResult = "test"
    )
  )

  describe("Step 1 : initialisation du projet avec 0 et 1 resultat") {

    val resultRepository = new ResultRepository
    val resultService = ResultService.build(resultRepository)

    it("devrait être initialisé avec une liste de résultat vide") {

      resultService.getAllResult shouldEqual List()
    }
  }

  describe("Après l'ajout d'un résultat,") {

    val resultRepository = new ResultRepository
    val resultService = ResultService.build(resultRepository)

    resultService.addResult(
      Result(
        id = ResultId(46),
        idOwner = 76,
        idRecipients = List(42),
        isSeen = false,
        eventResults = Nil,
        contentOfResult = "test"
      )
    )

    it("devrait avoir une liste de 1 résultat non vue") {

      resultService.getAllResult.length shouldEqual 1

    }

    it("devrait avoir une liste de 1 résultat vue aprés la vision de ce résultat") {

      resultService.seenResult(ResultId(46))
      resultService.getAllResultSeen.length shouldEqual 1
      resultService.getAllResult.head.isSeen shouldEqual true
    }

  }

  describe("Après l'ajout de 3 résultats,") {
    // init le service avec 3 resultats
    val resultRepository = new ResultRepository
    val resultService = ResultService.build(resultRepository)

    resultService.addResults(results)

    it("devrait avoir une liste de 3 resultats non vue aprés l'ajout de 3 resultat.") {
      resultService.getAllResult.length shouldEqual 3
      resultService.getAllResultSeen.length shouldEqual 0
    }

    it("ne devrait pas authorisé l'ajout d'un résultats avec un id existant") {
      val sut =
        resultService.addResult(
          Result(
            id = ResultId(46),
            idOwner = 76,
            idRecipients = List(42),
            isSeen = false,
            eventResults = Nil,
            contentOfResult = "test"
          )
        )

      sut shouldEqual false
    }

    it("devrait avoir 1 resultats vue dans la liste aprés la vision d'un resultat") {
      resultService.seenResult(ResultId(46))
      resultService.getAllResultSeen.length shouldEqual 1
      resultService.getAllResult.head.isSeen shouldEqual true
    }

    it("devrait avoir les 3 resultats vue dans la liste aprés qu'il soit tous vue") {
      resultService.seenAllResult()
      resultService.getAllResultSeen.length shouldEqual 3
      resultService.getAllResult.map(_.isSeen) shouldEqual List(true, true, true)
    }

    it("devrait avoir plus que 2 resultats vue dans la liste aprés qu'il soit tous vue puis 1 ou la vue est enlevé") {
      resultService.getAllResultSeen.length should be > 2
      resultService.unseenResult(ResultId(46))
      resultService.getAllResultSeen.length shouldEqual 2
    }

    it("ne devrait pas planté aprés la vision d'un resultat non ajouté") {
      resultService.seenResult(ResultId(9000))
      true
    }

  }


  describe("Après l'ajout de 3 résultats,") {
    pending
    // init le service avec 3 resultats
    it("devrait avoir la list des résultat dans l'order de création ( en se basant sur les events de création)") {
      true shouldEqual false
    }

    it("devrait avoir 1 event a la date de maintenant quand 1 résultat est vue") {
      true shouldEqual false
    }

    it("devrait avoir 2 events avec 2 dates différent aprés la vision d'un resultat puis la suppression de la vision") {
      true shouldEqual false
    }

    it("devrait avoir une fonction qui retourne une liste ordonnée des resultats par rapport au dernier modifier") {
      true shouldEqual false
    }
  }


  describe("N'hésitez pas a proposer de nouveaux tests") {}
}
