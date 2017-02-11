package picvote

import spray.http.StatusCodes
import spray.routing.HttpServiceActor
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

class Routes(dropbox: Dropbox) extends HttpServiceActor {
  implicit val ec = actorRefFactory.dispatcher

  // These are the only two elements of an event that having any bearing on the app.

  case class Event(`type`: String, payload: String)
  implicit val eventFormat = jsonFormat2(Event)

  override def receive = runRoute {
    logRequest("IN", akka.event.Logging.InfoLevel) {
      path("event") {
        post {
          entity(as[Event]) { e =>
            complete {
              e.`type` match {
                case "inboundMedia" =>
                  dropbox.save(e.payload)

                case "inboundText" =>
                  dropbox.vote(e.payload)

                case _ =>
                  // NOOP
              }
              StatusCodes.OK
            }
          }
        }
      } ~
      path("report") {
        get {
          complete(dropbox.votes)
        }
      }
    }
  }
}
