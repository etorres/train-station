package es.eriktorr.train_station

import arrival.Arrivals
import arrival.Arrivals.{Arrival, ArrivalError}
import circe.EventJsonProtocol

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl

object TrainControlPanelRoutes extends EventJsonProtocol {
  def arrivalRoutes[F[_]: Sync](A: Arrivals[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "arrival" =>
        for {
          arrival <- req.as[Arrival]
          result <- A.register(arrival)
          response <- result.fold(
            error =>
              error match {
                case ArrivalError.UnexpectedTrain(trainId) =>
                  BadRequest(s"Unexpected train ${trainId.toString}")
              },
            arrivalEvent => Created(arrivalEvent.id)
          )
        } yield response
    }
  }
}
