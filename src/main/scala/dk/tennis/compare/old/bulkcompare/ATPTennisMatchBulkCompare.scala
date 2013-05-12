package dk.tennis.compare.old.bulkcompare

import scala.io.Source
import org.apache.commons.io.FileUtils._
import java.io.File
import scala.collection.JavaConversions._
import dk.atp.api.facts.AtpFactsApi._
import dk.atp.api.domain.SurfaceEnum._
import dk.tennisprob.TennisProbCalc.MatchTypeEnum._
import org.apache.commons.math.util._
import dk.atp.api.tournament.TournamentAtpApi._
import org.joda.time.DateTime
import scala.math._
import dk.atp.api.ATPMatchesLoader
import dk.tennis.compare.matching.GenericMarketCompare
import dk.tennis.compare.domain.Market
import dk.tennis.compare.domain.MarketProb

/**
 * Calculates tennis market probabilities for a list of markets in a batch process.
 *
 * @author KorzekwaD
 */
class ATPTennisMatchBulkCompare(tennisMatchCompare: TennisPlayerCompare, atpMatchLoader: ATPMatchesLoader) extends TennisMatchBulkCompare {

  /**
   * Calculates tennis market probabilities for a list of markets in a batch process and exports it to CSV file.
   *
   *  @param marketDataFileIn File path to CSV file with market records. Input market data CSV columns:
   *  market_id, market_name, market_time (yyyy-mm-dd hh:mm:ss, e.g. 2011-12-21 18:19:09),selection_id, selection_name
   *  There must be two records for every market, one record for each selection.
   *
   *  @param marketProbFileOut CVS file for exporting market probabilities.
   *  Columns: The same columns as for input file, and:  'probability' of winning a tennis match, surface (CLAY,GRASS,HARD), matchType (THREE_SET_MATCH/FIVE_SET_MATCH).
   *
   *  @param progress Number of markets remaining for processing..
   *
   */
  def matchProb(marketDataFileIn: String, marketProbFileOut: String, progress: (Int) => Unit): Unit = {
    val marketDataSource = Source.fromFile(marketDataFileIn)

    val markets = Market.fromCSV(marketDataSource.getLines().drop(1).toList)

    val marketsSize = markets.size
    val marketProbabilities = for ((market, index) <- markets.zipWithIndex) yield {
      progress(marketsSize - index)
      val tournament = lookup(market)
      toMarketProb(market, tournament)
    }

    val filteredAndSortedMarketProbs = marketProbabilities.filter(p => p.isDefined && p.get.runnerProbs.values.exists(!_.isNaN)).
      sortWith(_.get.market.scheduledOff.getTime() < _.get.market.scheduledOff.getTime)
    val marketProbsData = filteredAndSortedMarketProbs.flatMap(p => p.get.toCSV())

    val marketProbFile = new File(marketProbFileOut)
    val header = "event_id,full_description,scheduled_off,selection_id,selection,probability, surface, match_type"
    writeLines(marketProbFile, header :: marketProbsData)
  }

  private def toMarketProb(m: Market, tournament: Option[Tournament]): Option[MarketProb] = {
    val marketProb: Option[MarketProb] = try {
      val runners = m.runnerMap.keys.toList

      val matchType = tournament.get.numOfSet match {
        case 2 => THREE_SET_MATCH
        case 3 => FIVE_SET_MATCH
      }
      val probability = tennisMatchCompare.matchProb(m.runnerMap(runners(0)).name, m.runnerMap(runners(1)).name, tournament.get.surface, matchType, tournament.get.tournamentTime)

      Option(MarketProb(m, Map(runners(0) -> probability, runners(1) -> (1 - probability)), tournament.get.surface, matchType))
    } catch { case _ => None }
    marketProb
  }

  /**Look for tournament matching given market.*/
  private def lookup(market: Market): Option[Tournament] = {
    val year = new DateTime(market.scheduledOff).getYear()

    val matches = atpMatchLoader.loadMatches(year)

    val filteredMatches = matches.filter(m => GenericMarketCompare.compare(m, market) > 0.032248)

    if (!filteredMatches.isEmpty) Option(filteredMatches.head.tournament) else None
  }

}