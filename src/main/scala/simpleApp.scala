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
    val logFile = "text.txt" // Should be some file on your system
    val spark = SparkSession.builder
      .appName("Simple Application")
      .master("local[*]")
      .getOrCreate()
    val logData = spark.read.textFile(logFile).cache()

    val numAs = logData.filter(_.contains("a")).count()
    val numBs = logData.filter(_.contains("b")).count()

    println(s"Lines with a: $numAs, Lines with b: $numBs")
    spark.close()


    case class UserData(
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

    case class TwitchResponse(data: List[UserData])


    sealed trait AppConf derives ConfigReader
    case class TwitchConf(clientId: String, twitchSecret: String, twitchApiKey: String) extends AppConf

    val config = ConfigSource.file(".env").load[AppConf]

    
    config match {
      case Right(conf) => {
        conf match {
          case TwitchConf(clientId, twitchSecret, twitchApiKey) => {
            val request: Request[TwitchResponse] = basicRequest
              .get(uri"https://api.twitch.tv/helix/users?login=twitchdev")
              .header("Authorization", s"Bearer $twitchApiKey")
              .header("Client-Id", clientId)
              .response(asJson[TwitchResponse].getRight)

            HttpClientMonixBackend
              .resource()
              .use { backend =>
                request.send(backend).map {(response: Response[TwitchResponse]) => {
                  println(s"Got response code: ${response.code}")
                  response.body.data.foreach { (user: UserData) => {
                    println(s"User: ${user.display_name}")
                    println(s"ID: ${user.id}")
                    println(s"Login: ${user.login}")
                    println(s"Type: ${user.`type`}")
                    println(s"Broadcaster Type: ${user.broadcaster_type}")
                    println(s"Description: ${user.description}")
                    println(s"Profile Image URL: ${user.profile_image_url}")
                    println(s"Offline Image URL: ${user.offline_image_url}")
                    println(s"View Count: ${user.view_count}")
                    println(s"Created At: ${user.created_at}")
                    println("---------------------------------------------------")
                  }}
                }}
              }
              .runSyncUnsafe()
          }
        }
      }
      case Left(error) => println(s"Error loading configuration: $error")
    }
  }
}
