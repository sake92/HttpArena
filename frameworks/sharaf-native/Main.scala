//> using scala 3.10.0
//> using platform native
//> using nativeVersion 0.5.13-20261119-b703b41-SNAPSHOT
//> using repository https://central.sonatype.com/repository/maven-snapshots
//> using dep ba.sake::sharaf-potateh::0.24.0

import java.net.InetSocketAddress
import ba.sake.potateh.ServerConfig
import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.potateh.PotatehSharafServer
import ba.sake.tupson.*

case class Rating(score: Int, count: Int) derives JsonRW
case class Item(
    id: Int,
    name: String,
    category: String,
    price: Long,
    quantity: Long,
    active: Boolean,
    tags: Seq[String],
    rating: Rating,
    total: Long = 0
) derives JsonRW
case class Payload(items: Seq[Item], count: Int) derives JsonRW

case class SumQuery(a: Long = 0L, b: Long = 0L) derives QueryStringRW

object Main {

  def main(args: Array[String]): Unit = {
    val datasetPath = sys.env.getOrElse("DATASET_PATH", "/data/dataset.json")
    val datasetJson = scala.util.Using.resource(scala.io.Source.fromFile(datasetPath))(_.mkString)
    val dataset = datasetJson.parseJson[Seq[Item]]
    val routes = Routes {
      case GET -> Path("pipeline") => Response.withBody("ok")
      case GET -> Path("baseline11") => Response.withBody(querySum.toString)
      case POST -> Path("baseline11") =>
        val body = Request.current.bodyString.trim.toLongOption.getOrElse(0L)
        Response.withBody((querySum + body).toString)
      case GET -> Path("json", count) =>
        case class JsonQuery(m: Long = 1L) derives QueryStringRW
        val multiplier = Request.current.queryParams[JsonQuery].m
        val itemCount = count.toIntOption.getOrElse(0).max(0)
        val items = dataset.take(itemCount).map { item =>
          item.copy(total = item.price * item.quantity * multiplier)
        }
        val payload = Payload(items, items.size)
        Response.withBody(payload)

    }
    val config = ServerConfig.Default
      .withBindAddress(new InetSocketAddress("0.0.0.0", 8080))
      .withMaxConnections(32768)
      .withBacklog(8192)
    val routesHandler = SharafHandler.routes(routes)
    val gzipHandler = SharafHandler.gzip(routesHandler, GzipSettings.default.withLevel(1))
    val jsonCompression: SharafHandler = context =>
      if context.params.path.segments.headOption.contains("json") then gzipHandler.handle(context)
      else routesHandler.handle(context)
    val handler = SharafHandler.exceptions(jsonCompression, ExceptionMapper.default)
    PotatehSharafServer(handler, "0.0.0.0", 8080).withConfig(config).start()
  }

  private def querySum(using Request): Long = {
    val query = Request.current.queryParams[SumQuery]
    query.a + query.b
  }

}
