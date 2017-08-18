package mesosphere.marathon
package core.flow

import org.rogach.scallop.ScallopConf
import scala.concurrent.duration._

trait ReviveOffersConfig extends ScallopConf {

  lazy val reviveOffersForNewApps = toggle(
    "revive_offers_for_new_apps",
    descrYes = "(Default) Call reviveOffers for new or changed apps.",
    descrNo = "Disable reviveOffers for new or changed apps.",
    hidden = true,
    default = Some(true),
    prefix = "disable_")

  lazy val minReviveOffersInterval = opt[Long](
    "min_revive_offers_interval",
    descr = "Do not ask for all offers (also already seen ones) more often than this interval (ms).",
    default = Some(5000))

  lazy val reviveOffersRepetitions = opt[Int](
    "revive_offers_repetitions",
    descr = "Repeat every reviveOffer request this many times, delayed by the --min_revive_offers_interval.",
    default = Some(3))

  lazy val suppressOffersMax = opt[Long](
    "suppress_offers_max",
    descr = "Do not suppress offers longer than this interval even if no offers are wanted (ms).",
    default = Some(5.minutes.toMillis)
  )
}
