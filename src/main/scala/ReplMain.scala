package ch.epfl.lamp.replhtml

//import javax.servlet.http._
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlet.DefaultServlet

object ReplMain {
  def main(args: Array[String]) {
    val server = new Server(8080)

    val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
    context.setContextPath("/")
    server.setHandler(context)

    context.addServlet(new ServletHolder(new ReplServlet()),"/socket/*")
    context.addServlet(new ServletHolder(new DefaultServlet()),"/*")

    server.start()
    println(">>> embedded jetty server started. press any key to stop.")
    while (System.in.available() == 0) {
      Thread.sleep(1500)
    }
    System.in.read()
    println(">>> stopping...")
    server.stop()
    server.join()
  }
}