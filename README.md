# Sprayed Graphhopper Routing Server

This server uses a simple setup of graphhopper (http://graphhopper.com) and exposes its functionality via spray (http://spray.io).

# How to run

- `git clone https://github.com/tdhd/sprayed-graphhopper.git`
- `cd sprayed-graphhopper`
- `sbt assembly`
- `wget http://download.geofabrik.de/europe/germany/berlin-latest.osm.pbf -P /tmp`
- `java -jar target/scala-2.11/sprayedgraphhopper-assembly-0.1-SNAPSHOT.jar -o /tmp/berlin-latest.osm.pbf -s /tmp/`

Go to localhost:8080 and enjoy.

