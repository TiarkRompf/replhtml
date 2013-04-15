package ch.epfl.lamp.replhtml

import unfiltered.netty.websockets._

object ReplMain {
  def main(args: Array[String]) {
    val sockets = collection.mutable.ListBuffer.empty[WebSocket]
    // TODO: WebSocketServer is deprecated in unfiltered 0.6.8
    WebSocketServer("/socket/repl", 8080) {
      case Open(s) => sockets += s
      case Message(s, Text(str)) =>
        println(s"message: $str"); val resp = handle(str); println(s"Response: $resp"); sockets foreach (_.send(resp))
      case Close(s) => sockets -= s
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

  def handle(data: String): String = {
    val idx = data.indexOf(":")
    val key = if (idx > 0) data.substring(0, idx) else ""
    var source = if (idx >= 0) data.substring(idx + 1) else data

    key match {
      case "complete" =>
        val idx = source.indexOf(":")
        val ipos = Integer.parseInt(source.substring(0, idx))
        source = source.substring(idx + 1)

        val res = if (ipos <= source.length) {
          val tokens = source.substring(0, ipos).split("""[\ \,\;\(\)\{\}]""") // could tokenize on client
          println("try to complete: " + tokens.mkString(","))
          if (tokens.nonEmpty) {
            val cmpl = completion.topLevelFor(Parsed.dotted(tokens.last, ipos) withVerbosity 4) // (?)
            "<completion>:" + ipos + "\n" + cmpl.mkString("\n")
          } else "<completion>:" + ipos + "\n"
        } else "<completion>:" + ipos + "\n"
        println("res: " + res)
        res

      case _ =>
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
