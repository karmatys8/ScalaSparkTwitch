import scala.collection.mutable

import org.apache.spark.sql.SparkSession

import monix.execution.Scheduler.Implicits.global
import sttp.client4.httpclient.monix.HttpClientMonixBackend

import sttp.client4.*
import sttp.client4.circe.*
import io.circe.generic.auto.*

import pureconfig.*
import pureconfig.generic.derivation.default.*

// ----------------------------------------------------------------

object SimpleApp {
  def main(args: Array[String]): Unit = {
    case class User(
      broadcaster_type: String,
      created_at: String,
      description: String,
      display_name: String,
      id: String,
      login: String,
      offline_image_url: String,
      profile_image_url: String,
      `type`: String,
      view_count: Int
    )

    case class UserResponse(data: List[User])


    sealed trait AppConfig derives ConfigReader
    case class TwitchConfig(clientId: String, secret: String, apiKey: String) extends AppConfig

    val config = ConfigSource.file(".env").load[AppConfig]

    
    config match {
      case Right(TwitchConfig(clientId, secret, apiKey)) => {
        val request: Request[UserResponse] = basicRequest
          .get(uri"https://api.twitch.tv/helix/users?login=twitchdev")
          .header("Authorization", s"Bearer $apiKey")
          .header("Client-Id", clientId)
          .response(asJson[UserResponse].getRight)

        HttpClientMonixBackend
          .resource()
          .use { backend =>
            request.send(backend).map {(response: Response[UserResponse]) => {
              println(s"Got response code: ${response.code}")
              response.body.data.foreach { (user: User) => {
                println(s"User: ${user.display_name}")
                println(s"ID: ${user.id}")
                println(s"Login: ${user.login}")
                println(s"Type: ${user.`type`}")
                println(s"Broadcaster Type: ${user.broadcaster_type}")
                println(s"Description: ${user.description}")
                println(s"View Count: ${user.view_count}")
                println(s"Created At: ${user.created_at}")
                println("---------------------------------------------------")
              }}
            }}
          }
          .runSyncUnsafe()
      }
      case Left(error) => println(s"Error loading configuration: $error")
    }
      
    
    case class Game(
      id: String,
      name: String,
      box_art_url: String,
      igdb_id: String
    )

    case class Pagination(
      cursor: String
    )

    case class GameResponse(
      data: List[Game],
      pagination: Pagination
    )

    case class GameInfo(streamers: Int, viewers: Int);
    val gamesStatistics: mutable.Map[String, GameInfo] = mutable.Map.empty[String, GameInfo];
    
    config match {
      case Right(TwitchConfig(clientId, secret, apiKey)) => {
        val request: Request[GameResponse] = basicRequest
          .get(uri"https://api.twitch.tv/helix/games/top")
          .header("Authorization", s"Bearer $apiKey")
          .header("Client-Id", clientId)
          .response(asJson[GameResponse].getRight)

        HttpClientMonixBackend
          .resource()
          .use { backend =>
            request.send(backend).map {(response: Response[GameResponse]) => {
              println(s"Got response code: ${response.code}");
              response.body.data.foreach { (game: Game) => {
                gamesStatistics += (game.name -> GameInfo(0, 0));
              }}
            }}
          }
          .runSyncUnsafe()
      }
      case Left(error) => println(s"Error loading configuration: $error")
    }


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

    case class StreamResponse(
      data: List[Stream],
      pagination: Pagination
    )

    config match {
      case Right(TwitchConfig(clientId, secret, apiKey)) => {
        val request: Request[StreamResponse] = basicRequest
          .get(uri"https://api.twitch.tv/helix/streams?first=100")
          .header("Authorization", s"Bearer $apiKey")
          .header("Client-Id", clientId)
          .response(asJson[StreamResponse].getRight)

        HttpClientMonixBackend
          .resource()
          .use { backend =>
            request.send(backend).map {(response: Response[StreamResponse]) => {
              println(s"Got response code: ${response.code}")
              response.body.data.foreach { (stream: Stream) => {
                gamesStatistics.get(stream.game_name) match {
                  case Some(info) => gamesStatistics.update(stream.game_name,
                    GameInfo(info.streamers + 1, info.viewers + stream.viewer_count));
                  case None => // Key not found, do nothing
                }
              }}
            }}
          }
          .runSyncUnsafe()
      }
      case Left(error) => println(s"Error loading configuration: $error")
    }


    gamesStatistics.foreach { case (gameName, gameInfo) =>
      println(s"Game: $gameName, Streamers: ${gameInfo.streamers}, Viewers: ${gameInfo.viewers}")
    }
  }
}
