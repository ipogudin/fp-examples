package ipogudin

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormats  {
  import DefaultJsonProtocol._

  implicit val crawlingRequestJsonFormat = jsonFormat3(CrawlingRequest)
  implicit val singleResponse: RootJsonFormat[SingleResponse] = rootFormat(lazyFormat(jsonFormat7(SingleResponse)))
  implicit val crawlingResponse: RootJsonFormat[CrawlingResponse] = rootFormat(lazyFormat(jsonFormat(CrawlingResponse, "responses")))

}
