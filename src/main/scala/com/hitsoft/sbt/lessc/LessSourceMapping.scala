package com.hitsoft.sbt.lessc

import java.io.File
import sbt.IO
import sbt.Path._

class LessSourceMapping(val sourcesDir: File, val lessFile: File, val cssDir: File, val lessSources: Seq[File], val mini: Boolean, val suffix: String) {

  private def cssPath = {
    val relPath = IO.relativize(sourcesDir, lessFile).get
    var res = relPath.replaceFirst("\\.entry\\.less$", ".css").replaceFirst("\\.less$", ".css")
    if (suffix != null)
      res = res.replaceFirst("\\.css$", ".%s.css" format suffix)
    if (mini)
      res.replaceFirst("\\.css$", ".min.css")
    else
      res
  }

  val cssFile = new File(cssDir, cssPath)

  def changed = (lessFile newerThan cssFile) || (lessSources exists (_ newerThan cssFile))

  override def toString = lessFile.toString
}
