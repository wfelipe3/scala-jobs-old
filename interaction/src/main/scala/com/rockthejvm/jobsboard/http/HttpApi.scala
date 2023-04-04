package com.rockthejvm.jobsboard.http

import cats.Monad
import cats.effect.kernel.Async
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.implicits.*
import cats.*
import com.rockthejvm.jobsboard.http.routes.HealthRoutes
import com.rockthejvm.jobsboard.http.routes.JobRoutes

class HttpApi[F[_]: Monad: Async] private:

  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F].routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )

end HttpApi

object HttpApi:
  def apply[F[_]: Monad: Async] = new HttpApi[F]
