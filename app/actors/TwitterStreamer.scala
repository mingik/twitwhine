package actors

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.stream.ActorMaterializer
import play.Logger
import play.api.Configuration
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.{StreamedResponse, WSClient}

import scala.concurrent.ExecutionContext
import scala.util.parsing.json.JSON

/**
 * Created by mintik on 4/5/16.
 */
class TwitterStreamer(configuration: Configuration, ws: WSClient, out: ActorRef) extends Actor {
  implicit val materializer = ActorMaterializer()
  import ExecutionContext.Implicits.global

  lazy val credentials: Option[(ConsumerKey, RequestToken)] = tweeterCredentials()

  override def receive: Receive = {
    // TODO: what if user sends 'subscribe' as a whine?
    case "subscribe" =>
      Logger.info("Received subscription from client")
      // TODO: evaluate credentials?
    case "stop" =>
      context.stop(self)
    case whine: String =>
      credentials.map { case (consumerKey, requestToken) =>
        ws
          .url("https://stream.twitter.com/1.1/statuses/filter.json")
          .sign(OAuthCalculator(consumerKey, requestToken))
          .withQueryString("track" -> whine)
          .withMethod("POST").stream()
          .map(loggingStreamConsumer(_))
      } getOrElse {
        out ! "Twitter credentials missing"
      }
  }

  private def loggingStreamConsumer(streamedResponse: StreamedResponse) = {
    Logger.debug("Status: " + streamedResponse.headers.status)
    streamedResponse.body
      .runForeach(byteString => {
        val str = byteString.utf8String
        // Make sure that server-side sends valid JSON string to the front-end
        JSON.parseRaw(str) match {
          case Some(validJsonStr) =>
            // TODO: filter out not relevant tweets?
            out ! str
          case _ => Logger.debug(s"string from stream is not a valid json: ${str}, skipped.")
        }})
      .onComplete {
        case _ => out ! "Stream is closed"
    }
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