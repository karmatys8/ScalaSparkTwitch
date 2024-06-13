import scala.collection.mutable

// ----------------------------------------------------------------

object App {
  import AppConfig.loadConfig
  import TwitchConfig.*
  import FetchUsers.{ UserResponse, fetchUsers }
  import FetchGames.{ GameResponse, fetchGames }
  import FetchStreams.{ StreamResponse, fetchStreams }
  
  case class GameInfo(streamers: Int, viewers: Int)

  // ----------------------------------------------------------------

  def main(args: Array[String]): Unit = {
    val config = loadConfig()

    config match {
      case Right(TwitchConfig(clientId, secret, apiKey)) => {
        fetchUsers(clientId, apiKey) match {
          case Right(users) => users.foreach { user =>
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

        fetchGames(clientId, apiKey) match {
          case Right(games) => games.foreach { game =>
            gamesStatistics += (game.name -> GameInfo(0, 0))
          }
          case Left(error) => println(error)
        }

        fetchStreams(clientId, apiKey) match {
          case Right(streams) => streams.foreach { stream =>
            gamesStatistics.get(stream.game_name) match {
              case Some(info) =>
                gamesStatistics.update(stream.game_name, GameInfo(info.streamers + 1, info.viewers + stream.viewer_count))
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
