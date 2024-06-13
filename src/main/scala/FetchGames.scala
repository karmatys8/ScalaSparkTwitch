import monix.execution.Scheduler.Implicits.global
import sttp.client4.*
import sttp.client4.circe.*
import sttp.client4.httpclient.monix.HttpClientMonixBackend
import io.circe.generic.auto.*

// ----------------------------------------------------------------

object FetchGames {
  case class Game(
    id: String,
    name: String,
    box_art_url: String,
    igdb_id: String
  )

  case class Pagination(cursor: String)

  case class GameResponse(data: List[Game], pagination: Pagination)

  def fetchGames(clientId: String, apiKey: String): Either[String, List[Game]] = {
    val request: Request[Either[ResponseException[String, io.circe.Error], GameResponse]] = basicRequest
      .get(uri"https://api.twitch.tv/helix/games/top")
      .header("Authorization", s"Bearer $apiKey")
      .header("Client-Id", clientId)
      .response(asJson[GameResponse])

    HttpClientMonixBackend.resource().use { backend =>
      request.send(backend).map { response =>
        response.body match {
          case Right(gameResponse) => Right(gameResponse.data)
          case Left(error) => Left(s"Error fetching games: ${error.getMessage}")
        }
      }
    }.runSyncUnsafe()
  }
}
