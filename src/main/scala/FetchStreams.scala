import monix.execution.Scheduler.Implicits.global
import sttp.client4.*
import sttp.client4.circe.*
import sttp.client4.httpclient.monix.HttpClientMonixBackend
import io.circe.generic.auto.*

// ----------------------------------------------------------------

object FetchStreams {
  case class Stream(
    id: String,
    user_id: String,
    user_login: String,
    user_name: String,
    game_id: String,
    game_name: String,
    `type`: String,
    title: String,
    tags: List[String],
    viewer_count: Int,
    started_at: String,
    language: String,
    thumbnail_url: String,
    tag_ids: List[String],
    is_mature: Boolean
  )

  case class Pagination(cursor: String)

  case class StreamResponse(data: List[Stream], pagination: Pagination)

  def fetchStreams(clientId: String, apiKey: String): Either[String, List[Stream]] = {
    val request: Request[Either[ResponseException[String, io.circe.Error], StreamResponse]] = basicRequest
      .get(uri"https://api.twitch.tv/helix/streams?first=100")
      .header("Authorization", s"Bearer $apiKey")
      .header("Client-Id", clientId)
      .response(asJson[StreamResponse])

    HttpClientMonixBackend.resource().use { backend =>
      request.send(backend).map { response =>
        response.body match {
          case Right(streamResponse) => Right(streamResponse.data)
          case Left(error) => Left(s"Error fetching streams: ${error.getMessage}")
        }
      }
    }.runSyncUnsafe()
  }
}
