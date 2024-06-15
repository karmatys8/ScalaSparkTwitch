import scala.collection.mutable
import scala.util.Try

// ----------------------------------------------------------------

object App {
  import AppConfig.loadConfig
  import TwitchConfig.*
  import FetchUsers.{ User, fetchUsers }
  import FetchGames.{ Game, fetchGames }
  import FetchStreams.{ Stream, fetchStreams }
  import FetchMoreData.fetchMoreData
  
  case class GameInfo(streamers: Int, viewers: Int)

  // ----------------------------------------------------------------

  def main(args: Array[String]): Unit = {
    if (args.length > 2) {
      throw new IllegalArgumentException("To many arguments provided.")
    }

    val gamesToFetch: Int = Try(args(0).toInt)
      .getOrElse(throw new IllegalArgumentException("First argument is not an integer"))
    val streamsToFetch: Int = Try(args(1).toInt)
      .getOrElse(throw new IllegalArgumentException("Second argument is not an integer"))


    val config = loadConfig()

    config match {
      case Right(TwitchConfig(clientId, secret, apiKey)) => {
        fetchUsers(clientId, apiKey) match {
          case Right(users: List[User]) => users.foreach { (user: User) =>
            println("---------------------------------------------------")
            println(s"User: ${user.display_name}")
            println(s"Login: ${user.login}")
            println(s"Description: ${user.description}")
            println(s"View Count: ${user.view_count}")
            println(s"Created At: ${user.created_at}")
            println("---------------------------------------------------")
          }
          case Left(error) => println(error)
        }

        val gamesStatistics: mutable.Map[String, GameInfo] = mutable.Map.empty[String, GameInfo]

        fetchMoreData(fetchGames, clientId, apiKey, gamesToFetch) match {
          case Right(games: List[Game]) => games.foreach { (game: Game) =>
            gamesStatistics += (game.name -> GameInfo(0, 0))
          }
          case Left(error) => println(error)
        }

        fetchMoreData(fetchStreams, clientId, apiKey, gamesToFetch) match {
          case Right(streams: List[Stream]) => streams.foreach { (stream: Stream) =>
            gamesStatistics.get(stream.game_name) match {
              case Some(info) =>
                gamesStatistics.update(stream.game_name,
                  GameInfo(info.streamers + 1, info.viewers + stream.viewer_count))
              case None => // Key not found, do nothing
            }
          }
          case Left(error) => println(error)
        }

        gamesStatistics.foreach { case (gameName, gameInfo) =>
          println(s"Game: $gameName, Streamers: ${gameInfo.streamers}, Viewers: ${gameInfo.viewers}")
        }
      }
      case Left(error) => println(s"Error loading configuration: $error")
    }
  }
}
