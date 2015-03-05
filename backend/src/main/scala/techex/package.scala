import java.util.concurrent.{ThreadFactory, Executors}

import com.typesafe.config.Config
import org.http4s.{Request, Response}
import org.joda.time.{ReadablePeriod, Interval, ReadableInstant}
import techex.web.WebSocket

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalaz.{-\/, \/-, Validation}
import scalaz.concurrent.Task

package object techex {

  type WebHandler = PartialFunction[Request, Task[Response]]

  type WSHandler = PartialFunction[Request,Task[WebSocket]]
  type Val[A] = Validation[String, A]

  implicit def toDuration(period:ReadablePeriod) = period.toPeriod.toStandardDuration

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

  def durationBetween(from:ReadableInstant,to:ReadableInstant) =
    new Interval(from,to).toDuration

  def prln(txt:String) = Task{
    println(txt)
  }

  def namedSingleThreadExecutor(name:String) = Executors.newSingleThreadExecutor(new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      new Thread(r,name)
    }
  })

  def namedSingleThreadScheduler(name:String) = Executors.newSingleThreadScheduledExecutor(new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      new Thread(r,name)
    }
  })

  def getStringOr(cfg:Config,key:String,defValue:String)=
  if(cfg.hasPath(key)) cfg.getString(key) else defValue
}
