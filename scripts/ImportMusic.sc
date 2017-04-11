import ammonite.ops._


def trash(path: Path) = {
  println(s"trash ← $path")
  mv.into(path, home/".Trash")
}
val prefixRegex = """(?i)(.+)_(?:small|large|medium|hd720|127k|Free_Mixtape_Downloads.*|HipHop_Early.*)\.mp(?:3|4)""".r
def prefixGroupedPaths(paths: Seq[Path]) = {
  paths.groupBy(_.name match {
                  case prefixRegex(prefix) ⇒
                    prefix
                  case name ⇒
                    name
                })

}

case class MediaManager(mediaDir: Path)(implicit wd: Path = pwd) {
  def mp3Paths = ls!mediaDir |? (_.ext.equalsIgnoreCase("mp3"))
  def convertToMp3s(fromExt: String) = {
    for {
      fromPath ← ls!mediaDir |? (_.ext.equalsIgnoreCase(fromExt))
    } yield {
      val mp3 = fromPath.toString.replaceAll(s"\\.$fromExt$$", ".mp3")
      try {
        %%ffmpeg("-y", "-i", fromPath, "-q:a", "0", "-map", "a", mp3)
        println(s"mp3 ← $fromExt ← $mp3")
      } catch {
        case e: ShelloutException ⇒
          e.printStackTrace
      }
      trash(fromPath)
    }
  }

  def trashSmallMp3s = mp3Paths.filter(_.size <= 2649).foreach(trash)
  def importMp3s = {
    for {
      (prefix, mp3s) ← prefixGroupedPaths(mp3Paths)
    } {
      val (Seq(winner), losers) = mp3s.sortBy(-_.size).splitAt(1)
      losers.foreach(trash)
      val newName = prefix.replaceAllLiterally("_", " ")
        .replaceAllLiterally(" ft ", " ft. ") ++ ".mp3"
      println(s"itunes ← $newName ← $winner)")
      mv(winner, home/'Music/'iTunes/"iTunes Media"/"Automatically Add to iTunes.localized"/newName)
    }
  }
}

@main
def main(musicDir: Path) = {
  val mediaManager = MediaManager(musicDir)
  Seq(
    "mp4",
    "m4a",
    "webm"
  ).foreach(mediaManager.convertToMp3s)
  mediaManager.trashSmallMp3s
  mediaManager.importMp3s
}
