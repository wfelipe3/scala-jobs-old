package com.rockthejvm.jobsboard.config

import cats.implicits.*
import cats.MonadThrow
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import scala.reflect.ClassTag
import pureconfig.ConfigReader

object syntax {
  extension (source: ConfigSource)
    def loadF[F[_], A](using reader: ConfigReader[A], F: MonadThrow[F], tag: ClassTag[A]): F[A] =
      F.pure(source.load[A]).flatMap {
        case Left(error)  => F.raiseError[A](ConfigReaderException(error))
        case Right(value) => F.pure(value)
      }
}
