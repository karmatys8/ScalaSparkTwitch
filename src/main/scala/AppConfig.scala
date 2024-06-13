import pureconfig.*
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.derivation.default.*

// ----------------------------------------------------------------

sealed trait AppConfig derives ConfigReader
case class TwitchConfig(clientId: String, secret: String, apiKey: String) extends AppConfig

object AppConfig {
  def loadConfig(): Either[ConfigReaderFailures, AppConfig] = {
    ConfigSource.file(".env").load[AppConfig]
  }
}
