package picvote

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http.HttpHeaders.Authorization
import spray.http.{HttpRequest, OAuth2BearerToken}
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class Dropbox(token: String, basePath: String)(implicit actorSystem: ActorSystem) {
  implicit val executionContext = actorSystem.dispatcher

  // For simplification, I'm assuming that no one else writes to this directory except this app.  That means that
  // our images don't get deleted, everything conforms to the file name conventions in here, all of the file names
  // are sequential and all of the files are images. If we allow others to write to this directory, it complicates
  // things quite a bit because we have to come up with a caching policy or hit dropbox every time anything happens.

  // At startup, we'll read the list of files in the folder and use that to build our cache.

  private val addAuthToken = { in: HttpRequest =>
    in.withHeaders(Authorization(OAuth2BearerToken(token)))
  }

  private val http = logRequest(println(_)) ~> addAuthToken ~> sendReceive ~> logResponse(println(_))

  private case class SaveParams(path: String, url: String)
  implicit private val formatSaveParams = jsonFormat2(SaveParams)

  // TODO: handle concurrency
  // TODO: weakness - blocks for result instead of using future combinators/callbacks
  // TODO: weakness - doesn't validate that the inbound media is a jpeg image

  def save(source: String) = {
    pictureCount += 1
    val f = http(Post("https://api.dropboxapi.com/2/files/save_url", SaveParams(s"$basePath/${picName(pictureCount)}", source)))
    Await.result(f, Duration.Inf)
  }

  private case class ListParams(path: String)

  implicit private val formatListParams = jsonFormat1(ListParams)

  private case class FileInfo(name: String)
  private case class ListResults(entries: List[FileInfo])

  implicit private val formatFileInfo = jsonFormat1(FileInfo)
  implicit private val formatListResults = jsonFormat1(ListResults)

  // Instead of using folder_list (which could require paging and fetching more data, use get_metadata to test for

  // TODO: limitation - only supports one hundred pictures, doesn't page
  // TODO: weakness - blocks for result instead of using future combinators/callbacks

  private def docsList = {
    val httpWithUnmarshal = http ~> unmarshal[ListResults]
    Await.result(httpWithUnmarshal(Post("https://api.dropboxapi.com/2/files/list_folder", ListParams(basePath))), Duration.Inf)
  }

  private def picName(n: Int) = s"pic$n.jpg"

  def votes: Map[String, Int] = Stream.from(1).take(pictureCount).map( n => picName(n) -> pictureVotes.getOrElse(n, 0) ).toMap

  // Since we've got constraints on who can write to this folder, all we need to know to test if an image exists is
  // the number of images.  Determine what's in the folder here and use that to initialize the count.

  private var pictureCount = {
    val entries = docsList.entries.map(_.name).toSet
    Stream.from(1).takeWhile( n => entries.contains(picName(n)) ).size
  }

  // This is used to decide what the next saved picture name should be.

  private var pictureVotes = Map.empty[Int, Int]

  private val picNameRe = """pic(\d+).jpg""".r

  def vote(filename: String) = filename match {
    case picNameRe(ns) =>
      val n = ns.toInt
      pictureVotes = pictureVotes + ( n -> ( pictureVotes.getOrElse(n, 0) + 1 ) )
    case _ =>
      // NOOP
  }
}
