import monix.execution.Scheduler.Implicits.global
import sttp.client4.*
import sttp.client4.circe.*
import sttp.client4.httpclient.monix.HttpClientMonixBackend
import io.circe.generic.auto.*
import scala.math.min

// ----------------------------------------------------------------

object FetchGames {
  val MAX_GAMES_PER_FETCH = 100

  case class Game(
    id: String,
    name: String,
    box_art_url: String,
    igdb_id: String
  )

  case class Pagination(cursor: String)

  case class GameResponse(data: List[Game], pagination: Pagination)

  def fetchGames(clientId: String, apiKey: String, amount: Int, prevPage: String): Either[String, GameResponse] = {
    var queryParameters = s"?first=$amount"
    if (prevPage != "") {
      queryParameters += s"&after=$prevPage"
    }
    
    val request: Request[Either[ResponseException[String, io.circe.Error], GameResponse]] = basicRequest
      .get(uri"https://api.twitch.tv/helix/games/top?$queryParameters")
      .header("Authorization", s"Bearer $apiKey")
      .header("Client-Id", clientId)
      .response(asJson[GameResponse])

    HttpClientMonixBackend.resource().use { backend =>
      request.send(backend).map { response =>
        response.body match {
          case Right(gameResponse: GameResponse) => Right(gameResponse)
          case Left(error) => Left(s"Error fetching games: ${error.getMessage}")
        }
      }
    }.runSyncUnsafe()
  }

  def fetchMoreGames(clientId: String, apiKey: String, amount: Int): Either[String, List[Game]] = {
    var gamesList: List[Game] = List.empty[Game]
    var toFetch: Int = amount
    var didErrorOccur: Boolean = false
    var prevPage: String = ""

    while (toFetch > 0 && !didErrorOccur) {
      fetchGames(clientId, apiKey, min(toFetch, MAX_GAMES_PER_FETCH), prevPage) match {
        case Right(gameResponse: GameResponse) => {
          prevPage = gameResponse.pagination.cursor
          gamesList = gamesList ++ gameResponse.data
        }
        case Left(error) => {
          didErrorOccur = true
          return Left(error)
        }
      }

      toFetch -= 100
    }

    Right(gamesList)
  }
}
