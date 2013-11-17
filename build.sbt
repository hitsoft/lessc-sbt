
sbtPlugin := true

organization := "com.hitsoft"

name := "sbt-lessc"

scalacOptions ++= Seq("-deprecation", "-feature", "-encoding", "utf-8")

description := "Sbt plugin for compiling Less CSS sources with lessc system command"

// Plugin tests with Scripted

seq(scriptedSettings:_*)

scriptedLaunchOpts <<= (scriptedLaunchOpts, version).apply {
  (scriptedOpts, vers) =>
    scriptedOpts ++ Seq(
      "-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + vers
    )
}

scriptedBufferLog := false

// publishing ivy artifact to bintray

seq(bintraySettings:_*)

publishArtifact in Test := false

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintray.Keys.bintrayOrganization in bintray.Keys.bintray := Some("hitsoft")

// ls.implicit.ly

seq(lsSettings:_*)