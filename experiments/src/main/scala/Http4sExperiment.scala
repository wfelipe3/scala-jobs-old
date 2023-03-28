import cats.effect.*
import cats.*
import cats.implicits.*
import org.http4s.*
import org.http4s.HttpRoutes
import java.util.UUID
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.http4s.dsl.impl.OptionalValidatingQueryParamDecoderMatcher
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.*
import cats.Monad
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.Status.ClientError
import sttp.tapir.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import org.typelevel.ci.CIString
import org.http4s.server.Router

object Http4sExperiment extends IOApp.Simple:

  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(
      id: String,
      title: String,
      year: Int,
      instructorName: String,
      students: List[Student]
  )

  object CourseRepository:
    private val courses: Map[String, Course] = Map(
      "48390679-0E24-4CF4-869B-57EB98C8AC15" -> Course(
        "48390679-0E24-4CF4-869B-57EB98C8AC15",
        "Scala for Beginners",
        2021,
        "Martin Odersky",
        List("Felipe Rojas", "Johanna Buitrago")
      ),
      "1705EDB6-A1E1-4259-B412-D122B7E2F642" -> Course(
        "1705EDB6-A1E1-4259-B412-D122B7E2F642",
        "Java for Beginners",
        2021,
        "John Doe",
        List("John Doe", "Jane Doe")
      )
    )

    def findCoursesById(id: UUID): Option[Course] =
      println(id.toString())
      courses.get(id.toString().toUpperCase())

    def findCoursesByInstructor(instructorName: String): List[Course] =
      courses.values.filter(_.instructorName == instructorName).toList

    def findCoursesByInstructorAndYear(instructorName: String, year: Int) =
      findCoursesByInstructor(instructorName).filter(_.year === year)

  end CourseRepository

  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")
  object YearQueryParamMatcher       extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")
  object IdQueryParamMatcher         extends QueryParamDecoderMatcher[String]("id")

  def courseRouterV2[F[_]: Monad] =
    endpoint.get
      .in("v2" / "courses")
      .in(query[String]("instructor"))
      .in(query[Option[Int]]("year"))
      .out(jsonBody[List[Course]])
      .out(header("My-custom-header", "Number of courses"))
      .serverLogic { (instructor, maybeYear) =>
        maybeYear match
          case None =>
            (CourseRepository
              .findCoursesByInstructor(instructor)
              .asRight[Unit])
              .pure[F]
          case Some(year) =>
            (CourseRepository
              .findCoursesByInstructorAndYear(instructor, year)
              .asRight[Unit])
              .pure[F]

      }

  def healthCheckRoutes[F[_]: Monad] =
    endpoint.get
      .in("health")
      .out(stringBody)
      .serverLogic(_ => "OK".asRight.pure[F])

  def routerWithPrefixes = Router(
    "/api" -> Http4sServerInterpreter[IO]()
      .toRoutes(healthCheckRoutes[IO]),
    "/api" -> Http4sServerInterpreter[IO]()
      .toRoutes(courseRouterV2[IO])
  )

  def courseRoutes[F[_]: Monad]: HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(
            instructorName
          ) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructorName)
        maybeYear match
          case None => Ok(courses.asJson)
          case Some(year) =>
            year.fold(
              _ => BadRequest("Invalid year"),
              y => Ok(courses.filter(_.year == y).asJson)
            )

      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCoursesById(courseId).map(_.students) match
          case None => NotFound(s"No course with $courseId found")
          case Some(students) =>
            Ok(students.asJson, Header.Raw(CIString("My-custom-header"), students.size.toString))
    }

  val routes = Http4sServerInterpreter[IO]()
    .toRoutes(courseRouterV2[IO])

  val server = EmberServerBuilder
    .default[IO]
    .withHttpApp((routerWithPrefixes <+> courseRoutes).orNotFound)
    .build
    .use(_ => IO.println("Server ready") *> IO.never)

  override def run: IO[Unit] = server

end Http4sExperiment
