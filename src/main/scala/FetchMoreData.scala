import scala.math.min

// ----------------------------------------------------------------

object FetchMoreData {
  val MAX_PER_FETCH = 100

  case class Pagination(cursor: String)

  case class FetchResponse[T](data: List[T], pagination: Pagination)

  type FetchFunction[T] = (String, String, Int, String) => Either[String, FetchResponse[T]]

  def fetchMoreData[T](
    fetchFunction: FetchFunction[T],
    clientId: String,
    apiKey: String,
    amount: Int
  ): Either[String, List[T]] = {
    var dataList: List[T] = List.empty[T]
    var toFetch: Int = amount
    var didErrorOccur: Boolean = false
    var prevPage: String = ""

    while (toFetch > 0 && !didErrorOccur) {
      fetchFunction(clientId, apiKey, min(toFetch, MAX_PER_FETCH), prevPage) match {
        case Right(response: FetchResponse[T]) => {
          prevPage = response.pagination.cursor
          dataList = dataList ++ response.data
        }
        case Left(error) => {
          didErrorOccur = true
          return Left(error)
        }
      }

      toFetch -= MAX_PER_FETCH
    }

    Right(dataList)
  }
}
