package controllers

import javax.inject._

import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.http.Status
import play.api.libs.iteratee.Iteratee
import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.{StreamedResponse, WSClient}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(configuration: Configuration, ws: WSClient)(implicit val mat: Materializer, exec: ExecutionContext) extends Controller {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def tweets = Action.async {
    val credentials: Option[(ConsumerKey, RequestToken)] = tweeterCredentials()

    credentials.map { case (consumerKey, requestToken) =>
      ws
        .url("https://stream.twitter.com/1.1/statuses/filter.json")
        .sign(OAuthCalculator(consumerKey, requestToken))
        .withQueryString("track" -> "Twitter")
        .withMethod("POST").stream()
        .map(loggingStreamConsumer(_))
        .map(_ => Ok("Stream closed"))
    } getOrElse {
      Future { InternalServerError("Twitter credentials missing") }
    }
  }

  private def loggingStreamConsumer(streamedResponse: StreamedResponse) = {
    Logger.info("Status: " + streamedResponse.headers.status)
    streamedResponse.body.runForeach(byteString => Logger.info(byteString.utf8String))
  }


  private def tweeterCredentials(): Option[(ConsumerKey, RequestToken)] = for {
    apiKey <- configuration.getString("twitter.apiKey")
    apiSecret <- configuration.getString("twitter.apiSecret")
    token <- configuration.getString("twitter.token")
    tokenSecret <- configuration.getString("twitter.tokenSecret")
  } yield (ConsumerKey(apiKey, apiSecret), RequestToken(token, tokenSecret))
}
