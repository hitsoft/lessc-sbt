package less

import sbt._
import sbt.Keys._
import sbt.Def.{ ScopedKey }
import java.io.File
import java.nio.charset.Charset
import scala.sys.process.Process

/** 2.10 shim for classifying Non fatal exceptions in exception handling */
private [less] object NonFatal {
  def apply(t: Throwable): Boolean = t match {
    case _: StackOverflowError => true
    case _: VirtualMachineError | _: ThreadDeath | _: InterruptedException | _: LinkageError  => false
    case _ => true
  }
  def unapply(t: Throwable): Option[Throwable] = if (apply(t)) Some(t) else None
}

/** Sbt frontend for the less CSS compiler */
object Plugin extends sbt.Plugin {

  object LessKeys {
    lazy val less = TaskKey[Seq[File]](
      "less", "Compiles .less sources.")
    lazy val mini = SettingKey[Boolean](
      "mini", "Minifies compiled .less sources. Default is false.")
    lazy val charset = SettingKey[Charset](
      "charset", "Sets the character encoding used in file IO. Default is utf-8.")
    lazy val filter = SettingKey[FileFilter](
      "filter", "Filter for selecting less sources from default directories.")
    lazy val all = TaskKey[Seq[File]](
      "all", "Compiles all .less sources regardless of freshness")
  }
  import LessKeys.{ less => lesskey, _ }

  private def lessCleanTask =
    (streams, resourceManaged in lesskey) map {
      (out, target) =>
        out.log.info("Cleaning generated CSS under %s" format target)
        IO.delete(target)
    }

  private def compileSource(
    charset: Charset,
    log: Logger,
    mini: Boolean)(mapping: LessSourceMapping) =
    try {
      log.debug("Compiling %s" format mapping.lessFile)
      Process(Seq("lessc", if(mini) "--compress" else "", mapping.lessFile.getAbsolutePath, mapping.cssFile.getAbsolutePath)).! match {
        case 0 => Some(mapping.cssFile)
        case n => sys.error("Could not compile %s source %s".format("lessc", mapping.cssFile))
      }
    } catch {
      case NonFatal(e) => throw new RuntimeException(
        "Error occured while compiling %s:\n%s" format(
        mapping, e.getMessage), e)
    }

  private def allCompileTask =
    (streams, sourceDirectory in lesskey,
     resourceManaged in lesskey, target in lesskey,
     filter in lesskey, excludeFilter in lesskey,
     charset in lesskey, mini in lesskey) map compileIf { _ => true }

  private def lessCompileTask =
    (streams, sourceDirectory in lesskey,
     resourceManaged in lesskey, target in lesskey,
     filter in lesskey, excludeFilter in lesskey,
     charset in lesskey, mini in lesskey) map compileIf(_.changed)

  private def compileIf(cond: LessSourceMapping => Boolean)
    (out: std.TaskStreams[ScopedKey[_]], sourcesDir: File, cssDir: File, targetDir: File,
     incl: FileFilter, excl: FileFilter, charset: Charset, mini: Boolean) =
       (for {
         file <- sourcesDir.descendantsExcept(incl, excl).get
         lessSrc = new LessSourceMapping(file, sourcesDir, targetDir, cssDir)
         if cond(lessSrc)
       } yield lessSrc) match {
         case Nil =>
           out.log.debug("No less sources to compile")
           compiled(cssDir)
         case files =>
           out.log.info("Compiling %d less sources to %s" format (
           files.size, cssDir))
           files map compileSource(charset, out.log, mini)
           compiled(cssDir)
       }

  // move defaultExcludes to excludeFilter in unmanagedSources later
  private def lessSourcesTask =
    (sourceDirectory in lesskey, filter in lesskey, excludeFilter in lesskey) map {
      (sourceDir, filt, excl) =>
         sourceDir.descendantsExcept(filt, excl).get
    }

  private def compiled(under: File) = (under ** "*.css").get

  def lessSettingsIn(c: Configuration): Seq[Setting[_]] =
    inConfig(c)(lessSettings0 ++ Seq(
      sourceDirectory in lesskey <<= (sourceDirectory in c) { _ / "less" },
      resourceManaged in lesskey <<= (resourceManaged in c) { _ / "css" },
      cleanFiles in lesskey <<= (resourceManaged in lesskey, target in lesskey)(_ :: _ :: Nil),
      watchSources in lesskey <<= (unmanagedSources in lesskey)
    )) ++ Seq(
      cleanFiles <++= (cleanFiles in lesskey in c),
      watchSources <++= (watchSources in lesskey in c),
      resourceGenerators in c <+= lesskey in c,
      compile in c <<= (compile in c).dependsOn(lesskey in c)
    )

  def lessSettings: Seq[Setting[_]] =
    lessSettingsIn(Compile) ++ lessSettingsIn(Test)

  def lessSettings0: Seq[Setting[_]] = Seq(
    charset in lesskey := Charset.forName("utf-8"),
    mini in lesskey := false,
    filter in lesskey := "*.less",
    excludeFilter in lesskey <<= excludeFilter in Global,
    unmanagedSources in lesskey <<= lessSourcesTask,
    clean in lesskey <<= lessCleanTask,
    lesskey <<= lessCompileTask,
    all in lesskey <<= allCompileTask
  )
}
