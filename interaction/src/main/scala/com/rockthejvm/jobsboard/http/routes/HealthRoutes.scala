package com.rockthejvm.jobsboard.http.routes

import cats.implicits.*
import cats.Monad
import cats.effect.kernel.Async
import org.http4s.*
import cats.effect.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.http4s.server.*
import sttp.tapir.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import org.http4s.ember.server.EmberServerBuilder

class HealthRoutes[F[_]: Monad: Async] private:

  private val healthCheckEndpoint =
    endpoint.get
      .out(stringBody)
      .serverLogic(_ => "OK".asRight.pure[F])

  val routes = Router(
    "/health" -> Http4sServerInterpreter[F]()
      .toRoutes(healthCheckEndpoint)
  )

object HealthRoutes:
  def apply[F[_]: Monad: Async]: HealthRoutes[F] =
    new HealthRoutes[F]
