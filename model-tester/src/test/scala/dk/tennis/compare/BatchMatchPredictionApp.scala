package dk.tennis.compare

import dk.tennis.compare.rating.multiskill.matchloader.MatchesLoader
import java.text.SimpleDateFormat
import com.typesafe.scalalogging.slf4j.Logging
import dk.tennis.compare.rating.multiskill.infer.matchprob.givenmatchresults.InferMatchProbGivenMatchResults
import java.util.Date
import dk.tennis.compare.rating.multiskill.matchloader.MatchResult
import dk.tennis.compare.rating.multiskill.model.perfdiff.Surface
import dk.tennis.compare.rating.multiskill.matchloader.PlayerStats

object BatchMatchPredictionApp extends App with Logging {

  val df = new SimpleDateFormat("dd/MM/yyyy")
  val matchesFile = "./src/test/resources/atp_historical_data/match_data_2006_2015_040115.csv"
  val matchResults = MatchesLoader.loadMatches(matchesFile, 2011, 2015)
  logger.info("All matches:" + matchResults.size)
  logger.info("All players:" + matchResults.flatMap(m => List(m.player1, m.player2)).distinct.size)

  val matchModel = InferMatchProbGivenMatchResults(matchResults.toIndexedSeq)

  val predictionMatches = List(
    ("Stan Wawrinka", "David Goffin"),
     ("Roger Federer", "Milos Raonic"),
     ("David Ferrer", "Tomas Berdych"))

  predictionMatches.foreach {
    case (p1, p2) =>

      val time = df.parse("10/01/2015")
      val result = MatchResult(time, "tournament name", Surface.HARD, p1, p2, time, player1Won = true, numOfSets = 2, PlayerStats(0, 0, 0), PlayerStats(0, 0, 0))

      val prediction = matchModel.predict(result)

      val p1Matches = matchResults.filter(m => m.containsPlayer(p1))
      val p2Matches = matchResults.filter(m => m.containsPlayer(p2))

      println("%20s %20s %10.2f %10.2f %10d %10d".format(p1, p2, 1d / prediction.matchProb(p1), 1d / prediction.matchProb(p2), p1Matches.size, p2Matches.size))
  }
}