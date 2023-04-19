package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*

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
import sttp.model.StatusCode

import com.rockthejvm.jobsboard.domain.job.*
import scala.collection.mutable

class JobRoutes[F[_]: Monad: Async] private:

  private val database = mutable.Map[UUID, Job]()

  sealed trait Errors
  case class JobNotFound(id: UUID) extends Errors

  // POST /jobs?offset=10&limit=20 {filter}
  private val allJobsEndpoint =
    endpoint.post
      .in(query[Int]("offset"))
      .in(query[Int]("limit"))
      // .in(jsonBody[JobFilter])
      .out(jsonBody[List[Job]])
      .serverLogic(_ => database.values.toList.asRight.pure[F])

  // GET /jobs/uuid
  private val getJobsEndpoint =
    endpoint.get
      .errorOut(oneOf[Errors](
        oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[JobNotFound].description("not found")))
      ))
      .in(path[UUID]("id"))
      .out(jsonBody[Job])
      .serverLogic(id => database.get(id).fold(JobNotFound(id).asLeft[Job])(_.asRight[Errors]).pure[F])

  // POST /jobs {payload} -> creates a new job
  private val createJobEndpoint =
    endpoint.post
      .in("create")
      .in(jsonBody[Job])
      .out(jsonBody[Job])
      .serverLogic(job => {
        database.put(job.id, job)
        job.asRight.pure[F]
      })

  // PUT /jobs/uuid {payload} -> updates a job
  private val updateJobEndpoint =
    endpoint.put
      .in(path[UUID]("id"))
      .in(jsonBody[Job])
      .out(jsonBody[Job])
      .errorOut(oneOf[Errors](
        oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[JobNotFound].description("not found")))
      ))
      .serverLogic{(id, job) =>
        database.get(job.id)
          .fold(JobNotFound(job.id).asLeft[Job]){ _ =>
            database.put(job.id, job)
            job.asRight[Errors]
          }.pure[F]
      }

  // DELETE /jobs/uuid
  private val deleteJobEndpoint =
    endpoint.delete
      .in(path[UUID]("id"))
      .out(stringBody)
      .errorOut(oneOf[Errors](
        oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[JobNotFound].description("not found")))
      ))
      .serverLogic{id =>
        database.get(id)
          .fold(JobNotFound(id).asLeft[String]){ _ =>
            database.remove(id)
            "Job deleted".asRight[Errors]
          }.pure[F]
      }

  val routes = Router(
    "/jobs" -> Http4sServerInterpreter[F]()
      .toRoutes(
        List(
          createJobEndpoint,
          allJobsEndpoint,
          getJobsEndpoint,
          updateJobEndpoint,
          deleteJobEndpoint
        )
      )
  )

object JobRoutes:
  def apply[F[_]: Monad: Async]: JobRoutes[F] =
    new JobRoutes[F]
