package ch.epfl.lamp.replhtml

import scala.collection.mutable.Set
import java.io.{ OutputStream, PrintStream }
import javax.servlet.http._
import org.eclipse.jetty.websocket._
import org.eclipse.jetty.websocket.WebSocket.Outbound

import scala.tools.nsc._
import scala.tools.nsc.interpreter._

class ReplServlet extends WebSocketServlet {
  val clients = Set.empty[ReplWebSocket]

  var classpath = System.getProperty("replhtml.class.path")
  assert(classpath ne null, "System property replhtml.class.path is not set. Repl needs a class path to operate.")
  classpath += System.getProperty("path.separator") + System.getProperty("replhtml.extra.class.path", "")
  println(classpath)
  println("EXTRA:"+System.getProperty("replhtml.extra.class.path"))

  val cmd = new InterpreterCommand(Nil, println)
  val settings = cmd.settings//new Settings
  settings.classpath.value = classpath
//  settings.usejavacp.value = true
//settings.Ycompletion.value = true
  val interpreter = new Interpreter(settings) {
    override def reset() = { super.reset; unleash() }
    override def unleash() = { super.unleash; bind("servlet", "ch.epfl.lamp.replhtml.ReplServlet", ReplServlet.this) }
  }
  interpreter.unleash()
  val completion = new Completion(interpreter)

  override def doGet(req: HttpServletRequest, res: HttpServletResponse) =
    getServletContext.getNamedDispatcher("default").forward(req, res)

  override def doWebSocketConnect(req:HttpServletRequest, protocol:String ) =
    new ReplWebSocket

  class WebSocketPrintStream(cls: Set[ReplWebSocket]) extends PrintStream(new OutputStream { def write(b: Int) = {} }) {

    override def print(message: String) = {
      cls.foreach { c => c.outbound.sendMessage(0:Byte, message) }
    }
/*
    override def println(message: String) = {
      clients.foreach { c => c.outbound.sendMessage(0:Byte, message+"\n\r") }
    }
    override def println() = {
      clients.foreach { c => c.outbound.sendMessage(0:Byte, "\n\r") }
    }
*/
  }


  class ReplWebSocket extends WebSocket {

    var outbound:Outbound = _

    override def onConnect(outbound:Outbound) = {
      this.outbound = outbound
      clients += this
    }

    override def onMessage(frame:Byte, data:Array[Byte], offset:Int, length:Int) = {}

    override def onMessage(frame:Byte, data:String) = {
      val idx = data.indexOf(":")
      val key = if (idx > 0) data.substring(0,idx) else ""
      var source = if (idx >= 0) data.substring(idx+1) else data
        key match {
          case "complete" => 
          
            val out = new WebSocketPrintStream(Set(this))
            
            val idx = source.indexOf(":")
            val ipos = Integer.parseInt(source.substring(0,idx))
            source = source.substring(idx+1)

            if (ipos <= source.length) {
              val tokens = source.substring(0,ipos).split("""[\ \,\;\(\)\{\}]""") // could tokenize on client
              //println("try to complete: " + tokens.mkString(","))
              if (!tokens.isEmpty) {
                val cmpl = completion.topLevelFor(Parsed.dotted(tokens.last, ipos) withVerbosity 4) // (?)
                out.println("<completion>:"+ipos+"\n"+cmpl.mkString("\n"))
              } else {
                out.println("<completion>:"+ipos+"\n")
              }
            } else {
              out.println("<completion>:"+ipos+"\n")
            }
            //interpreter.requestFromLine(source)

         case _ =>
            Console.withOut(new WebSocketPrintStream(Set(this))) {
              interpreter.interpret(source) match {
                case InterpreterResults.Error       => println("<done:error>")
                case InterpreterResults.Success     => println("<done:success>")
                case InterpreterResults.Incomplete  => println("<done:incomplete>")
              }
            }
        }
    }

    override def onDisconnect = clients -= this

  }
}
