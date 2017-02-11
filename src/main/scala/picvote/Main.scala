package picvote

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import spray.httpx.SprayJsonSupport
import spray.routing.Directives

object Main extends Directives with SprayJsonSupport {
  implicit val system = ActorSystem("rest-server")
  implicit val executionContext = system.dispatcher

  private var votes = Map.empty[String, Int]

  private val dropboxToken = Option(System.getenv("DROPBOX_TOKEN")) getOrElse {
    throw new Exception("You must specify a Dropbox token through environment variable DROPBOX_TOKEN")
  }

  private val dropbox = new Dropbox(dropboxToken, "/picvote")

  def main(args: Array[String]): Unit = {
    val routes = system.actorOf(Props(classOf[Routes], dropbox))

    IO(Http) ! Http.Bind(routes, interface = "0.0.0.0", port = 8888)
  }
}
