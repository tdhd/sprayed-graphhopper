package io.github.tdhd.graphhopper

import language.postfixOps
import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }
import scala.collection.JavaConversions._
import com.graphhopper.{ GraphHopper, GHRequest }
import com.graphhopper.routing.util.EncodingManager
import akka.actor.ActorSystem
import akka.io.IO
import spray.can.Http

case class GraphhopperConfig(osmFile: String = "", storageLocation: String = "", port: Int = 8080)

object SprayedGraphhopper {
  def main(args: Array[String]) = {
    val parser = new scopt.OptionParser[GraphhopperConfig]("SprayedGraphhopper") {
      head("SprayedGraphhopper", "0.1")
      opt[String]('o', "osm") required () valueName ("file.osm.pbf") action { (x, c) =>
        c.copy(osmFile = x)
      } text ("path to osm file")
      opt[String]('s', "storage") required () valueName ("/tmp/") action { (x, c) =>
        c.copy(storageLocation = x)
      } text ("path to storage location for graphhopper")
      opt[Int]('p', "port") valueName ("8080") action { (x, c) =>
        c.copy(port = x)
      } text ("TCP listen port")
      help("help") text ("print help")
    }

    parser.parse(args, GraphhopperConfig()).foreach {
      cmdLineConfig =>
        // we need an ActorSystem to host our application in
        implicit val system = ActorSystem("sprayedGraphhopper-system")
        // create and start our service actor
        val service = system.actorOf(GraphhopperRouteService.props(cmdLineConfig.osmFile, cmdLineConfig.storageLocation), "sprayedGraphhopper")
        // start a new HTTP server with GraphhopperRouteService as the actor
        IO(Http) ! Http.Bind(service, "localhost", port = cmdLineConfig.port)
    }
  }
}
