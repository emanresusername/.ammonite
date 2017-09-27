interp.repositories() ++= Seq(
    coursier.maven.MavenRepository("https://jitpack.io")
)

val ammoniteGroup = s"ammonite-shell_${scala.util.Properties.versionNumberString}"

interp.load.ivy(
  "com.lihaoyi"              % ammoniteGroup           % ammonite.Constants.version,
  "com.gitlab.gitjab.searx" %% "client"                % "0.0.1"
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
import monix.reactive.{Consumer, Observable}
import monix.eval.{Task, Coeval}
import fr.hmil.roshttp.HttpRequest

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
