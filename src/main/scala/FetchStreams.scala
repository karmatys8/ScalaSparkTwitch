import monix.execution.Scheduler.Implicits.global
import sttp.client4.*
import sttp.client4.circe.*
import sttp.client4.httpclient.monix.HttpClientMonixBackend
import io.circe.generic.auto.*
import scala.math.min

// ----------------------------------------------------------------

object FetchStreams {
  import FetchMoreData.{ Pagination, FetchResponse }

  // ----------------------------------------------------------------

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

  def fetchStreams(
    clientId: String,
    apiKey: String,
    amount: Int,
    prevPage: String = ""
    ): Either[String, FetchResponse[Stream]] = {
    var queryParameters = s"?first=$amount"
    if (prevPage != "") {
      queryParameters += s"&after=$prevPage"
    }
    
    val request: Request[
        Either[
          ResponseException[String, io.circe.Error],
          FetchResponse[Stream]
        ]
      ] = basicRequest
      .get(uri"https://api.twitch.tv/helix/streams?$queryParameters")
      .header("Authorization", s"Bearer $apiKey")
      .header("Client-Id", clientId)
      .response(asJson[FetchResponse[Stream]])

    HttpClientMonixBackend.resource().use { backend =>
      request.send(backend).map { response =>
        response.body match {
          case Right(streamResponse: FetchResponse[Stream]) => Right(streamResponse)
          case Left(error) => Left(s"Error fetching streams: ${error.getMessage}")
        }
      }
    }.runSyncUnsafe()
  }
}
