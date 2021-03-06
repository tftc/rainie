resolvers ++= Seq(
  "Twitter Maven" at "https://maven.twttr.com"
)


  addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.2")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.0")
addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "4.5.0")

//hot reload plugin https://github.com/spray/sbt-revolver
addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.1")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.5")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.2.0")
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")
