package controllers

import javax.inject.Inject

import actors.TwitterStreamer
import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import akka.stream.Materializer
import play.Logger
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.{Logger, Configuration}
import play.api.libs.oauth.{OAuthCalculator, RequestToken, ConsumerKey}
import play.api.libs.ws.{StreamedResponse, WSClient}
import play.api.mvc.{WebSocket, Action, Controller}

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by mintik on 4/5/16.
 */
class TweeterController @Inject()
(configuration: Configuration, ws: WSClient)
(implicit system: ActorSystem, materializer: Materializer, exec: ExecutionContext) extends Controller {
  def tweetsWS = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => TwitterStreamer.props(configuration, ws, out))
  }

  def tweets = Action { implicit request =>
    Ok(views.html.tweets("Tweets"))
  }
}