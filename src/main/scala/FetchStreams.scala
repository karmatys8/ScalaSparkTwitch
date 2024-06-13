import monix.execution.Scheduler.Implicits.global
import sttp.client4.*
import sttp.client4.circe.*
import sttp.client4.httpclient.monix.HttpClientMonixBackend
import io.circe.generic.auto.*
import scala.math.min

// ----------------------------------------------------------------

object FetchStreams {
  val MAX_STREAMS_PER_FETCH = 100

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

  def fetchStreams(clientId: String, apiKey: String, amount: Int, prevPage: String): Either[String, StreamResponse] = {
    var queryParameters = s"?first=$amount"
    if (prevPage != "") {
      queryParameters += s"&after=$prevPage"
    }
    
    val request: Request[Either[ResponseException[String, io.circe.Error], StreamResponse]] = basicRequest
      .get(uri"https://api.twitch.tv/helix/streams?$queryParameters")
      .header("Authorization", s"Bearer $apiKey")
      .header("Client-Id", clientId)
      .response(asJson[StreamResponse])

    HttpClientMonixBackend.resource().use { backend =>
      request.send(backend).map { response =>
        response.body match {
          case Right(streamResponse: StreamResponse) => Right(streamResponse)
          case Left(error) => Left(s"Error fetching streams: ${error.getMessage}")
        }
      }
    }.runSyncUnsafe()
  }

  def fetchMoreStreams(clientId: String, apiKey: String, amount: Int): Either[String, List[Stream]] = {
    var streamsList: List[Stream] = List.empty[Stream]
    var toFetch: Int = amount
    var didErrorOccur: Boolean = false
    var prevPage: String = ""

    while (toFetch > 0 && !didErrorOccur) {
      fetchStreams(clientId, apiKey, min(toFetch, MAX_STREAMS_PER_FETCH), prevPage) match {
        case Right(streamResponse: StreamResponse) => {
          prevPage = streamResponse.pagination.cursor
          streamsList = streamsList ++ streamResponse.data
        }
        case Left(error) => {
          didErrorOccur = true
          return Left(error)
        }
      }

      toFetch -= 100
    }

    Right(streamsList)
  }
}
