package io.github.tdhd.graphhopper

import java.io.File
import org.parboiled.common.FileUtils
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import spray.routing.{ HttpService, RequestContext }
import spray.routing.directives.CachingDirectives
import spray.can.server.Stats
import spray.can.Http
import spray.httpx.marshalling.Marshaller
import spray.httpx.encoding.Gzip
import spray.util._
import spray.http._
import MediaTypes._
import scala.collection.JavaConversions._
import com.graphhopper.{ GraphHopper, GHRequest }
import com.graphhopper.routing.util.EncodingManager
import scala.math.BigDecimal
import spray.json._
import scala.util.Try

/** custom JSON format for the routing service **/
case class GeoPosition(latitude: Double, longitude: Double)
case class Route(waypoints: List[GeoPosition])
object GeoJsonProtocol extends DefaultJsonProtocol {
  implicit val geoFormat = jsonFormat2(GeoPosition)
  implicit val routeFormat = jsonFormat1(Route)
}

import GeoJsonProtocol._

object GraphhopperRouteService { def props(osmFile: String, storageLocation: String) = Props(new GraphhopperRouteService(osmFile, storageLocation)) }

class GraphhopperRouteService(osmFile: String, storageLocation: String) extends Actor with HttpService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing,
  // timeout handling or alternative handler registration
  def receive = runRoute(serviceRoute)

  def round(d: Double, places: Int = 3) = BigDecimal(d).setScale(places, BigDecimal.RoundingMode.HALF_UP).toDouble

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  implicit def executionContext = actorRefFactory.dispatcher

  val hopper = new GraphHopper().forServer()
  hopper.setOSMFile(osmFile)
  hopper.setGraphHopperLocation(storageLocation)
  hopper.setEncodingManager(new EncodingManager("car"))
  // now this can take minutes if it imports or a few seconds for loading
  // of course this is dependent on the area you import
  hopper.importOrLoad()

  /**
   * method expects a string in the format latFrom,lonFrom,latTo,lonTo
   */
  def respondWithRoute(routeParams: String) = {
    val (latFrom, lonFrom, latTo, lonTo) = routeParams.split(",").map(_.toDouble) match {
      case Array(a, b, c, d) => (a, b, c, d)
    }
    // simple configuration of the request object, see the GraphHopperServlet classs for more possibilities.
    val req = new GHRequest(latFrom, lonFrom, latTo, lonTo)
      .setWeighting("fastest")
      .setVehicle("car")
    //.setLocale(Locale.US)
    val rsp = hopper.route(req)

    // first check for errors
    if (rsp.hasErrors()) {
      // handle them!
      rsp.getErrors().toList.foreach { println }
    }

    // points, distance in meters and time in millis of the full path
    val pointList = rsp.getPoints()
    Route(pointList.iterator().toList.map(p => GeoPosition(round(p.getLat), round(p.getLon)))).toJson.toString
  }

  val serviceRoute = compressResponse() {
    get {
      pathSingleSlash {
        complete(index)
      } ~
        path("route" / Rest) { routeParams =>
          respondWithMediaType(`application/json`) {
            val response = Try(respondWithRoute(routeParams)).getOrElse("")
            complete(response)
          }
        } ~
        path("stats") {
          complete {
            actorRefFactory.actorSelection("/user/IO-HTTP/listener-0")
              .ask(Http.GetStats)(1.second)
              .mapTo[Stats]
          }
        }
    } ~
      (post | parameter('method ! "post")) {
        path("stop") {
          complete {
            in(1.second) { actorSystem.shutdown() }
            "Shutting down in 1 second..."
          }
        }
      }
  }

  lazy val index =
    <html>
      <body>
        <h1>Say hello to <i>Sprayed Graphhopper</i>!</h1>
        <p>Defined resources:</p>
        <ul>
          <li><a href="/stats">/stats</a></li>
          <li><a href="/route/52.5,13.3,52.4,13.2">/route sample</a></li>
          <li><a href="/stop?method=post">/stop</a></li>
        </ul>
      </body>
    </html>

  implicit val statsMarshaller: Marshaller[Stats] =
    Marshaller.delegate[Stats, String](ContentTypes.`text/plain`) { stats =>
      "Uptime                : " + stats.uptime.formatHMS + '\n' +
        "Total requests        : " + stats.totalRequests + '\n' +
        "Open requests         : " + stats.openRequests + '\n' +
        "Max open requests     : " + stats.maxOpenRequests + '\n' +
        "Total connections     : " + stats.totalConnections + '\n' +
        "Open connections      : " + stats.openConnections + '\n' +
        "Max open connections  : " + stats.maxOpenConnections + '\n' +
        "Requests timed out    : " + stats.requestTimeouts + '\n'
    }

  def in[U](duration: FiniteDuration)(body: => U): Unit =
    actorSystem.scheduler.scheduleOnce(duration)(body)
}
