package ipogudin

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ipogudin.Crawler.crawl

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

object App {
//curl -vvv -X POST "http://localhost:8080/crawler" -H "Content-Type: application/json" -d '{"urls": {"ya": "https://ya.ru"}, "childrenLevel": 1, "childrenPattern": ".*yandex\\.ru.*"}'
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "lowlevel")
    implicit val executionContext: ExecutionContext = system.executionContext
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import JsonFormats._

    val routes: Route = pathPrefix("crawler") {
            post {
              entity(as[CrawlingRequest]) { request =>
                onSuccess(crawl(request)) { response =>
                  complete(response)
                }
              }
            }
    }
    startHttpServer(routes)
  }

}
