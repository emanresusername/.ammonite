interp.repositories() ++= Seq(
    coursier.maven.MavenRepository("https://jitpack.io")
)

val circeVersion = "0.8.+"
val raptureVersion = "2.0.0-M9"
val ammoniteGroup = s"ammonite-shell_${scala.util.Properties.versionNumberString}"

Seq(
  "com.lihaoyi"                 % ammoniteGroup         % ammonite.Constants.version,
  "com.github.emanresusername"  % "squants-fx"          % "0.0.1",
  "io.circe"                   %% "circe-core"          % circeVersion,
  "io.circe"                   %% "circe-generic"       % circeVersion,
  "io.circe"                   %% "circe-parser"        % circeVersion,
  "io.circe"                   %% "circe-optics"        % circeVersion,
  "org.gnieh"                  %% "diffson-circe"       % "2.2.+",
  "com.propensive"             %% "rapture-json-circe"  % raptureVersion,
  "com.github.javafaker"        % "javafaker"           % "0.+",
  "net.ruippeixotog"           %% "scala-scraper"       % "2.0.0-RC2",
  "eu.timepit"                 %% "refined"             % "0.8.2",
  "org.scala-graph"            %% "graph-dot"           % "1.11.5"
).foreach(interp.load.ivy(_))
@
val shellSession = ammonite.shell.ShellSession()
import shellSession._
import ammonite.ops._
import ammonite.shell._
ammonite.shell.Configure(interp, repl, wd)

import scala.collection.JavaConverters._

import scala.util.{ Try, Success, Failure, Random }

import io.circe.generic.auto._, io.circe.syntax._
import rapture.json.jsonBackends.circe._
import rapture.json._
import rapture.json.dictionaries.dynamic._
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
import squants.market._, MoneyConversions._
import squants.space._, LengthConversions._
import squants.time.TimeConversions._

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Consumer, Observable}
import monix.eval.{Task, Coeval}
import fr.hmil.roshttp.HttpRequest

import my.will.be.done.squants.fx.{
  FixerDotIo, MoneyContextSource, CachingExchangeRatesSource
}
case object FixerDotIoMoneyContextSource
    extends FixerDotIo with MoneyContextSource with CachingExchangeRatesSource {
  implicit val scheduler = global
  implicit val executionContext = global
}
import FixerDotIoMoneyContextSource.moneyContext

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

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.Url

type AbvPrice = squants.Price[squants.Volume]
def abvPrice(cost: squants.Money, abv: Double, volume: squants.Volume): AbvPrice = {
  cost / (volume * (abv / 100))
}
def abvPrice(cost: squants.Money, proof: Int, volume: squants.Volume): AbvPrice = {
  abvPrice(cost = cost, abv = proof / 2.0, volume = volume)
}

// TODO: this interferes with amm's `|>`
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Document
import net.ruippeixotog.scalascraper.browser.{HtmlUnitBrowser, JsoupBrowser}
import net.ruippeixotog.scalascraper.util.ProxyUtils

def torify: Unit = {
  ProxyUtils.setSocksProxy("localhost", 9050)
}
torify

def privoxify: Unit = {
  ProxyUtils.setProxy("localhost", 8118)
}
privoxify

def withoutHttpProxy[T](f: ⇒ Future[T]): Future[T] = {
  Future {
    val httpProxy = ProxyUtils.getProxy
    ProxyUtils.removeProxy
    httpProxy
  }.flatMap {
    case httpProxy ⇒
      f.andThen {
        case _ ⇒
          httpProxy.foreach {
            case (host, port) ⇒ ProxyUtils.setProxy(host, port)
          }
      }
  }
}

def withoutSocksProxy[T](f: ⇒ Future[T]): Future[T] = {
  Future {
    val socksProxy = ProxyUtils.getSocksProxy
    ProxyUtils.removeSocksProxy
    socksProxy
  }.flatMap {
    case socksProxy ⇒
      f.andThen {
        case _ ⇒
          socksProxy.foreach {
            case (host, port) ⇒ ProxyUtils.setSocksProxy(host, port)
          }
      }
  }
}

def withoutProxys[T](f: ⇒ Future[T]): Future[T] = {
  withoutHttpProxy(withoutSocksProxy(f))
}

def withoutHttpProxy[T](f: ⇒ T): T = {
  Await.result(withoutHttpProxy { Future { f } }, Duration.Inf)
}
def withoutSocksProxy[T](f: ⇒ T): T = {
  Await.result(withoutSocksProxy { Future { f } }, Duration.Inf)
}
def withoutProxys[T](f: ⇒ T): T = {
  Await.result(withoutProxys { Future { f } }, Duration.Inf)
}

lazy val jsoupBrowser = JsoupBrowser.typed()
// NOTE: function because proxy settings locked in after first request
def htmlUnitBrowser: HtmlUnitBrowser = {
  HtmlUnitBrowser.typed()
}
def withHtmlUnitBrowser[T](f: (HtmlUnitBrowser) ⇒ T): T = {
  val browser = htmlUnitBrowser
  try {
    f(browser)
  } finally {
    browser.clearCookies
    browser.closeAll
  }
}

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

