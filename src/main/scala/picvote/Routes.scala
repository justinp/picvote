package picvote

import spray.http.StatusCodes
import spray.routing.HttpServiceActor
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

class Routes(dropbox: Dropbox) extends HttpServiceActor {
  implicit val ec = actorRefFactory.dispatcher

  // These are the only two elements of an event that having any bearing on the app.

  case class Event(eventType: String, payload: String)
  implicit val eventFormat = jsonFormat2(Event)

  override def receive = runRoute {
    logRequest("IN") {
      path("event") {
        post {
          entity(as[Event]) { e =>
            e.eventType match {
              case "inboundMedia" =>
                dropbox.save(e.payload)
                complete(StatusCodes.OK)

              case "inboundText" =>
                dropbox.vote(e.payload)
                complete(StatusCodes.OK)

              case _ =>
                complete(StatusCodes.OK)
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
