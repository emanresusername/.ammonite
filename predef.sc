interp.repositories() ++= Seq(
    coursier.maven.MavenRepository("https://jitpack.io")
)

val ammoniteGroup = s"ammonite-shell_${scala.util.Properties.versionNumberString}"

interp.load.ivy(
  "com.lihaoyi"                          % ammoniteGroup    % ammonite.Constants.version,
  "com.github.emanresusername" %% "scalandroid"    % "0.0.12",
  "com.gitlab.gitjab.searx"             %% "client"         % "0.0.5",
  "net.ruippeixotog"                    %% "scala-scraper"  % "2.0.0"
)
@
val shellSession = ammonite.shell.ShellSession()
import shellSession._
import ammonite.ops._
import ammonite.shell._
ammonite.shell.Configure(interp, repl, wd)

import scala.collection.JavaConverters._

import scala.util.{ Try, Success, Failure, Random }

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import monix.execution.Scheduler.Implicits.global
import monix.execution.CancelableFuture
import monix.reactive.{Consumer, Observable}
import monix.eval.{Task, Coeval}
import fr.hmil.roshttp.HttpRequest

import java.io.InputStream
import java.time
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, Instant}

import java.awt, awt.datatransfer.{StringSelection, Clipboard, DataFlavor}
def clipboard: Clipboard = {
  awt.Toolkit.getDefaultToolkit.getSystemClipboard
}

def clipcopy(string: String): Unit = {
  val stringSelection = new StringSelection(string)
  clipboard.setContents(
    stringSelection,
    stringSelection
  )
}

def clippaste: String = {
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

// TODO: conflicts with ammonite |>
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Document
import net.ruippeixotog.scalascraper.browser.{HtmlUnitBrowser, JsoupBrowser}
import net.ruippeixotog.scalascraper.util.ProxyUtils

def torify: Unit = {
  ProxyUtils.setSocksProxy("localhost", 9050)
}

def privoxify: Unit = {
  ProxyUtils.setProxy("localhost", 8118)
}
privoxify

lazy val jsoupBrowser = JsoupBrowser.typed()
// TODO: function because proxy settings locked in after first request (pre 2.0.0 release)
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

import my.will.be.done.scalandroid._
lazy implicit val droid = Unicoid()

implicit class FutureIterable[F](future: Future[Iterable[F]]) {
  def observable: Observable[F] = {
    Observable.fromFuture(future)
      .flatMap(Observable.fromIterable)
  }
}

import my.will.be.done.searx.client.Client
import my.will.be.done.searx.model._
lazy val searx = new Client("https://search.disroot.org")

implicit class FutureSearch(search: Future[Search]) {
  def results: Observable[Result] = {
    search.map(_.results).observable
  }
}

def droidWaitForResource(resourceId: String): Observable[UiNode] = {
  Observable
    .repeatEval(droid.uiautomatorDump.findResource(resourceId))
    .delayOnNext(0.5.second)
    .flatMap { option ⇒
      Observable.fromIterable(option.toIterable)
    }
    .headF
}

def droidCloseAll: Observable[InputStream] = {
  Observable.cons(
    droid.keycode(Keycode.AppSwitch),
    droidWaitForResource("com.android.systemui:id/recents_close_all_button")
      .map(droid.tap)
  )
}

import java.awt.event.KeyEvent._
val robot = new java.awt.Robot

def compoundInterest(principle: Double, rate: Double, perYear: Double, years: Double): Double = {
  principle * math.pow((1 + (rate / perYear)), (perYear * years))
}

import scala.io.Source
def stdinLinerator: Iterator[String] = Source.stdin.getLines
def stdinLines: String = stdinLinerator.mkString("\n")
def stdinLine: String = stdinLinerator.next
