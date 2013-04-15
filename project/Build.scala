import sbt._
import Keys._

object ReplHtmlBuild extends Build {
  val mySettings = Defaults.defaultSettings ++ Seq(
    organization := "ch.epfl.lamp",
    name         := "replhtml",
    version      := "1.1",
    scalaVersion := "2.10.1",
    libraryDependencies := Seq(
      "org.scala-lang" % "scala-compiler" % "2.10.1",
      "org.scala-lang" % "scala-reflect" % "2.10.1",
      "org.scala-lang" % "scala-library" % "2.10.1",
      "net.databinder" %% "unfiltered-filter" % "0.6.8",
      "net.databinder" %% "unfiltered-netty-server" % "0.6.8",
      "net.databinder" %% "unfiltered-netty-websockets" % "0.6.8")
  )

  val setupReplClassPath = TaskKey[Unit]("setup-repl-classpath", "Set up the repl server's classpath based on our dependencies.")

  lazy val project = Project (
    "replhtml",
    file ("."),
    settings = mySettings ++ Seq(
      setupReplClassPath <<= (dependencyClasspath in Compile) map {cp =>
        val cpStr = cp map { case Attributed(str) => str} mkString(System.getProperty("path.separator"))
        println("Repl will use classpath "+ cpStr)
        System.setProperty("replhtml.class.path", cpStr)
      },
      run in Compile <<= (run in Compile).dependsOn(setupReplClassPath)
    )
  )
}