import actors.TwitterStreamer
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.testkit.{TestActorRef, TestProbe, TestKit, ImplicitSender}
import akka.util.ByteString
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike, Matchers}
import play.api.Configuration
import play.api.libs.concurrent.Promise
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}
import play.api.libs.ws.{WSResponseHeaders, StreamedResponse, WSRequest, WSClient}

import scala.concurrent.Future

/**
 * Created by mintik on 4/10/16.
 */
class ActorsSpec extends TestKit(ActorSystem("ActorsSpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory {

  "TweeterStreaamer" should {
    "reply with credentials missing message if no configuration was provided" in {
      val mockConfiguration = Configuration.empty
      val wsClient = stub[WSClient]
      val probe = TestProbe()
      val twitterStreamerActor = TestActorRef(new TwitterStreamer(mockConfiguration, wsClient, probe.ref))
      twitterStreamerActor ! "subscribe"
      twitterStreamerActor ! "twitter"
      probe.expectMsg("Twitter credentials missing")
    }

    "reply with 'Stream is closed' message if configuration was valid but source is not a valid JSON" in {
      import scala.concurrent.ExecutionContext.Implicits.global
      val validConfiguration = Configuration(
        ("twitter.apiKey","apikey"),("twitter.apiSecret","apisecret"),
        ("twitter.token","token"), ("twitter.tokenSecret","secret"))
      val wsClient = stub[WSClient]
      val wsRequest = stub[WSRequest]
      (wsClient.url _).when("https://api.twitter.com/1.1/search/tweets.json").returns(wsRequest)
      (wsRequest.sign _).when(*).returns(wsRequest)
      (wsRequest.withQueryString _).when(List(("q", "twitter")).toSeq).returns(wsRequest)
      (wsRequest.withMethod _).when("GET").returns(wsRequest)
      val headers = stub[WSResponseHeaders]
      val streamedResponse = Future { StreamedResponse(headers, Source.single(ByteString("Hello World"))) }
      (wsRequest.stream _).when().returns(streamedResponse)
      val probe = TestProbe()
      val twitterStreamerActor = TestActorRef(new TwitterStreamer(validConfiguration, wsClient, probe.ref))
      twitterStreamerActor ! "subscribe"
      twitterStreamerActor ! "twitter"

      probe.expectMsg("Stream is closed")
    }

    "reply with correct messages if configuration was valid and source is valid JSON" in {
      import scala.concurrent.ExecutionContext.Implicits.global
      val validConfiguration = Configuration(
        ("twitter.apiKey","apikey"),("twitter.apiSecret","apisecret"),
        ("twitter.token","token"), ("twitter.tokenSecret","secret"))
      val wsClient = stub[WSClient]
      val wsRequest = stub[WSRequest]
      (wsClient.url _).when("https://api.twitter.com/1.1/search/tweets.json").returns(wsRequest)
      (wsRequest.sign _).when(*).returns(wsRequest)
      (wsRequest.withQueryString _).when(List(("q", "twitter")).toSeq).returns(wsRequest)
      (wsRequest.withMethod _).when("GET").returns(wsRequest)
      val headers = stub[WSResponseHeaders]
      val streamedResponse = Future { StreamedResponse(headers, Source.single(ByteString("{\"hello\": 33}"))) }
      (wsRequest.stream _).when().returns(streamedResponse)
      val probe = TestProbe()
      val twitterStreamerActor = TestActorRef(new TwitterStreamer(validConfiguration, wsClient, probe.ref))
      twitterStreamerActor ! "subscribe"
      twitterStreamerActor ! "twitter"

      probe.expectMsg("{\"hello\": 33}")
    }
  }
}
