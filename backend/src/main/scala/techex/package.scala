import org.http4s.{Request, Response}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalaz.{-\/, \/-, Validation}
import scalaz.concurrent.Task

package object techex {

  type WebHandler = PartialFunction[Request, Task[Response]]

  type Val[A] = Validation[String, A]

  def succ[A](a: A): Task[A] =
    Task.now(a)

  def fail[A](failMsg: String): Task[A] =
    Task.fail(new Exception(failMsg))

  def asTask[T](fut: Future[T])(implicit ec: ExecutionContext): Task[T] = {
    Task.async {
      register =>
        fut.onComplete {
          case Success(v) => register(\/-(v))
          case Failure(ex) => register(-\/(ex))
        }
    }
  }

  def toJsonQuotes(str: String) =
    str.replace("'", "\"")

  def nonEmpty(str: String): Option[String] =
    if (str == null || str.length == 0)
      None
    else
      Some(str)

}
