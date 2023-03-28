package com.rockthejvm.jobsboard

import cats.implicits.*
import cats.Monad
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
import com.rockthejvm.jobsboard.http.routes.HealthRoutes
import com.rockthejvm.jobsboard.config.EmberConfig
import com.rockthejvm.jobsboard.config.syntax.* 
import pureconfig.ConfigSource
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import pureconfig.error.ConfigReaderException

object Application extends IOApp.Simple:

  def jobsServer(host: Host, port: Port) =
    EmberServerBuilder
      .default[IO]
      .withHost(host)
      .withPort(port)
      .withHttpApp(HealthRoutes[IO].routes.orNotFound)
      .build
      .use(_ => IO.println("Server ready") *> IO.never)

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
      jobsServer(config.host, config.port)
    }

end Application