def getBody(url: String Refined Url): Future[String] = {
  HttpRequest(url).get.map(_.body)
}
def getJson(url: String Refined Url): Future[Json] = {
  getBody(url).map(Json.parse(_))
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
  val recentTracks = withoutHttpProxy(withHtmlUnitBrowser(_.get(host)))
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
def blazinTracksSince(daysAgo: Int): Seq[String] = {
  datesBlazinTracks(
    Stream.iterate(LocalDate.now)(_.minusDays(1)).take(daysAgo):_*
  )
}
def todaysBlazinTracks: Seq[String] = {
  datesBlazinTracks(LocalDate.now)
}

case class UrbanDefinition(
  host: String,
  word: String,
  href: String,
  meaning: String,
  example: Option[String],
  tagHrefs: Iterable[String]
) {
  def url(href: String): String = host ++ href
  def wordUrl: String = url(href)
  def tagUrls: Iterable[String] = tagHrefs.map(url)
  def tags: Iterable[String] = tagHrefs.map(_.split("=").last)
}
def urbanDefine(term: String): Observable[UrbanDefinition] = {
  val host = "https://www.urbandictionary.com"
  for {
    htmlDoc ← Observable.fromFuture(getHtmlDoc(HttpRequest(s"$host/define.php")
                                    .withQueryParameter("term", term)))
    defPanel ← Observable(htmlDoc >> elementList(".def-panel"):_*)
    anchor = defPanel >> element("a.word")
    word = anchor.text
    href = anchor.attr("href")
    meaning = defPanel >> text(".meaning")
    example = defPanel >?> text(".example")
    tagHrefs = defPanel >> attrs("href")(".tags>a[href]")
  } yield {
    UrbanDefinition(
      host = host,
      word = word,
      href = href,
      meaning = meaning,
      example = example,
      tagHrefs = tagHrefs
    )
  }
}
implicit def parallelism: Int = Runtime.getRuntime().availableProcessors
def printlnParallelSync[T](implicit parallelism: Int): Consumer[T, Unit] = {
  Consumer.foreachParallel(parallelism)(println)
}
def printlnSerialSync[T]: Consumer[T, Unit] = Consumer.foreach(println)

repl.prompt.bind(
  Seq(
    Option(s"$whoami@$hostname:${wd.toString}[$date]"),
    gitBranch.map(branch => {
                         s"<$branch>"
                       }),
    Option("\nᕕ( ᐛ )ᕗ ")
  ).flatten.mkString
)

def cmd(op: String)(args: Shellable*): Future[CommandResult] = Future {
  %%.applyDynamic(op)(args:_*)
}

import scalax.collection.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
  // NOTE: scala concurrent triemap doesn't cross compile to scalajs
import java.util.concurrent.ConcurrentHashMap

// NOTE: this is just to play with scalagraph and monix. the wheel: https://github.com/martido/brew-graph
type StringGraph = Graph[String, DiEdge]
def brewDependencyGraph: Task[StringGraph] = {
  val rootGraphMap = {
    new ConcurrentHashMap[String, Observable[StringGraph]]
  }

  def brewLines(args: Shellable*): Observable[String] = {
    Observable.fromFuture(cmd("brew")(args:_*))
      .map(_.out.lines)
      .flatMap(Observable.fromIterable)
  }

  def cachedRootGraph(root: String, putIfAbsent: Observable[StringGraph]): Observable[StringGraph] = {
    val firstRun = putIfAbsent.cache
    rootGraphMap.putIfAbsent(root, firstRun) match {
      case null ⇒
        firstRun
      case cached ⇒
        cached
    }
  }

  def rootGraph(formula: String): Observable[StringGraph] = {
    brewLines("deps", formula).flatMap { dep ⇒
      Observable.cons(
        Graph(formula ~> dep),
        cachedRootGraph(dep, rootGraph(dep))
      )
    }.foldLeftF(Graph.empty: StringGraph) {
      case (graph, subGraph) ⇒
        graph ++ subGraph
    }
  }

  brewLines("list").flatMap { root ⇒
    Observable.cons(
      Graph(root): StringGraph,
      cachedRootGraph(root, rootGraph(root))
    )
  }.foldLeftL(Graph.empty: StringGraph) {
    case (graph, subGraph) ⇒
      graph ++ subGraph
  }
}

import scalax.collection.io.dot, dot._, implicits._

def toDot[N, E[X] <: EdgeLikeIn[X]](
  graph: Graph[N, E],
  directed: Boolean,
  edgeTransformer: Graph[N,E]#EdgeT => Option[DotEdgeStmt],
  nodeTransformer: Graph[N,E]#NodeT => Option[DotNodeStmt],
  id: Option[dot.Id] = None,
  attrStmts: List[DotAttrStmt] = Nil,
  attrList: List[DotAttr] = Nil
): String = {
  val root = DotRootGraph (
    directed = directed,
    id        = id,
    attrStmts = attrStmts,
    attrList  = attrList
  )
  graph.toDot(
    dotRoot = root,
    edgeTransformer = edgeTransformer(_).map(root → _),
    iNodeTransformer = Option(nodeTransformer(_).map(root → _))
  )
}

def stringGraphNodeTransformer(innerNode: StringGraph#NodeT):
    Option[DotNodeStmt] =
  Option(DotNodeStmt('"' +: innerNode.toString :+ '"', Nil))

def stringGraphEdgeTransformer(innerEdge: StringGraph#EdgeT):
    Option[DotEdgeStmt] = innerEdge.edge match {
  case DiEdge(source, target) =>
    Option(
      DotEdgeStmt(
        '"' +: source.toString :+ '"',
        '"' +: target.toString :+ '"'
      )
    )
}
