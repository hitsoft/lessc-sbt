// Running tests scripts in sbt-test folder (scripted)

libraryDependencies <+= sbtVersion(v =>"org.scala-sbt" % "scripted-plugin" % v)

// Plugin that generates IntelliJ Idea projects (gen-idea)

addSbtPlugin("com.hanhuy.sbt" % "sbt-idea" % "1.6.0")

// Plugin that helps to publish artifacts to bintray

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")

// Plugin to work with http://ls.implicit.ly/ repository

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")