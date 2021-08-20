package ipogudin

import java.nio.charset.StandardCharsets

import scala.language.postfixOps
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.duration.{DurationInt}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import org.jsoup.Jsoup
import scala.jdk.CollectionConverters._

final case class CrawlingRequest(urls: Map[String, String], childrenPattern: String, childrenLevel: Int)

final case class SingleResponse(
                           successful: Boolean,
                           code: Option[Int],
                           message: String,
                           headers: Map[String, String],
                           body: Option[String],
                           links: List[String],
                           children: Option[CrawlingResponse]
                         )

final case class CrawlingResponse(responses: Map[String, SingleResponse]) extends AnyVal

object Crawler {

  def crawl(request: CrawlingRequest)(implicit executor: ExecutionContext, system: ActorSystem[Nothing]): Future[CrawlingResponse] = {
    Future.sequence(
      request
        .urls
        .map(prepareRequestContext)
        .map {
          case (id, httpRequest) =>
            for {
              response <- Http().singleRequest(httpRequest).transformWith(parseResponse(httpRequest.getUri.getScheme))
              childrenResponse <- handleChildren(request.childrenPattern, request.childrenLevel, response)
            } yield (id, childrenResponse)
        }
    ).map(responses => CrawlingResponse(responses = responses.toMap))
  }

  def extractLinks(parentUrlSchema: String)(body: String): List[String] =
      Jsoup.parse(body)
        .select("a").asScala
        .map(_.attr("href"))
        .map(processParentUrlSchema(parentUrlSchema))
        .toList

  def processParentUrlSchema(parentUrlSchema: String)(url: String): String =
    if (url.startsWith("//"))
      parentUrlSchema + ":" + url
    else
      url

  def toRequest(pattern: String, level: Int, response: SingleResponse): CrawlingRequest =
    CrawlingRequest(
      urls = response.links.filter(pattern.r.matches).map(l => (l, l)).toMap,
      childrenPattern = pattern,
      childrenLevel = level - 1
    )

  def handleChildren(pattern: String, level: Int, response: SingleResponse)(implicit executor: ExecutionContext, system: ActorSystem[Nothing]): Future[SingleResponse] =
    level match {
      case 0 => Future.apply(response)
      case _ => crawl(toRequest(pattern, level, response)).map(r => response.copy(children = Some(r)))
    }

  def prepareRequestContext(context: (String, String)): (String, HttpRequest) =
    (context._1, HttpRequest(uri = context._2))

  def parseResponse(parentUrlSchema: String)(t: Try[HttpResponse])(implicit executor: ExecutionContext, system: ActorSystem[Nothing]): Future[SingleResponse] = t match {
    case Success(httpResponse) =>
      httpResponse.entity.toStrict(30 seconds)
        .map {
          entity => {
            val body = entity
              .data
              .decodeString(
                httpResponse.entity
                  .getContentType()
                  .getCharsetOption
                  .map(_.nioCharset())
                  .orElse(StandardCharsets.UTF_8))

            SingleResponse(
              successful = true,
              code = Option.apply(httpResponse.status.intValue()),
              message = httpResponse.status.defaultMessage(),
              headers = httpResponse.headers.map(h => (h.name(), h.value())).toMap,
              body = Option.apply(body),
              links = extractLinks(parentUrlSchema)(body),
              children = Option.empty
            )
          }
        }
    case Failure(exception) => Future.apply(SingleResponse(
      successful = false,
      code = Option.empty,
      message = exception.getMessage,
      headers = Map.empty,
      body = Option.apply(exception.getLocalizedMessage()),
      links = List.empty,
      children = Option.empty
    ))
  }

}