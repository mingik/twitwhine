import actors.TwitterStreamer
import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe, TestKit, ImplicitSender}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike, Matchers}
import play.api.Configuration
import play.api.libs.ws.WSClient

/**
 * Created by mintik on 4/10/16.
 */
class ActorsSpec extends TestKit(ActorSystem("ActorsSpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory  {

  "TweeterStreaamer" should {
    "reply with credentials missing message if no configuration was provided" in {
      val mockConfiguration = Configuration.empty
      val wsClient = mock[WSClient]
      val probe = TestProbe()
      val twitterStreamerActor = TestActorRef(new TwitterStreamer(mockConfiguration, wsClient, probe.ref))
      twitterStreamerActor ! "subscribe"
      twitterStreamerActor ! "twitter"
      probe.expectMsg("Twitter credentials missing")
    }
  }
}
