package ch.epfl.lamp.replhtml

import unfiltered.netty.websockets._

object ReplMain {
  def main(args: Array[String]) {
    val sockets = collection.mutable.ListBuffer.empty[WebSocket]
    // TODO: WebSocketServer is deprecated in unfiltered 0.6.8
    WebSocketServer("/socket/repl", 8080) {
      case Open(s)               => sockets += s
      case Message(s, Text(str)) => println(s"message: $str")
        val resp = interpret(str); println(s"Response: $resp")
        sockets foreach (_.send(resp))
      case Close(s)    => sockets -= s
      case Error(s, e) => println(s"error ${e.getMessage}")
    } run ()
  }

  import scala.tools.nsc._
  import scala.tools.nsc.interpreter._

  val cmd = new CommandLine(Nil, println)
  import cmd.settings
  settings.classpath.value = System.getProperty("replhtml.class.path")

  val interpreter = new IMain(settings)
  val completion = new JLineCompletion(interpreter)
  // interpreter.bind("servlet", "ch.epfl.lamp.replhtml.ReplServlet", ReplServlet.this) }
  // interpreter.unleash()

  def interpret(data: String): String = {
    // TODO: use json
    val Complete = """complete@(\d*)""".r
    object I { def unapply(x: String): Option[Int] = scala.util.Try { x.toInt } toOption }
    data.split(":", 2) match {
      case Array(Complete(I(pos)), source) =>
        "<completion>:" + pos + "\n" + {
          lazy val tokens = source.substring(0, pos).split("""[\ \,\;\(\)\{\}]""") // could tokenize on client
          if (pos <= source.length && tokens.nonEmpty)
            completion.topLevelFor(Parsed.dotted(tokens.last, pos) withVerbosity 4).mkString("\n")
          else ""
        }

      case Array("run", source) =>
        util.stringFromStream { ostream =>
          Console.withOut(ostream) {
            interpreter.interpret(source) match {
              case IR.Error => println("<done:error>")
              case IR.Success => println("<done:success>")
              case IR.Incomplete => println("<done:incomplete>")
            }
          }
        }
    }
  }
}
