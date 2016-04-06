package actors

import akka.actor.{Props, Actor, ActorRef}
import play.Logger
import play.api.Configuration
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.{StreamedResponse, WSClient}

/**
 * Created by mintik on 4/5/16.
 */
class TwitterStreamer(configuration: Configuration, ws: WSClient, out: ActorRef) extends Actor {
  override def receive: Receive = {
    case "subscribe" =>
      Logger.info("Received subscription from client")
      val credentials: Option[(ConsumerKey, RequestToken)] = tweeterCredentials()

      credentials.map { case (consumerKey, requestToken) =>
        ws
          .url("https://stream.twitter.com/1.1/statuses/filter.json")
          .sign(OAuthCalculator(consumerKey, requestToken))
          .withQueryString("track" -> "Twitter")
          .withMethod("POST").stream()
          .map(loggingStreamConsumer(_))
      } getOrElse {
        out ! "Twitter credentials missing"
      }
  }

  private def loggingStreamConsumer(streamedResponse: StreamedResponse) = {
    Logger.info("Status: " + streamedResponse.headers.status)
    streamedResponse.body.runForeach(byteString => {
      Logger.info(byteString.utf8String)
      out ! byteString.utf8String
    })
    out ! "Stream is closed"
  }


  private def tweeterCredentials(): Option[(ConsumerKey, RequestToken)] = for {
    apiKey <- configuration.getString("twitter.apiKey")
    apiSecret <- configuration.getString("twitter.apiSecret")
    token <- configuration.getString("twitter.token")
    tokenSecret <- configuration.getString("twitter.tokenSecret")
  } yield (ConsumerKey(apiKey, apiSecret), RequestToken(token, tokenSecret))
}

object TwitterStreamer {
  def props(configuration: Configuration, ws: WSClient, out: ActorRef) = Props(new TwitterStreamer(configuration, ws, out))
}