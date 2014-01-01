package com.hitsoft.sbt.lessc

import sbt._
import sbt.Keys._
import sbt.Def.ScopedKey
import java.io.File
import java.nio.charset.Charset
import scala.sys.process.Process


/** Sbt frontend for the less CSS compiler */
object Plugin extends sbt.Plugin {

  object LessKeys {
    lazy val less = TaskKey[Seq[File]](
      "less", "Compiles .less files sources filtered by entryFilter if them were changed.")
    lazy val force = TaskKey[Seq[File]](
      "force", "Compiles .less files sources filtered by entryFilter regardless of freshness.")
    lazy val mini = SettingKey[Boolean](
      "mini", "Minifies compiled .less sources. Default is false. If true, output css file will have .min.css extension.")
    lazy val charset = SettingKey[Charset](
      "charset", "Sets the character encoding used in file IO. Default is utf-8.")
    lazy val suffix = TaskKey[String](
      "suffix", "String to append to output filename (before file extension)")
    lazy val entryFilter = SettingKey[FileFilter](
      "entry-filter", "Filter for selecting less files to compile. Default is *.entry.less.")
    lazy val unmanagedLessSources = TaskKey[Seq[File]](
      "unmanaged-less-sources", "List of source less files that could be used in compilation")
    lazy val lessc = SettingKey[Seq[String]](
      "lessc", "lessc command")
  }

  import LessKeys.{less => lesskey, _}

  private def lessCleanTask =
    (streams, resourceManaged in lesskey) map {
      (out, target) =>
        out.log.info("Cleaning generated CSS under %s" format target)
        IO.delete(target)
    }

  private def compileSource(lessc: Seq[String],
                             charset: Charset,
                             log: Logger,
                             mini: Boolean)(mapping: LessSourceMapping) =
    try {
      log.debug("Compiling %s" format mapping.lessFile)
      IO.createDirectory(mapping.cssFile.getParentFile)

      Process(lessc ++ Seq(if (mini) "--compress" else "", mapping.lessFile.getCanonicalPath, mapping.cssFile.getCanonicalPath)).! match {
        case 0 => Some(mapping.cssFile)
        case n => sys.error("Could not compile %s source %s".format(mapping.cssFile, mapping.lessFile))
      }
    } catch {
      case NonFatal(e) => throw new RuntimeException(
        "Error occured while compiling %s:\n%s" format(
          mapping, e.getMessage), e)
    }

  private def forceLessCompileTask =
    (streams,
      lessc in lesskey,
      sourceDirectory in lesskey,
      unmanagedSources in lesskey,
      unmanagedLessSources in lesskey,
      resourceManaged in lesskey,
      charset in lesskey, mini in lesskey, suffix in lesskey) map compileIf {
      _ => true
    }

  private def lessCompileTask =
    (streams,
      lessc in lesskey,
      sourceDirectory in lesskey,
      unmanagedSources in lesskey,
      unmanagedLessSources in lesskey,
      resourceManaged in lesskey,
      charset in lesskey, mini in lesskey, suffix in lesskey) map compileIf(_.changed)

  private def compileIf(cond: LessSourceMapping => Boolean)
                       (out: std.TaskStreams[ScopedKey[_]], lessc: Seq[String], sourcesDir: File, entryFiles: Seq[File], lessFiles: Seq[File],
                        cssDir: File, charset: Charset, mini: Boolean, suffix: String) =
    (for {
      file <- entryFiles
      mapping = new LessSourceMapping(sourcesDir, file, cssDir, lessFiles, mini, suffix)
      if cond(mapping)
    } yield mapping) match {
      case Nil =>
        out.log.debug("No less sources to compile")
        compiled(cssDir)
      case files =>
        out.log.info("Compiling %d less sources to %s" format(
          files.size, cssDir))
        files map compileSource(lessc, charset, out.log, mini)
        compiled(cssDir)
    }

  private def lessEntriesTask =
    (sourceDirectory in lesskey, entryFilter in lesskey, excludeFilter in lesskey) map {
      (sourceDir, incl, excl) =>
        sourceDir.descendantsExcept(incl, excl).get
    }

  private def lessSourcesTask =
    (sourceDirectory in lesskey, includeFilter in lesskey, excludeFilter in lesskey, entryFilter in lesskey) map {
      (sourceDir, incl, excl, entry) =>
        sourceDir.descendantsExcept(incl, excl || entry).get
    }

  private def compiled(under: File) = (under ** "*.css").get

  def lessSettingsManualCompileIn(c: Configuration): Seq[Setting[_]] =
    inConfig(c)(lessSettings0 ++ Seq(
      sourceDirectory in lesskey <<= (sourceDirectory in c) {
        _ / "less"
      },
      resourceManaged in lesskey <<= (resourceManaged in c) {
        _ / "css"
      },
      cleanFiles in lesskey <<= (resourceManaged in lesskey)(_ :: Nil),
      watchSources in lesskey <<= (unmanagedSources in lesskey),
      watchSources in lesskey <++= (unmanagedLessSources in lesskey)
    )) ++ Seq(
      cleanFiles <++= (cleanFiles in lesskey in c),
      watchSources <++= (watchSources in lesskey in c),
      resourceGenerators in c <+= lesskey in c
    )

  def lessSettingsIn(c: Configuration): Seq[Setting[_]] =
    lessSettingsManualCompileIn(c) ++
    inConfig(c)(Seq(
      compile in c <<= (compile in c).dependsOn(lesskey in c)
    ))

  def lessSettingsManualCompile: Seq[Setting[_]] =
    lessSettingsManualCompileIn(Compile)

  def lessSettings: Seq[Setting[_]] =
    lessSettingsIn(Compile)

  def lesscCommand: Seq[String] = {
    System.getProperty("lessc") match {
      case null => Seq("lessc")
      case cmd: String => if (cmd.isEmpty) Seq("lessc") else cmd.split("\\+\\+\\+").toSeq
    }
  }

  def lessSettings0: Seq[Setting[_]] = Seq(
    charset in lesskey := Charset.forName("utf-8"),
    mini in lesskey := false,
    entryFilter in lesskey := "*.entry.less",
    includeFilter in lesskey := "*.less",
    suffix in lesskey := "",
    excludeFilter in lesskey <<= excludeFilter in Global,
    unmanagedSources in lesskey <<= lessEntriesTask,
    unmanagedLessSources in lesskey <<= lessSourcesTask,
    clean in lesskey <<= lessCleanTask,
    lesskey <<= lessCompileTask,
    force in lesskey <<= forceLessCompileTask,
    lessc in lesskey := lesscCommand
  )
}
