import monix.execution.Scheduler.Implicits.global
import sttp.client4.*
import sttp.client4.circe.*
import sttp.client4.httpclient.monix.HttpClientMonixBackend
import io.circe.generic.auto.*
import scala.math.min

// ----------------------------------------------------------------

object FetchGames {
  import FetchMoreData.{ Pagination, FetchResponse }

  // ----------------------------------------------------------------

  case class Game(
    id: String,
    name: String,
    box_art_url: String,
    igdb_id: String
  )

  def fetchGames(
    clientId: String,
    apiKey: String,
    amount: Int,
    prevPage: String = ""
  ): Either[String, FetchResponse[Game]] = {
    var queryParameters = s"?first=$amount"
    if (prevPage != "") {
      queryParameters += s"&after=$prevPage"
    }
    
    val request: Request[
        Either[
          ResponseException[String, io.circe.Error],
          FetchResponse[Game]
        ]
      ] = basicRequest
      .get(uri"https://api.twitch.tv/helix/games/top?$queryParameters")
      .header("Authorization", s"Bearer $apiKey")
      .header("Client-Id", clientId)
      .response(asJson[FetchResponse[Game]])

    HttpClientMonixBackend.resource().use { backend =>
      request.send(backend).map { response =>
        response.body match {
          case Right(gameResponse: FetchResponse[Game]) => Right(gameResponse)
          case Left(error) => Left(s"Error fetching games: ${error.getMessage}")
        }
      }
    }.runSyncUnsafe()
  }
}
