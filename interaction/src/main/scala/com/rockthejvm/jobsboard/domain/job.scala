package com.rockthejvm.jobsboard.domain

import java.util.UUID

object job:

  case class Job(
      id: UUID,
      date: Long,
      ownerInfo: String,
      jobInfo: JobInfo,
      active: Boolean = false
  )

  case class JobInfo(
      company: String,
      title: String,
      description: String,
      externalUrl: String,
      salaryLo: Option[Int],
      salaryHi: Option[Int],
      currency: Option[String],
      remote: Boolean,
      location: String,
      country: Option[String],
      tags: Option[List[String]],
      image: Option[String],
      seniority: Option[String],
      //there is a compilation error that should increase Xmax-inlines to allow this value
      // other: Option[String]
  )

  object JobInfo:
    def empty = JobInfo(
      company = "",
      title = "",
      description = "",
      externalUrl = "",
      salaryLo = None,
      salaryHi = None,
      currency = None,
      remote = false,
      location = "",
      country = None,
      tags = None,
      image = None,
      seniority = None,
      // other = None
    )

end job
