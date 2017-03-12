import Resolvers._

val oss = Resolver.Http(
  "Sonatype OSS Snapshots",
  "https://oss.sonatype.org/content/repositories/snapshots",
  MavenPattern,
  true
)

interp.resolvers() = interp.resolvers() :+ oss

val circeVersion = "0.7.+"
val raptureVersion = "2.0.0-M8"

Seq(
  "com.lihaoyi"            %% "ammonite-shell"      % ammonite.Constants.version,
  "com.github.kxbmap"      %% "configs"             % "0.4.+",
  "net.ruippeixotog"       %% "scala-scraper"       % "1.2.+",
  "org.apache.poi"          % "poi-ooxml"           % "3.15",
  "io.circe"               %% "circe-core"          % circeVersion,
  "io.circe"               %% "circe-generic"       % circeVersion,
  "io.circe"               %% "circe-parser"        % circeVersion,
  "io.circe"               %% "circe-optics"        % circeVersion,
  "org.gnieh"              %% "diffson-circe"       % "2.1.+",
  "com.propensive"         %% "rapture-json-circe"  % raptureVersion,
  "com.propensive"         %% "rapture-io"          % raptureVersion,
  "com.propensive"         %% "rapture-uri"         % raptureVersion,
  "com.propensive"         %% "rapture-net"         % raptureVersion,
  "com.github.javafaker"    % "javafaker"           % "0.12",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.25"
).foreach(interp.load.ivy(_))
@
val shellSession = ammonite.shell.ShellSession()
import shellSession._
import ammonite.shell.PPrints._
import ammonite.ops._

import ammonite.shell._
ammonite.shell.Configure(repl, wd)

import com.typesafe.config.ConfigFactory
import configs.syntax._
import configs.Configs

import sys.process._

import scala.collection.JavaConversions._

import scala.util.{ Try, Success, Failure, Random }

import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import java.time
import time.{LocalDateTime, Instant, ZoneId, format}, format.DateTimeFormatter

import rapture.core.EnrichedString
import rapture.uri._
import rapture.io._
import rapture.net._
import rapture.codec._, encodings.`UTF-8`._

import io.circe.generic.auto._, io.circe.syntax._
import rapture.json.jsonBackends.circe._
import rapture.json.Json
import rapture.json.formatters.humanReadable._
implicit val encodeRapture = new io.circe.Encoder[Json] {
  final def apply(json: Json): io.circe.Json = json.as[io.circe.Json]
}
import gnieh.diffson.circe._

val faker = new com.github.javafaker.Faker

def pbcopy(text: String) = {
  ("pbcopy" #< new java.io.ByteArrayInputStream(text.getBytes))!
}

def pbpaste = %%('pbpaste).out.string

private[this] def current_branch = {
  scala.util.Try {
    ((%%git('branch)).out.lines.filter(grep!("""\*""".r))).head.substring(2)
  }.toOption
}

private[this] def hostname = {
  (%%hostname).out.lines.head
}

private[this] def whoami = {
  (%%whoami).out.lines.head
}

private[this] def date = {
  LocalDateTime.ofInstant(
    Instant.now,
    ZoneId.systemDefault
  ).format(DateTimeFormatter.ofPattern("E, MMMM | YYYY-MM-dd HH:mm:ss"))
}

repl.prompt.bind(
  Seq(
    Option(s"$whoami@$hostname:${wd.toString}[$date]"),
    current_branch.map(branch => {
                         s"<$branch>"
                       }),
    Option("\nᕕ( ᐛ )ᕗ ")
  ).flatten.mkString
)
