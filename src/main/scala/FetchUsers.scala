import monix.execution.Scheduler.Implicits.global
import sttp.client4.*
import sttp.client4.circe.*
import sttp.client4.httpclient.monix.HttpClientMonixBackend
import io.circe.generic.auto.*

// ----------------------------------------------------------------

object FetchUsers {
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

  def fetchUsers(clientId: String, apiKey: String): Either[String, List[User]] = {
    val request: Request[Either[ResponseException[String, io.circe.Error], UserResponse]] = basicRequest
      .get(uri"https://api.twitch.tv/helix/users?login=twitchdev")
      .header("Authorization", s"Bearer $apiKey")
      .header("Client-Id", clientId)
      .response(asJson[UserResponse])

    HttpClientMonixBackend.resource().use { backend =>
      request.send(backend).map { response =>
        response.body match {
          case Right(userResponse) => Right(userResponse.data)
          case Left(error) => Left(s"Error fetching users: ${error.getMessage}")
        }
      }
    }.runSyncUnsafe()
  }
}
