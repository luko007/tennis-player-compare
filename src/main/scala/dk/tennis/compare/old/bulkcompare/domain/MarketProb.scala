package dk.tennis.compare.old.bulkcompare.domain

import java.text.SimpleDateFormat
import dk.tennisprob.TennisProbCalc.MatchTypeEnum._
import dk.atp.api.domain._
import dk.atp.api.domain.SurfaceEnum._
import java.text.DecimalFormatSymbols
import java.util.Locale
import dk.tennis.compare.domain.BfMarket

/**@param runnerProbs[selectionId, probability]*/
case class MarketProb(market: BfMarket, runnerProbs: Map[Long, Double], surface: SurfaceEnum, matchType: MatchTypeEnum) {

  private val DATA_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

  def toCSV(): List[String] = {
    val df = new SimpleDateFormat(DATA_FORMAT)
    val numberFormat = new java.text.DecimalFormat("#.####", new DecimalFormatSymbols(Locale.UK))
    val marketData = for {
      (selectionId, prob) <- runnerProbs
      val runnerRecord = market.eventId :: market.fullDescription :: df.format(market.scheduledOff) ::
        selectionId :: market.runnerMap(selectionId).name :: numberFormat.format(prob) :: surface.toString :: matchType :: Nil

    } yield runnerRecord.mkString(",")

    marketData.toList
  }
}