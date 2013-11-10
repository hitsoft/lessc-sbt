sbtPlugin := true

organization := "com.hitsoft"

name := "lessc-sbt"

version <<= sbtVersion(v =>
  if (v.startsWith("0.13")) "0.1.0"
  else error("unsupported sbt version %s" format v)
)

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