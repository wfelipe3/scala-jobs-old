package com.rockthejvm.jobsboard.http.routes

import cats.implicits.*
import cats.Monad
import cats.effect.kernel.Async
import org.http4s.*
import cats.effect.*
import org.http4s.dsl.*
import org.http4s.server.*
import sttp.tapir.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import org.http4s.ember.server.EmberServerBuilder
import java.util.UUID

class JobRoutes[F[_]: Monad: Async] private:

  // POST /jobs?offset=10&limit=20 {filter}
  private val allJobsEndpoint =
    endpoint.post
      .in(query[Int]("offset"))
      .in(query[Int]("limit"))
      // .in(jsonBody[JobFilter])
      // .out(jsonBody[List[Job]])
      .out(stringBody)
      .serverLogic(_ => "search for jobs".asRight.pure[F])

  // GET /jobs/uuid
  private val getJobsEndpoint =
    endpoint.get
      .in(path[UUID]("id"))
      // .out(jsonBody[Job])
      .out(stringBody)
      .serverLogic(id => s"get a job $id".asRight.pure[F])

  // POST /jobs {payload} -> creates a new job
  private val createJobEndpoint =
    endpoint.post
      .in("create")
      // .in(jsonBody[Job])
      .out(stringBody)
      .serverLogic(_ => s"Create a job".asRight.pure[F])

  // PUT /jobs/uuid {payload} -> updates a job
  private val updateJobEndpoint =
    endpoint.put
      .in(path[UUID]("id"))
      // .in(jsonBody[Job])
      .out(stringBody)
      .serverLogic(id => "update a job $id".asRight.pure[F])

  // DELETE /jobs/uuid
  private val deleteJobEndpoint =
    endpoint.delete
      .in(path[UUID]("id"))
      .out(stringBody)
      .serverLogic(id => s"delete a job $id".asRight.pure[F])

  val routes = Router(
    "/jobs" -> Http4sServerInterpreter[F]()
      .toRoutes(
        List(
          allJobsEndpoint,
          getJobsEndpoint,
          createJobEndpoint,
          updateJobEndpoint,
          deleteJobEndpoint
        )
      )
  )

object JobRoutes:
  def apply[F[_]: Monad: Async]: JobRoutes[F] =
    new JobRoutes[F]
