val circeVersion = "0.8.+"
val raptureVersion = "2.0.0-M9"
val ammoniteGroup = s"ammonite-shell_${scala.util.Properties.versionNumberString}"
val monixVersion = "2.3.0"

Seq(
  "com.lihaoyi"           % ammoniteGroup         % ammonite.Constants.version,
  "io.circe"             %% "circe-core"          % circeVersion,
  "io.circe"             %% "circe-generic"       % circeVersion,
  "io.circe"             %% "circe-parser"        % circeVersion,
  "io.circe"             %% "circe-optics"        % circeVersion,
  "org.gnieh"            %% "diffson-circe"       % "2.2.+",
  "com.propensive"       %% "rapture-json-circe"  % raptureVersion,
  "com.github.javafaker"  % "javafaker"           % "0.+",
  "org.typelevel"        %% "squants"             % "1.3.0",
  "net.ruippeixotog"     %% "scala-scraper"       % "2.0.0-RC2",
  "fr.hmil"              %% "roshttp"             % "2.0.1",
  "eu.timepit"           %% "refined"             % "0.8.2",
  "io.monix"             %% "monix"               % monixVersion
).foreach(interp.load.ivy(_))
@
val shellSession = ammonite.shell.ShellSession()
import shellSession._
import ammonite.ops._
import ammonite.shell._
ammonite.shell.Configure(repl, wd)

import scala.collection.JavaConverters._

import scala.util.{ Try, Success, Failure, Random }

import io.circe.generic.auto._, io.circe.syntax._
import rapture.json.jsonBackends.circe._
import rapture.json.Json
import io.circe.{Json ⇒ Circe}
import io.circe.optics.JsonPath

implicit class RaptureJson(json: Json) {
  def asCirce: Circe = {
    json.as[Circe]
  }
}

implicit class CirceJson(json: Circe) {
  def asRapture: Json = {
    Json(json)
  }
}

import gnieh.diffson.circe._

import squants.energy.EnergyConversions._
import squants.energy.PowerConversions._
import squants.information.InformationConversions._
import squants.market.MoneyConversions._
import squants.space.LengthConversions._
import squants.time.TimeConversions._

import scala.concurrent.duration.{FiniteDuration, Duration}
implicit def durationToFiniteDuration(duration: Duration): FiniteDuration = duration match {
  case finiteDuration: FiniteDuration ⇒
    finiteDuration
}

val faker = new com.github.javafaker.Faker

import java.time
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, Instant}

import java.awt, awt.datatransfer.{StringSelection, Clipboard, DataFlavor}
def clipboard: Clipboard = {
  awt.Toolkit.getDefaultToolkit.getSystemClipboard
}

def pbcopy(string: String): Unit = {
  val stringSelection = new StringSelection(string)
  clipboard.setContents(
    stringSelection,
    stringSelection
  )
}

def pbpaste: String = {
  import DataFlavor.stringFlavor
  val reader = stringFlavor.getReaderForText(
    clipboard.getContents(stringFlavor)
  )
  new String(
    Iterator.continually(reader.read)
      .takeWhile(_ != -1)
      .map(_.toByte)
      .toArray
  )
}

private[this] def gitBranch = {
  scala.util.Try {
    (%%git('status, "-b", "--porcelain")).out.lines.head.drop(3)
  }.toOption
}

private[this] def hostname = {
  (%%hostname).out.lines.head
}

private[this] def whoami = {
  sys.env("USER")
}

private[this] def date = {
  LocalDateTime.now.format(DateTimeFormatter.ofPattern("E, MMMM | YYYY-MM-dd HH:mm:ss"))
}

import fr.hmil.roshttp.HttpRequest
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.Future

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.Url

import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Document
import net.ruippeixotog.scalascraper.browser.{HtmlUnitBrowser, JsoupBrowser}
lazy val jsoupBrowser = JsoupBrowser()
lazy val htmlUnitBrowser = HtmlUnitBrowser()
def htmlDoc(html: String): Document = {
  jsoupBrowser.parseString(html)
}

def getHtmlDoc(httpRequest: HttpRequest): Future[Document] = {
  for {
    response ← httpRequest.get
  } yield {
    htmlDoc(response.body)
  }
}

def refinedGetHtmlDoc(url: String Refined Url): Future[Document] = {
  getHtmlDoc(HttpRequest(url))
}

def getHtmlDoc(url: String): Future[Document] = {
  Future {
    refineV[Url].unsafeFrom(url)
  }.flatMap(refinedGetHtmlDoc)
}

def externalIp: Future[String] = {
  HttpRequest("https://icanhazip.com").get.map(_.body.trim)
}
def copyExternalIp: Future[String] = {
  for {
    ip ← externalIp
  } yield {
    pbcopy(ip)
    ip
  }
}
def celebrityNetworth(query: String): Future[String] = {
  for {
    resultsDoc <- getHtmlDoc(s"http://www.celebritynetworth.com/dl/${query.replaceAllLiterally(" ", "-")}/")
    href = resultsDoc >> attr("href")(".search_result.lead>a")
    resultDoc <- getHtmlDoc(href)
  } yield {
    resultDoc >> text(".networth>.value")
  }
}

def blazinTracks: Seq[(LocalDate, String)] = {
  val host = "http://www.hiphopearly.com"
  val recentTracks = htmlUnitBrowser.get(host)
  val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMMdd")
  for {
    trackListing <- recentTracks >> elementList(".track-listing")
    year ← trackListing >> texts("div.date>span.year")
    month ← trackListing >> texts("div.date>span.month")
    day ← trackListing >> texts("div.date>span.date")
    localDate = LocalDate.from(dateTimeFormatter.parse(s"$year$month$day"))
    href <- trackListing >> attrs("href")(".track.blazin>a[title]")
  } yield {
    localDate → s"$host/$href"
  }
}
def datesBlazinTracks(dates: LocalDate*): Seq[String] = {
  val dateSet = dates.toSet
  blazinTracks.collect {
    case (day, url) if dateSet.contains(day) ⇒
      url
  }
}
def todaysBlazinTracks: Seq[String] = {
  datesBlazinTracks(LocalDate.now)
}

repl.prompt.bind(
  Seq(
    Option(s"$whoami@$hostname:${wd.toString}[$date]"),
    gitBranch.map(branch => {
                         s"<$branch>"
                       }),
    Option("\nᕕ( ᐛ )ᕗ ")
  ).flatten.mkString
)
