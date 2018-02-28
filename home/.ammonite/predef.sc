interp.repositories() ++= Seq(
    coursier.maven.MavenRepository("https://jitpack.io")
)

val ammoniteGroup = s"ammonite-shell_${scala.util.Properties.versionNumberString}"

interp.load.ivy(
  "com.lihaoyi"                          % ammoniteGroup    % ammonite.Constants.version,
  "com.github.emanresusername" %% "scalandroid"    % "0.0.12",
  "com.gitlab.gitjab.searx"             %% "client"         % "0.0.6",
  "net.ruippeixotog"                    %% "scala-scraper"  % "2.1.0"
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

import java.net.InetSocketAddress
import com.gargoylesoftware.htmlunit.ProxyConfig
import java.net.Proxy.Type.{HTTP, SOCKS}

// TODO: add to scala-scraper
case class Proxy(host: String, port: Int, isSocks: Boolean) {
  def jsoup: java.net.Proxy = {
    new java.net.Proxy(
      if(isSocks) { SOCKS } else { HTTP },
      new InetSocketAddress(host, port)
    )
  }

  def htmlunit: ProxyConfig = {
    new ProxyConfig(host, port, isSocks)
  }
}

lazy val tor = Proxy("localhost", 9050, true)
lazy val privoxy = Proxy("localhost", 8118, false)

def jsoupBrowser(proxy: Option[Proxy] = Option(privoxy)): JsoupBrowser = {
  proxy.map(_.jsoup).fold(new JsoupBrowser)(p ⇒ new JsoupBrowser(proxy = p))
}
def htmlUnitBrowser(proxy: Option[Proxy] = Option(privoxy)): HtmlUnitBrowser = {
  proxy.map(_.htmlunit).fold(new HtmlUnitBrowser)(p ⇒ new HtmlUnitBrowser(proxy = p))
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
