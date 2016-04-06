package controllers

import javax.inject.Inject

import actors.TwitterStreamer
import akka.actor.{Actor, ActorRef, Props}
import akka.stream.Materializer
import play.Logger
import play.api.Logger
import play.api.libs.json.JsValue
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
(implicit val exec: ExecutionContext) extends Controller {
  // TODO: remove this deprectated method
  def tweets = WebSocket.acceptWithActor[String, JsValue] {
    request => out => TwitterStreamer.props(configuration, ws, out)
  }
}