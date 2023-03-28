package com.rockthejvm.jobsboard.config

import cats.implicits.*
import pureconfig.generic.derivation.default.*
import pureconfig.ConfigReader
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import pureconfig.error.CannotConvert
import pureconfig.error.FailureReason

final case class EmberConfig(host: Host, port: Port) derives ConfigReader

object EmberConfig:

  given ConfigReader[Host] = ConfigReader[String]
    .emap { hostString =>
      Host
        .fromString(hostString)
        .toRight(
          CannotConvert(hostString, Host.getClass.toString, s"Invalid host string: $hostString")
        )
    }

  given ConfigReader[Port] = ConfigReader[Int]
    .emap { portInt =>
      Port
        .fromInt(portInt)
        .toRight(
          CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port string: $portInt")
        )
    }
